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

package org.mycore.restapi.v2;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_DER_PATH;
import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.util.MCRRestContentHelper;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.restapi.annotations.MCRRequireTransaction;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Ordering;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@Path("/objects/{" + PARAM_MCRID + "}/derivates/{" + PARAM_DERID + "}/contents{" + PARAM_DER_PATH + ":(/[^/]+)*}")
public class MCRRestDerivateContents {

    private static final int BUFFER_SIZE = 8192;

    @Context
    ContainerRequestContext request;

    @Context
    ServletContext context;

    @Parameter(example = "mir_mods_00004711")
    @PathParam(PARAM_MCRID)
    MCRObjectID mcrId;

    @Parameter(example = "mir_derivate_00004711")
    @PathParam(PARAM_DERID)
    MCRObjectID derid;

    @PathParam(PARAM_DER_PATH)
    @DefaultValue("")
    String path;

    private static Response createDirectory(MCRPath mcrPath) {
        try {
            BasicFileAttributes directoryAttrs = Files.readAttributes(mcrPath, BasicFileAttributes.class);
            if (!directoryAttrs.isDirectory()) {
                throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_CREATE_DIRECTORY_ON_FILE)
                    .withMessage("Could not create directory " + mcrPath + ". A file allready exist!")
                    .toException();
            }
            return Response.noContent().build();
        } catch (IOException e) {
            //does not exist
            LogManager.getLogger().info("Creating directory: {}", mcrPath);
            try {
                doWithinTransaction(() -> Files.createDirectory(mcrPath));
            } catch (IOException e2) {
                throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_CREATE_DIRECTORY)
                    .withMessage("Could not create directory " + mcrPath + ".")
                    .withDetail(e.getMessage())
                    .withCause(e)
                    .toException();
            }
            return Response.status(Response.Status.CREATED).build();
        }
    }

    private static void doWithinTransaction(IOOperation op) throws IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        try {
            MCRTransactionHelper.beginTransaction();
            op.run();
        } finally {
            if (MCRTransactionHelper.transactionRequiresRollback()) {
                MCRTransactionHelper.rollbackTransaction();
            } else {
                MCRTransactionHelper.commitTransaction();
            }
        }
    }

    private static Response updateFile(InputStream contents, MCRPath mcrPath) {
        LogManager.getLogger().info("Updating file: {}", mcrPath);
        int memBuf = getUploadMemThreshold();
        java.io.File uploadDirectory = getUploadTempStorage();
        try (DeferredFileOutputStream dfos = new DeferredFileOutputStream(memBuf, mcrPath.getOwner(),
            mcrPath.getFileName().toString(), uploadDirectory);
            MaxBytesOutputStream mbos = new MaxBytesOutputStream(dfos)) {
            IOUtils.copy(contents, mbos);
            mbos.close(); //required if temporary file was used
            OutputStream out = Files.newOutputStream(mcrPath);
            try {
                if (dfos.isInMemory()) {
                    out.write(dfos.getData());
                } else {
                    java.io.File tempFile = dfos.getFile();
                    if (tempFile != null) {
                        try {
                            Files.copy(tempFile.toPath(), out);
                        } finally {
                            LogManager.getLogger().debug("Deleting file {} of size {}.", tempFile.getAbsolutePath(),
                                tempFile.length());
                            tempFile.delete();
                        }
                    }
                }
            } finally {
                //close writes data to database
                doWithinTransaction(out::close);
            }
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_UPDATE_FILE)
                .withMessage("Could not update file " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        return Response.noContent().build();
    }

    private static Response createFile(InputStream contents, MCRPath mcrPath) {
        LogManager.getLogger().info("Creating file: {}", mcrPath);
        try {
            OutputStream out = Files.newOutputStream(mcrPath, StandardOpenOption.CREATE_NEW);
            try {
                IOUtils.copy(contents, out, BUFFER_SIZE);
            } finally {
                //close writes data to database
                doWithinTransaction(out::close);
            }
        } catch (IOException e) {
            try {
                doWithinTransaction(() -> Files.deleteIfExists(mcrPath));
            } catch (IOException e2) {
                LogManager.getLogger().warn("Error while deleting incomplete file.", e2);
            }
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_CREATE_DIRECTORY)
                .withMessage("Could not create file " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    private static EntityTag getETag(MCRFileAttributes attrs) {
        return new EntityTag(attrs.md5sum());
    }

    private static long getUploadMaxSize() {
        return MCRConfiguration2.getOrThrow("MCR.FileUpload.MaxSize", Long::parseLong);
    }

    private static java.io.File getUploadTempStorage() {
        return MCRConfiguration2.getOrThrow("MCR.FileUpload.TempStoragePath", java.io.File::new);
    }

    private static int getUploadMemThreshold() {
        return MCRConfiguration2.getOrThrow("MCR.FileUpload.MemoryThreshold", Integer::parseInt);
    }

    /**
     * Generate Digest header value.
     * @see <a href="https://tools.ietf.org/html/rfc3230">RFC 3230</a>
     * @see <a href="https://tools.ietf.org/html/rfc5843">RFC 45843</a>
     */
    private static String getDigestHeader(String md5sum) {
        final String md5Base64 = Base64.getEncoder().encodeToString(getMD5Digest(md5sum));
        return "MD5=" + md5Base64;
    }

    private static byte[] getMD5Digest(String md5sum) {
        final char[] data = md5sum.toCharArray();
        final int len = data.length;

        // two characters form the hex value.
        final byte[] md5Bytes = new byte[len >> 1];
        for (int i = 0, j = 0; j < len; i++) {
            int f = Character.digit(data[j], 16) << 4;
            j++;
            f = f | Character.digit(data[j], 16);
            j++;
            md5Bytes[i] = (byte) (f & 0xFF);
        }
        return md5Bytes;
    }

    @HEAD
    @MCRCacheControl(sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(description = "get information about mime-type(s), last modified, ETag (md5sum) and ranges support",
        tags = MCRRestUtils.TAG_MYCORE_FILE,
        responses = @ApiResponse(
            description = "Use this for single file metadata queries only. Support is implemented for user agents.",
            headers = {
                @Header(name = "Content-Type", description = "mime type of file"),
                @Header(name = "Content-Length", description = "size of file"),
                @Header(name = "ETag", description = "MD5 sum of file"),
                @Header(name = "Last-Modified", description = "last modified date of file"),
            }))
    public Response getFileOrDirectoryMetadata() {
        MCRPath mcrPath = getPath();
        MCRFileAttributes fileAttributes;
        try {
            fileAttributes = Files.readAttributes(mcrPath, MCRFileAttributes.class);
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_NOT_FOUND)
                .withMessage("Could not find file or directory " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        if (fileAttributes.isDirectory()) {
            return Response.ok()
                .variants(Variant
                    .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE)
                    .build())
                .build();
        }
        String mimeType = context.getMimeType(path);
        return Response
            .status(Response.Status.PARTIAL_CONTENT)
            .header("Accept-Ranges", "bytes")
            .header(HttpHeaders.CONTENT_TYPE, mimeType)
            .lastModified(Date.from(fileAttributes.lastModifiedTime().toInstant()))
            .header(HttpHeaders.CONTENT_LENGTH, fileAttributes.size())
            .tag(getETag(fileAttributes))
            .header("Digest", getDigestHeader(fileAttributes.md5sum()))
            .build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.WILDCARD })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "List directory contents or serves file given by {path} in derivate",
        tags = MCRRestUtils.TAG_MYCORE_FILE)
    public Response getFileOrDirectory(@Context UriInfo uriInfo, @Context HttpHeaders requestHeader) {
        LogManager.getLogger().info("{}:{}", derid, path);
        MCRPath mcrPath = MCRPath.getPath(derid.toString(), path);
        MCRFileAttributes fileAttributes;
        try {
            fileAttributes = Files.readAttributes(mcrPath, MCRFileAttributes.class);
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_NOT_FOUND)
                .withMessage("Could not find file or directory " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        Date lastModified = new Date(fileAttributes.lastModifiedTime().toMillis());
        if (fileAttributes.isDirectory()) {
            return MCRRestUtils
                .getCachedResponse(request.getRequest(), lastModified)
                .orElseGet(() -> serveDirectory(mcrPath, fileAttributes));
        }
        return MCRRestUtils
            .getCachedResponse(request.getRequest(), lastModified, getETag(fileAttributes))
            .orElseGet(() -> {
                MCRPathContent content = new MCRPathContent(mcrPath, fileAttributes);
                content.setMimeType(context.getMimeType(mcrPath.getFileName().toString()));
                try {
                    final List<Map.Entry<String, String>> responseHeader = List
                        .of(Map.entry("Digest", getDigestHeader(fileAttributes.md5sum())));
                    return MCRRestContentHelper.serveContent(content, uriInfo, requestHeader, responseHeader);
                } catch (IOException e) {
                    throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_IO_ERROR)
                        .withMessage("Could not send file " + mcrPath + ".")
                        .withDetail(e.getMessage())
                        .withCause(e)
                        .toException();
                }
            });
    }

    @PUT
    @Consumes(MediaType.WILDCARD)
    @Operation(summary = "Creates directory or file. Parent must exists.",
        responses = {
            @ApiResponse(responseCode = "204", description = "if directory already exists or while was updated"),
            @ApiResponse(responseCode = "201", description = "if directory or file was created"),
            @ApiResponse(responseCode = "400",
                description = "if directory overwrites file or vice versa; content length is too big"),
        },
        tags = MCRRestUtils.TAG_MYCORE_FILE)
    public Response createFileOrDirectory(InputStream contents) {
        MCRPath mcrPath = MCRPath.getPath(derid.toString(), path);
        if (mcrPath.getNameCount() > 1) {
            MCRPath parentDirectory = mcrPath.getParent();
            try {
                BasicFileAttributes parentAttrs = Files.readAttributes(parentDirectory, BasicFileAttributes.class);
                if (!parentAttrs.isDirectory()) {
                    throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_DIRECTORY)
                        .withMessage(parentDirectory + " is not a directory.")
                        .toException();
                }
            } catch (IOException e) {
                throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_NOT_FOUND)
                    .withMessage("Could not find directory " + parentDirectory + ".")
                    .withDetail(e.getMessage())
                    .withCause(e)
                    .toException();
            }
        }
        if (isFile()) {
            long maxSize = getUploadMaxSize();
            String contentLength = request.getHeaderString(HttpHeaders.CONTENT_LENGTH);
            if (contentLength != null && Long.parseLong(contentLength) > maxSize) {
                throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_SIZE)
                    .withMessage("Maximum file size (" + maxSize + " bytes) exceeded.")
                    .withDetail(contentLength)
                    .toException();
            }
            return updateOrCreateFile(contents, mcrPath);
        } else {
            //is directory
            return createDirectory(mcrPath);
        }
    }

    @DELETE
    @Operation(summary = "Deletes file or empty directory.",
        responses = { @ApiResponse(responseCode = "204", description = "if deletion exists"),
            @ApiResponse(responseCode = "400", description = "if directory is not empty"),
        },
        tags = MCRRestUtils.TAG_MYCORE_FILE)
    @MCRRequireTransaction
    public Response deleteFileOrDirectory() {
        MCRPath mcrPath = getPath();
        try {
            if (Files.deleteIfExists(mcrPath)) {
                return Response.noContent().build();
            }
        } catch (DirectoryNotEmptyException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_DIRECTORY_NOT_EMPTY)
                .withMessage("Directory " + mcrPath + " is not empty.")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        } catch (IOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_DELETE)
                .withMessage("Could not delete file or directory " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        throw MCRErrorResponse.fromStatus(Response.Status.NOT_FOUND.getStatusCode())
            .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_NOT_FOUND)
            .withMessage("Could not find file or directory " + mcrPath + ".")
            .toException();
    }

    private Response updateOrCreateFile(InputStream contents, MCRPath mcrPath) {
        MCRFileAttributes fileAttributes;
        try {
            fileAttributes = Files.readAttributes(mcrPath, MCRFileAttributes.class);
            if (!fileAttributes.isRegularFile()) {
                throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_NOT_FILE)
                    .withMessage(mcrPath + " is not a file.")
                    .toException();
            }
        } catch (IOException e) {
            //does not exist
            return createFile(contents, mcrPath);
        }
        //file does already exist
        Date lastModified = new Date(fileAttributes.lastModifiedTime().toMillis());
        EntityTag eTag = getETag(fileAttributes);
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request.getRequest(), lastModified,
            eTag);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }
        return updateFile(contents, mcrPath);
    }

    private boolean isFile() {
        //as per https://tools.ietf.org/html/rfc7230#section-3.3
        MultivaluedMap<String, String> headers = request.getHeaders();
        return headers.containsKey(HttpHeaders.CONTENT_LENGTH) || headers.containsKey("Transfer-Encoding");
    }

    private Response serveDirectory(MCRPath mcrPath, MCRFileAttributes dirAttrs) {
        Directory dir = new Directory(mcrPath, dirAttrs);
        try (DirectoryStream ds = Files.newDirectoryStream(mcrPath)) {
            //A SecureDirectoryStream may get attributes faster than reading attributes for every path instance
            Function<MCRPath, MCRFileAttributes> attrResolver = p -> {
                try {
                    return (ds instanceof SecureDirectoryStream)
                        ? ((SecureDirectoryStream<MCRPath>) ds).getFileAttributeView(MCRPath.toMCRPath(p.getFileName()),
                            MCRMD5AttributeView.class).readAllAttributes() //usually faster
                        : Files.readAttributes(p, MCRFileAttributes.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            List<DirectoryEntry> entries = StreamSupport
                .stream(((DirectoryStream<MCRPath>) ds).spliterator(), false)
                .collect(Collectors.toMap(p -> p, attrResolver))
                .entrySet()
                .stream()
                .map(e -> e.getValue().isDirectory() ? new Directory(e.getKey(), e.getValue())
                    : new File(e.getKey(), e.getValue(), context.getMimeType(e.getKey().getFileName().toString())))
                .sorted() //directories first, than sort for filename
                .collect(Collectors.toList());
            dir.setEntries(entries);
        } catch (IOException | UncheckedIOException e) {
            throw MCRErrorResponse.fromStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_IO_ERROR)
                .withMessage("Could not send directory " + mcrPath + ".")
                .withDetail(e.getMessage())
                .withCause(e)
                .toException();
        }
        return Response.ok(dir).lastModified(new Date(dirAttrs.lastModifiedTime().toMillis())).build();
    }

    private MCRPath getPath() {
        return MCRPath.getPath(derid.toString(), path);
    }

    @FunctionalInterface
    private interface IOOperation {
        void run() throws IOException;
    }

    @XmlRootElement(name = "directory")
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
    private static class Directory extends DirectoryEntry {
        private List<Directory> dirs;

        private List<File> files;

        Directory() {
            super();
        }

        Directory(MCRPath p, MCRFileAttributes attr) {
            super(p, attr);
        }

        void setEntries(List<? extends DirectoryEntry> entries) {
            LogManager.getLogger().info(entries);
            dirs = new ArrayList<>();
            files = new ArrayList<>();
            entries.stream()
                .collect(Collectors.groupingBy(Object::getClass))
                .forEach((c, e) -> {
                    if (File.class.isAssignableFrom(c)) {
                        files.addAll((List<File>) e);
                    } else if (Directory.class.isAssignableFrom(c)) {
                        dirs.addAll((List<Directory>) e);
                    }

                });
        }

        @XmlElement(name = "directory")
        @JsonProperty("directories")
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<Directory> getDirs() {
            return dirs;
        }

        @XmlElement(name = "file")
        @JsonProperty("files")
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<File> getFiles() {
            return files;
        }

    }

    private static class File extends DirectoryEntry {

        private String mimeType;

        private String md5;

        private long size;

        File() {
            super();
        }

        File(MCRPath p, MCRFileAttributes attr, String mimeType) {
            super(p, attr);
            this.md5 = attr.md5sum();
            this.size = attr.size();
            this.mimeType = mimeType;
        }

        @XmlAttribute
        @JsonProperty(index = 3)
        public String getMd5() {
            return md5;
        }

        @XmlAttribute
        @JsonProperty(index = 1)
        public long getSize() {
            return size;
        }

        @XmlAttribute
        @JsonProperty(index = 4)
        public String getMimeType() {
            return mimeType;
        }
    }

    @JsonInclude(content = JsonInclude.Include.NON_NULL)
    private abstract static class DirectoryEntry implements Comparable<DirectoryEntry> {
        private String name;

        private Date modified;

        DirectoryEntry(MCRPath p, MCRFileAttributes attr) {
            this.name = Optional.ofNullable(p.getFileName())
                .map(java.nio.file.Path::toString)
                .orElse(null);
            this.modified = Date.from(attr.lastModifiedTime().toInstant());
        }

        DirectoryEntry() {
        }

        @XmlAttribute
        @JsonProperty(index = 0)
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        String getName() {
            return name;
        }

        @XmlAttribute
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = MCRRestUtils.JSON_DATE_FORMAT)
        @JsonProperty(index = 2)
        public Date getModified() {
            return modified;
        }

        @Override
        public int compareTo(DirectoryEntry o) {
            return Ordering
                .<DirectoryEntry>from((de1, de2) -> {
                    if (de1 instanceof Directory && !(de2 instanceof Directory)) {
                        return -1;
                    }
                    if (de1.getClass().equals(de2.getClass())) {
                        return 0;
                    }
                    return 1;
                })
                .compound((de1, de2) -> de1.getName().compareTo(de2.getName()))
                .compare(this, o);
        }
    }

    private static class MaxBytesOutputStream extends CountingOutputStream {

        private final long maxSize;

        MaxBytesOutputStream(OutputStream out) {
            super(out);
            maxSize = getUploadMaxSize();
        }

        @Override
        protected synchronized void beforeWrite(int n) {
            super.beforeWrite(n);
            if (getByteCount() > maxSize) {
                throw MCRErrorResponse.fromStatus(Response.Status.BAD_REQUEST.getStatusCode())
                    .withErrorCode(MCRErrorCodeConstants.MCRDERIVATE_FILE_SIZE)
                    .withMessage("Maximum file size (" + maxSize + " bytes) exceeded.")
                    .toException();
            }
        }
    }

}
