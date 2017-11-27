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

package org.mycore.services.zipper;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet delivers the contents of MCROjects to the client in
 * container files (see classes extending this servlet). Read permission is required.
 * There are three modes
 * <ol>
 *  <li>if id=mycoreobjectID (delivers the metadata, including all derivates)</li>
 *  <li>if id=derivateID (delivers all files of the derivate)</li>
 *  <li>if id=derivateID/directoryPath (delivers all files in the given directory of the derivate)</li>
 * </ol>
 * 
 * "id" maybe specified as {@link HttpServletRequest#getPathInfo()} or as {@link MCRServlet#getProperty(HttpServletRequest, String)}.
 * @author Thomas Scheffler (yagee)
 *
 */
public abstract class MCRCompressServlet<T extends AutoCloseable> extends MCRServlet {
    private static final long serialVersionUID = 1L;

    protected static String KEY_OBJECT_ID = MCRCompressServlet.class.getCanonicalName() + ".object";

    protected static String KEY_PATH = MCRCompressServlet.class.getCanonicalName() + ".path";

    private static Pattern PATH_INFO_PATTERN = Pattern.compile("\\A([\\w]+)/([\\w/]+)\\z");

    private static Logger LOGGER = LogManager.getLogger(MCRCompressServlet.class);

    @Override
    protected void think(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        //id parameter for backward compatibility
        String paramid = getProperty(req, "id");
        if (paramid == null) {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null) {
                paramid = pathInfo.substring(1);
            }
        }
        if (paramid == null) {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "What should I do?");
            return;
        }
        Matcher ma = PATH_INFO_PATTERN.matcher(paramid);
        //path & directory
        MCRObjectID id;
        String path;
        try {
            if (ma.find()) {
                id = MCRObjectID.getInstance(ma.group(1));
                path = ma.group(2);
            } else {
                id = MCRObjectID.getInstance(paramid);
                path = null;
            }
        } catch (MCRException e) {
            String objId = ma.find() ? ma.group(1) : paramid;
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "ID is not valid: " + objId);
            return;
        }
        boolean readPermission = id.getTypeId().equals("derivate") ? MCRAccessManager
            .checkPermissionForReadingDerivate(id.toString()) : MCRAccessManager.checkPermission(id, PERMISSION_READ);
        if (!readPermission) {
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, "You may not read " + id);
            return;
        }
        req.setAttribute(KEY_OBJECT_ID, id);
        req.setAttribute(KEY_PATH, path);
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (ex != null) {
            //we cannot handle it ourself
            throw ex;
        }
        if (job.getResponse().isCommitted()) {
            return;
        }
        MCRObjectID id = (MCRObjectID) job.getRequest().getAttribute(KEY_OBJECT_ID);
        String path = (String) job.getRequest().getAttribute(KEY_PATH);
        try (ServletOutputStream sout = job.getResponse().getOutputStream()) {
            StringBuffer requestURL = job.getRequest().getRequestURL();
            if (job.getRequest().getQueryString() != null) {
                requestURL.append('?').append(job.getRequest().getQueryString());
            }
            MCRISO8601Date mcriso8601Date = new MCRISO8601Date();
            mcriso8601Date.setDate(new Date());
            String comment = "Created by " + requestURL + " at " + mcriso8601Date.getISOString();
            try (T container = createContainer(sout, comment)) {
                job.getResponse().setContentType(getMimeType());
                String filename = getFileName(id, path);
                job.getResponse().addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                if (id.getTypeId().equals("derivate")) {
                    sendDerivate(id, path, container);
                } else {
                    sendObject(id, job, container);
                }
                disposeContainer(container);
            }
        }
    }

    private void sendObject(MCRObjectID id, MCRServletJob job, T container) throws Exception {
        MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(id);
        if (content == null) {
            throw new FileNotFoundException("Could not find object: " + id);
        }
        long lastModified = MCRXMLMetadataManager.instance().getLastModified(id);
        HttpServletRequest req = job.getRequest();
        byte[] metaDataContent = getMetaDataContent(content, req);
        sendMetadataCompressed("metadata.xml", metaDataContent, lastModified, container);

        // zip all derivates
        List<Element> li = content.asXML().getRootElement().getChild("structure").getChild("derobjects")
            .getChildren("derobject");

        for (Element el : li) {
            if (el.getAttributeValue("inherited").equals("0")) {
                String ownerID = el.getAttributeValue("href", XLINK_NAMESPACE);
                // here the access check is tested only against the derivate
                if (MCRAccessManager.checkPermission(ownerID, PERMISSION_READ)
                    && MCRXMLFunctions.isDisplayedEnabledDerivate(ownerID)) {
                    sendDerivate(MCRObjectID.getInstance(ownerID), null, container);
                }
            }
        }
    }

    private byte[] getMetaDataContent(MCRContent content, HttpServletRequest req) throws Exception {
        // zip the object's Metadata
        MCRParameterCollector parameters = new MCRParameterCollector(req);
        if (parameters.getParameter("Style", null) == null) {
            parameters.setParameter("Style", "compress");
        }
        MCRContentTransformer contentTransformer = MCRLayoutService.getContentTransformer(content.getDocType(),
            parameters);
        ByteArrayOutputStream out = new ByteArrayOutputStream(32 * 1024);
        if (contentTransformer instanceof MCRParameterizedTransformer) {
            ((MCRParameterizedTransformer) contentTransformer).transform(content, out, parameters);
        } else {
            contentTransformer.transform(content, out);
        }
        return out.toByteArray();
    }

    private void sendDerivate(MCRObjectID id, String path, T container) throws IOException {

        MCRPath resolvedPath = MCRPath.getPath(id.toString(), path == null ? "/" : path);

        if (!Files.exists(resolvedPath)) {
            throw new NoSuchFileException(id.toString(), path, "Could not find path " + resolvedPath);
        }

        if (Files.isRegularFile(resolvedPath)) {
            BasicFileAttributes attrs = Files.readAttributes(resolvedPath, BasicFileAttributes.class);
            sendCompressedFile(resolvedPath, attrs, container);
            LOGGER.debug("file {} zipped", resolvedPath);
            return;
        }
        // root is a directory
        Files.walkFileTree(resolvedPath, new CompressVisitor<>(this, container));
    }

    private String getFileName(MCRObjectID id, String path) {
        if (path == null || path.equals("")) {
            return MessageFormat.format("{0}.{1}", id, getFileExtension());
        } else {
            return MessageFormat.format("{0}-{1}.{2}", id, path.replaceAll("/", "-"), getFileExtension());
        }
    }

    /**
     * Constructs a path name in form of {ownerID}+'/'+{path} or {ownerID} if path is root component.
     * @param path absolute path
     */
    protected String getFilename(MCRPath path) {
        return path.getNameCount() == 0 ? path.getOwner()
            : path.getOwner() + '/'
                + path.getRoot().relativize(path);
    }

    protected abstract void sendCompressedDirectory(MCRPath file, BasicFileAttributes attrs, T container)
        throws IOException;

    protected abstract void sendCompressedFile(MCRPath file, BasicFileAttributes attrs, T container) throws IOException;

    protected abstract void sendMetadataCompressed(String fileName, byte[] content, long modified, T container)
        throws IOException;

    protected abstract String getMimeType();

    protected abstract String getFileExtension();

    protected abstract T createContainer(ServletOutputStream sout, String comment);

    protected abstract void disposeContainer(T container) throws IOException;

    private static class CompressVisitor<T extends AutoCloseable> extends SimpleFileVisitor<Path> {

        private MCRCompressServlet<T> impl;

        private T container;

        public CompressVisitor(MCRCompressServlet<T> impl, T container) {
            this.impl = impl;
            this.container = container;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            impl.sendCompressedDirectory(MCRPath.toMCRPath(dir), attrs, container);
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            impl.sendCompressedFile(MCRPath.toMCRPath(file), attrs, container);
            LOGGER.debug("file {} zipped", file);
            return super.visitFile(file, attrs);
        }

    }

}
