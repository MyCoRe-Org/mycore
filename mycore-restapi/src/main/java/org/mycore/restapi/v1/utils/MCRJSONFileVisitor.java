/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.restapi.v1.utils;

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR;
import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig;
import org.mycore.frontend.jersey.MCRJerseyUtil;

import com.google.gson.stream.JsonWriter;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRJSONFileVisitor extends SimpleFileVisitor<Path> {
    private JsonWriter jw;

    private String baseURL;

    private String objId;

    private String derId;

    public MCRJSONFileVisitor(JsonWriter jw, MCRObjectID objId, MCRObjectID derId, UriInfo info, Application app) {
        super();
        this.jw = jw;
        this.baseURL = MCRJerseyUtil.getBaseURL(info, app);
        this.objId = objId.toString();
        this.derId = derId.toString();
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        jw.beginObject();
        writePathInfo(dir, attrs);
        jw.name("children").beginArray();
        return super.preVisitDirectory(dir, attrs);
    }

    private String writePathInfo(Path path, BasicFileAttributes attrs) throws IOException {
        MCRPath mcrPath = MCRPath.ofPath(path);
        MCRPath relativePath = mcrPath.getRoot().relativize(mcrPath);
        boolean isRoot = mcrPath.getNameCount() == 0;
        jw.name("type").value(attrs.isDirectory() ? "directory" : "file");
        if (isRoot) {
            jw.name("mycoreobject").value(objId);
            jw.name("mycorederivate").value(mcrPath.getOwner());
        }
        jw.name("name").value(isRoot ? "" : mcrPath.getFileName().toString());
        String pathString;
        String urlPathString;
        try {
            pathString = toStringValue(relativePath, attrs.isDirectory());
            urlPathString = MCRXMLFunctions.encodeURIPath(pathString);
            jw.name("path").value(pathString);
            jw.name("urlPath").value(urlPathString);
        } catch (URISyntaxException e) {
            throw new IOException("Can't encode file path to URI " + relativePath, e);
        }
        addBasicAttributes(path, attrs);
        return urlPathString;
    }

    private void addBasicAttributes(Path path, BasicFileAttributes attrs) throws IOException {
        jw.name("size").value(attrs.size());
        jw.name("time").beginObject();
        jw.name("created").value(attrs.creationTime().toString());
        jw.name("modified").value(attrs.lastModifiedTime().toString());
        jw.name("accessed").value(attrs.lastAccessTime().toString());
        jw.endObject();
        if (attrs.isRegularFile()) {
            jw.name("contentType").value(MCRContentTypes.probeContentType(path));
            if (attrs instanceof MCRFileAttributes<?> fileAttrs) {
                jw.name("md5").value(fileAttrs.digest().toHexString());
            }
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        jw.beginObject();
        String urlPathString = writePathInfo(file, attrs);
        jw.name("extension").value(getFileExtension(file.getFileName().toString()));
        jw.name("href").value(getHref(urlPathString));
        jw.endObject();
        return super.visitFile(file, attrs);
    }

    private String getHref(String urlPathString) {
        return MCRSecureTokenV2FilterConfig.getFileNodeServletSecured(
            MCRObjectID.getInstance(derId), urlPathString.substring(1), this.baseURL);
    }

    private static String getFileExtension(String fileName) {
        if (fileName.endsWith(".")) {
            return "";
        }
        int pos = fileName.lastIndexOf('.');
        return pos == -1 ? "" : fileName.substring(pos + 1);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        jw.endArray();
        jw.endObject();
        return super.postVisitDirectory(dir, exc);
    }

    private static String toStringValue(MCRPath relativePath, boolean isDirectory) {
        if (relativePath == null) {
            return SEPARATOR_STRING;
        }
        String pathString = relativePath.toString();
        if (pathString.isEmpty()) {
            return SEPARATOR_STRING;
        }
        if (pathString.equals(SEPARATOR_STRING)) {
            return SEPARATOR_STRING;
        }
        if (isDirectory) {
            return SEPARATOR + pathString + SEPARATOR;
        } else {
            return SEPARATOR + pathString;
        }
    }

}
