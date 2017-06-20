/**
 * 
 */
package org.mycore.restapi.v1.utils;

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR;
import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.ws.rs.core.UriInfo;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.filter.MCRSecureTokenV2FilterConfig;
import org.mycore.frontend.jersey.MCRJerseyUtil;

import com.google.gson.stream.JsonWriter;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRJSONFileVisitor extends SimpleFileVisitor<Path> {

    private JsonWriter jw;

    private String baseURL;

    private String objId;

    private String derId;

    public MCRJSONFileVisitor(JsonWriter jw, MCRObjectID objId, MCRObjectID derId, UriInfo info) {
        super();
        this.jw = jw;
        this.baseURL = MCRJerseyUtil.getBaseURL(info);
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

    private void writePathInfo(Path path, BasicFileAttributes attrs) throws IOException {
        MCRPath mcrPath = MCRPath.toMCRPath(path);
        MCRPath relativePath = mcrPath.getRoot().relativize(mcrPath);
        boolean isRoot = mcrPath.getNameCount() == 0;
        jw.name("type").value(attrs.isDirectory() ? "directory" : "file");
        if (isRoot) {
            jw.name("mycoreobject").value(objId);
            jw.name("mycorederivate").value(mcrPath.getOwner());
        }
        jw.name("name").value(isRoot ? "" : mcrPath.getFileName().toString());
        jw.name("path")
            .value(attrs.isDirectory() ? toStringValue(relativePath) : SEPARATOR_STRING + relativePath.toString());
        if (!isRoot) {
            jw.name("parentPath").value(toStringValue(relativePath.getParent()));
        }
        addBasicAttributes(path, attrs);
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
            if (attrs instanceof MCRFileAttributes) {
                jw.name("md5").value(((MCRFileAttributes<?>) attrs).md5sum());
            }
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        MCRPath mcrPath = MCRPath.toMCRPath(file);
        MCRPath relativePath = mcrPath.getRoot().relativize(mcrPath);
        jw.beginObject();
        writePathInfo(file, attrs);
        jw.name("extension").value(getFileExtension(file.getFileName().toString()));
        jw.name("href").value(MCRSecureTokenV2FilterConfig.getFileNodeServletSecured(MCRObjectID.getInstance(derId),
            relativePath.toString(), this.baseURL));
        jw.endObject();
        return super.visitFile(file, attrs);
    }

    private static String getFileExtension(String fileName) {
        if (fileName.endsWith(".")) {
            return "";
        }
        int pos = fileName.lastIndexOf(".");
        return pos == -1 ? "" : fileName.substring(pos + 1);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        jw.endArray();
        jw.endObject();
        return super.postVisitDirectory(dir, exc);
    }

    private static String toStringValue(MCRPath relativePath) {
        if (relativePath == null) {
            return SEPARATOR_STRING;
        }
        String pathString = relativePath.toString();
        if (pathString.isEmpty()) {
            return SEPARATOR_STRING;
        }
        if (pathString.equals(SEPARATOR_STRING)) {
            return pathString;
        }
        return SEPARATOR + pathString + SEPARATOR;
    }

}
