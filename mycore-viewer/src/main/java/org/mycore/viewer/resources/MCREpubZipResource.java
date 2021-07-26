/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.viewer.resources;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Path("/epub")
@MCRStaticContent // prevents session creation
public class MCREpubZipResource {

    public static final String EPUB_SPLIT = ".epub/";

    @Context
    HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    private static void suppressedClose(Closeable... closeus) {
        for (Closeable closeme : closeus) {
            if (closeme != null) {
                try {
                    closeme.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @GET
    @Path("/{derivateID}/{epubFilePathAndPathInEpub:.+}")
    public Response extract(
        @PathParam("derivateID") String derivateID,
        @PathParam("epubFilePathAndPathInEpub") String epubFileAndSubpath) {
        java.nio.file.Path epubPath = null;

        final String[] split = epubFileAndSubpath.split(EPUB_SPLIT, 2);

        if (split.length != 2) {
            throw new WebApplicationException("The path seems to be wrong: " + epubFileAndSubpath,
                Response.Status.BAD_REQUEST);
        }

        final String epubFile = split[0] + EPUB_SPLIT.substring(0, EPUB_SPLIT.length() - 1);
        final String pathInEpub = split[1];

        MCRSession session = null;
        try { // creates a quick session to get the physical path to the epub from db and check rights
            MCRSessionMgr.unlock();
            session = MCRServlet.getSession(request);
            MCRSessionMgr.setCurrentSession(session);
            MCRFrontendUtil.configureSession(session, request, response);
            if (!MCRAccessManager.checkPermission(derivateID, MCRAccessManager.PERMISSION_READ)) {
                throw new WebApplicationException("No rights to read " + derivateID, Response.Status.FORBIDDEN);
            }
            epubPath = MCRPath.getPath(derivateID, epubFile).toPhysicalPath();
        } catch (IOException e) {
            throw new WebApplicationException("Error while resolving physical path of " + derivateID + ":" + epubFile,
                e);
        } finally { // releases the session reliable
            try {
                if (session != null && MCRTransactionHelper.isTransactionActive()) {
                    if (MCRTransactionHelper.transactionRequiresRollback()) {
                        MCRTransactionHelper.rollbackTransaction();
                    } else {
                        MCRTransactionHelper.commitTransaction();
                    }
                }
            } finally {
                MCRSessionMgr.releaseCurrentSession();
                MCRSessionMgr.lock();
            }
        }

        if (!Files.exists(epubPath)) {
            throw new WebApplicationException("The file " + derivateID + ":" + epubFile + " is not present!",
                Response.Status.NOT_FOUND);
        }

        // try with closeable can not be used in this case.
        // The streams/files would be closed after we leave this method
        // StreamingOutput is called after this method is left
        // StreamingOutput would try to call write/read on the closed stream
        // this method is responsible for the streams and the zip file until the point responsible is set to false
        // after that the StreamingOutput is responsible
        SeekableByteChannel epubStream = null;
        InputStream zipFileStream = null;
        ZipFile zipFile = null;
        boolean responsible = true;

        try {
            epubStream = Files.newByteChannel(epubPath, StandardOpenOption.READ);
            zipFile = new ZipFile(epubStream);

            final Optional<ZipArchiveEntry> entryOfFileInEpub = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(zipFile.getEntries().asIterator(), Spliterator.ORDERED),
                    false)
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> Optional.ofNullable(entry.getName()).filter(pathInEpub::equals).isPresent())
                .findFirst();

            final ZipArchiveEntry zipArchiveEntry = entryOfFileInEpub
                .orElseThrow(() -> new WebApplicationException("EPUB does not contain: " + pathInEpub,
                    Response.Status.NOT_FOUND));

            zipFileStream = zipFile.getInputStream(zipArchiveEntry);

            final InputStream finalZipFileStream = zipFileStream;
            final Closeable finalZipFile = zipFile;
            final Closeable finalEpubStream = epubStream;

            StreamingOutput out = output -> {
                try {
                    IOUtils.copy(finalZipFileStream, output);
                } catch (IOException e) {
                    // suppress spamming the console with broken pipe on request abort
                } finally {
                    suppressedClose(finalZipFileStream, finalZipFile, finalEpubStream);
                }
            };
            responsible = false;
            return Response.ok(out).build();
        } catch (IOException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            if (responsible) {
                // if responsible is true then this method is responsible for closing
                suppressedClose(zipFileStream, zipFile, epubStream);
            }
        }
    }
}
