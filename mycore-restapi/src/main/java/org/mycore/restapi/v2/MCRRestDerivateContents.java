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

import static org.mycore.restapi.MCRRestAuthorizationFilter.PARAM_DERID;
import static org.mycore.restapi.MCRRestAuthorizationFilter.PARAM_MCRID;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.SecureDirectoryStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRMD5AttributeView;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.jersey.MCRCacheControl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Ordering;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@Path("/v2/objects/{" + PARAM_MCRID + "}/derivates/{" + PARAM_DERID + "}/contents{path:(/[^/]+)*}")
public class MCRRestDerivateContents {
    @Context
    HttpRequest request;

    @Context
    HttpResponse response;

    @Context
    ServletContext context;

    @HEAD
    @MCRCacheControl(sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getHead(
        @Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid,
        @PathParam("path") @DefaultValue("") String path) {
        MCRPath mcrPath = getPath(derid, path);
        MCRFileAttributes fileAttributes;
        try {
            fileAttributes = Files.readAttributes(mcrPath, MCRFileAttributes.class);
        } catch (IOException e) {
            throw new NotFoundException(e);
        }
        if (fileAttributes.isDirectory()) {
            return Response.ok()
                .variants(Variant
                    .mediaTypes(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_XML_TYPE)
                    .build())
                .build();
        }
        String mimeType = context.getMimeType(path);
        return Response
            .status(Response.Status.PARTIAL_CONTENT)
            .header(HttpHeaders.CONTENT_TYPE, mimeType)
            .header(HttpHeaders.CONTENT_LENGTH, fileAttributes.size())
            .tag(getETag(mcrPath, fileAttributes))
            .build();
    }

    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.WILDCARD })
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    @Operation(
        summary = "List directory contents or serves file given by {path} in derivate")
    public Response getFileOrDirectory(
        @Parameter(example = "mir_mods_00004711") @PathParam(PARAM_MCRID) MCRObjectID mcrId,
        @Parameter(example = "mir_derivate_00004711") @PathParam(PARAM_DERID) MCRObjectID derid,
        @PathParam("path") @DefaultValue("") String path) {
        LogManager.getLogger().info("{}:{}", derid, path);
        MCRPath mcrPath = MCRPath.getPath(derid.toString(), path);
        MCRFileAttributes fileAttributes = null;
        try {
            fileAttributes = Files.readAttributes(mcrPath, MCRFileAttributes.class);
        } catch (IOException e) {
            throw new NotFoundException(e);
        }
        if (fileAttributes.isDirectory()) {
            return serveDirectory(mcrPath, fileAttributes);
        }
        StreamingOutput sout = out -> {

        };
        Response.Status status = Response.Status.OK;
        return Response.status(status)
            .entity(sout)
            .tag(getETag(mcrPath, fileAttributes))
            .lastModified(Date.from(fileAttributes.lastModifiedTime().toInstant()))
            .header(HttpHeaders.CONTENT_TYPE, context.getMimeType(mcrPath.getFileName().toString()))
            .build();
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
                    : new File(e.getKey(), e.getValue()))
                .sorted() //directories first, than sort for filename
                .collect(Collectors.toList());
            dir.setEntries(entries);
        } catch (IOException | UncheckedIOException e) {
            throw new InternalServerErrorException(e);
        }
        return Response.ok(dir).build();
    }

    private static MCRPath getPath(MCRObjectID derId, String path) {
        return MCRPath.getPath(derId.toString(), path);
    }

    private static EntityTag getETag(MCRPath path, MCRFileAttributes attrs) {
        if (attrs.fileKey() instanceof String) {
            return EntityTag.valueOf(attrs.fileKey().toString());
        }
        long lastModified = attrs.lastModifiedTime().toMillis();
        long length = attrs.size();
        if (path == null || length < 0 || lastModified <= 0) {
            return null;
        }
        StringBuilder b = new StringBuilder(32);
        b.append("W/\"");
        long lhash = path.hashCode();
        byte[] unencodedETag = ByteBuffer.allocate(Long.SIZE / 4).putLong(lastModified ^ lhash).putLong(length ^ lhash)
            .array();
        b.append(Base64.getEncoder().encodeToString(unencodedETag));
        b.append('"');
        return EntityTag.valueOf(b.toString());
    }

    @XmlRootElement(name = "directory")
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
    private static class Directory extends DirectoryEntry {
        private List<Directory> dirs;

        private List<File> files;

        protected Directory() {
            super();
        }

        public void setEntries(List<? extends DirectoryEntry> entries) {
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

        protected Directory(MCRPath p, MCRFileAttributes attr) {
            super(p, attr);
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

        private String md5;

        private Date modified;

        private long size;

        protected File() {
            super();
        }

        protected File(MCRPath p, MCRFileAttributes attr) {
            super(p, attr);
            this.md5 = attr.md5sum();
            this.modified = Date.from(attr.lastModifiedTime().toInstant());
            this.size = attr.size();
        }

        @XmlAttribute
        @JsonProperty(index = 3)
        public String getMd5() {
            return md5;
        }

        @XmlAttribute
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = MCRRestUtils.JSON_DATE_FORMAT)
        @JsonProperty(index = 2)
        public Date getModified() {
            return modified;
        }

        @XmlAttribute
        @JsonProperty(index = 1)
        public long getSize() {
            return size;
        }
    }

    @JsonInclude(content = JsonInclude.Include.NON_NULL)
    private static abstract class DirectoryEntry implements Comparable<DirectoryEntry> {
        private String name;

        protected DirectoryEntry(MCRPath p, MCRFileAttributes attr) {
            this.name = Optional.ofNullable(p.getFileName())
                .map(java.nio.file.Path::toString)
                .orElse(null);
        }

        protected DirectoryEntry() {
        }

        @XmlAttribute
        @JsonProperty(index = 0)
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public String getName() {
            return name;
        }

        @Override
        public int compareTo(DirectoryEntry o) {
            return Ordering
                .<DirectoryEntry> from((de1, de2) -> {
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

}
