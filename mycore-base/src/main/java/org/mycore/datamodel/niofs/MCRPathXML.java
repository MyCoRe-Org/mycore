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

package org.mycore.datamodel.niofs;

import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR;
import static org.mycore.datamodel.niofs.MCRAbstractFileSystem.SEPARATOR_STRING;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Thomas Scheffler (yagee)
 * @version $Revision: 28688 $ $Date: 2013-12-18 15:27:20 +0100 (Wed, 18 Dec 2013) $
 */
public class MCRPathXML {

    static Logger LOGGER = LogManager.getLogger(MCRPathXML.class);

    private MCRPathXML() {
    }

    public static Document getDirectoryXML(MCRPath path) throws IOException {
        BasicFileAttributes attr = path.getFileSystem().provider().readAttributes(path, BasicFileAttributes.class);
        return getDirectoryXML(path, attr);
    }

    /**
     * Sends the contents of an MCRDirectory as XML data to the client
     */
    public static Document getDirectoryXML(MCRPath path, BasicFileAttributes attr) throws IOException {
        LOGGER.debug("MCRDirectoryXML: start listing of directory {}", path);

        Element root = new Element("mcr_directory");
        Document doc = new org.jdom2.Document(root);

        addString(root, "uri", path.toUri().toString(), false);
        addString(root, "ownerID", path.getOwner(), false);
        MCRPath relativePath = path.getRoot().relativize(path);
        boolean isRoot = relativePath.toString().isEmpty();
        addString(root, "name", (isRoot ? "" : relativePath.getFileName().toString()), true);
        addString(root, "path", toStringValue(relativePath), true);
        if (!isRoot) {
            addString(root, "parentPath", toStringValue(relativePath.getParent()), true);
        }
        addBasicAttributes(root, attr, path);
        Element numChildren = new Element("numChildren");
        Element here = new Element("here");
        root.addContent(numChildren);
        numChildren.addContent(here);

        Element nodes = new Element("children");
        root.addContent(nodes);
        SortedMap<MCRPath, MCRFileAttributes<?>> directories = new TreeMap<>();
        SortedMap<MCRPath, MCRFileAttributes<?>> files = new TreeMap<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
            LOGGER.debug(() -> "Opened DirectoryStream for " + path);
            Function<Path, MCRFileAttributes<?>> attrResolver = p -> {
                try {
                    return Files.readAttributes(p, MCRFileAttributes.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            if (dirStream instanceof SecureDirectoryStream) {
                //fast path
                LOGGER.debug(() -> "Using SecureDirectoryStream code path for " + path);
                attrResolver = p -> {
                    try {
                        BasicFileAttributeView attributeView = ((SecureDirectoryStream<Path>) dirStream)
                            .getFileAttributeView(p.getFileName(), BasicFileAttributeView.class);
                        return (MCRFileAttributes<?>) attributeView.readAttributes();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                };

            }
            for (Path child : dirStream) {
                MCRFileAttributes<?> childAttrs;
                try {
                    childAttrs = attrResolver.apply(child);
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
                if (childAttrs.isDirectory()) {
                    directories.put(MCRPath.toMCRPath(child), childAttrs);
                } else {
                    files.put(MCRPath.toMCRPath(child), childAttrs);
                }
            }
        }
        //store current directory statistics
        addString(here, "directories", Integer.toString(directories.size()), false);
        addString(here, "files", Integer.toString(files.size()), false);
        for (Map.Entry<MCRPath, MCRFileAttributes<?>> dirEntry : directories.entrySet()) {
            Element child = new Element("child");
            child.setAttribute("type", "directory");
            addString(child, "name", dirEntry.getKey().getFileName().toString(), true);
            addString(child, "uri", dirEntry.getKey().toUri().toString(), false);
            nodes.addContent(child);
            addBasicAttributes(child, dirEntry.getValue(), dirEntry.getKey());
        }
        for (Map.Entry<MCRPath, MCRFileAttributes<?>> fileEntry : files.entrySet()) {
            Element child = new Element("child");
            child.setAttribute("type", "file");
            addString(child, "name", fileEntry.getKey().getFileName().toString(), true);
            addString(child, "uri", fileEntry.getKey().toUri().toString(), false);
            nodes.addContent(child);
            addAttributes(child, fileEntry.getValue(), fileEntry.getKey());
        }

        LOGGER.debug("MCRDirectoryXML: end listing of directory {}", path);

        return doc;

    }

    /**
     * Returns metadata of the file retrievable by 'path' in XML form. Same as
     * {@link #getFileXML(MCRPath, BasicFileAttributes)}, but attributes are retrieved first.
     */
    public static Document getFileXML(MCRPath path) throws IOException {
        MCRFileAttributes<?> attrs = Files.readAttributes(path, MCRFileAttributes.class);
        return getFileXML(path, attrs);
    }

    /**
     * Returns metadata of the file retrievable by 'path' in XML form.
     *
     * @param path
     *            Path to File
     * @param attrs
     *            file attributes of given file
     */
    public static Document getFileXML(MCRPath path, BasicFileAttributes attrs) throws IOException {
        Element root = new Element("file");
        root.setAttribute("uri", path.toUri().toString());
        root.setAttribute("ownerID", path.getOwner());
        String fileName = path.getFileName().toString();
        root.setAttribute("name", fileName);
        String absolutePath = path.getOwnerRelativePath();
        root.setAttribute("path", absolutePath);
        root.setAttribute("extension", getFileExtension(fileName));
        root.setAttribute("returnId",
            MCRMetadataManager.getObjectId(MCRObjectID.getInstance(path.getOwner()), 10, TimeUnit.SECONDS).toString());
        Collection<MCRCategoryID> linksFromReference = MCRCategLinkServiceFactory.getInstance().getLinksFromReference(
            new MCRCategLinkReference(path));
        for (MCRCategoryID category : linksFromReference) {
            Element catEl = new Element("category");
            catEl.setAttribute("id", category.toString());
            root.addContent(catEl);
        }
        if (!attrs.isDirectory() && attrs instanceof MCRFileAttributes<?>) {
            addAttributes(root, (MCRFileAttributes<?>) attrs, path);
        } else {
            addBasicAttributes(root, attrs, path);
        }
        return new Document(root);
    }

    private static String getFileExtension(String fileName) {
        if (fileName.endsWith(".")) {
            return "";
        }
        int pos = fileName.lastIndexOf(".");
        return pos == -1 ? "" : fileName.substring(pos + 1);
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

    private static void addBasicAttributes(Element root, BasicFileAttributes attr, MCRPath path) throws IOException {
        addString(root, "size", String.valueOf(attr.size()), false);
        addDate(root, "created", attr.creationTime());
        addDate(root, "lastModified", attr.lastModifiedTime());
        addDate(root, "lastAccessed", attr.lastAccessTime());
        if (attr.isRegularFile()) {
            addString(root, "contentType", MCRContentTypes.probeContentType(path), false);
        }
    }

    private static void addAttributes(Element root, MCRFileAttributes<?> attr, MCRPath path) throws IOException {
        addBasicAttributes(root, attr, path);
        addString(root, "md5", attr.md5sum(), false);
    }

    private static void addDate(Element parent, String type, FileTime date) {
        Element xDate = new Element("date");
        parent.addContent(xDate);
        xDate.setAttribute("type", type);
        xDate.addContent(date.toString());
    }

    private static void addString(Element parent, String itemName, String content, boolean preserve) {
        if (content == null) {
            return;
        }
        Element child = new Element(itemName).addContent(content);
        if (preserve) {
            child.setAttribute("space", "preserve", MCRConstants.XML_NAMESPACE);
        }
        parent.addContent(child);
    }
}
