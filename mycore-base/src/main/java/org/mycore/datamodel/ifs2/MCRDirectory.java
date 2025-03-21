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

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import org.jdom2.Element;

/**
 * Represents a directory stored in a file collection, which may contain other
 * files and directories.
 *
 * @author Frank Lützenkirchen
 */
public class MCRDirectory extends MCRStoredNode {

    /**
     * Create MCRDirectory representing an existing, already stored directory.
     *
     * @param parent
     *            the parent directory of this directory
     * @param fo
     *            the local directory in the store storing this directory
     */
    protected MCRDirectory(MCRDirectory parent, Path fo, Element data) {
        super(parent, fo, data);
    }

    /**
     * Create a new MCRDirectory that does not exist yet
     *
     * @param parent
     *            the parent directory of this directory
     * @param name
     *            the name of the new subdirectory to create
     */
    protected MCRDirectory(MCRDirectory parent, String name) throws IOException {
        super(parent, name, "dir");
        Files.createDirectory(path);
        getRoot().saveAdditionalData();
    }

    /**
     * Creates a new subdirectory within this directory
     *
     * @param name
     *            the name of the new directory
     */
    public MCRDirectory createDir(String name) throws IOException {
        return new MCRDirectory(this, name);
    }

    /**
     * Creates a new file within this directory
     *
     * @param name
     *            the name of the new file
     */
    public MCRFile createFile(String name) throws IOException {
        return new MCRFile(this, name);
    }

    @SuppressWarnings("unchecked")
    private Element getChildData(String name) {
        for (Element child : readData(Element::getChildren)) {
            if (name.equals(child.getAttributeValue("name"))) {
                return child;
            }
        }

        Element childData = new Element("node");
        childData.setAttribute("name", name);
        writeData(e -> e.addContent(childData));
        return childData;
    }

    /**
     * Returns the MCRFile or MCRDirectory that is represented by the given
     * FileObject, which is a direct child of the directory FileObject this
     * MCRDirectory is stored in.
     *
     * @return an MCRFile or MCRDirectory child
     */
    @Override
    protected MCRStoredNode buildChildNode(Path fo) {
        if (fo == null) {
            return null;
        }
        if (!this.path.equals(fo.getParent())) {
            throw new IllegalArgumentException(fo + " is not a direct child of " + this.path + ".");
        }
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(fo, BasicFileAttributes.class);
        } catch (IOException e) {
            //does not exist
            return null;
        }

        Element childData = getChildData(fo.getFileName().toString());
        if (attrs.isRegularFile()) {
            return new MCRFile(this, fo, childData);
        } else {
            return new MCRDirectory(this, fo, childData);
        }
    }

    /**
     * Repairs additional metadata of this directory and all its children
     */
    @Override
    @SuppressWarnings("unchecked")
    void repairMetadata() throws IOException {
        writeData(e -> {
            e.setName("dir");
            e.setAttribute("name", getName());

            for (Element childEntry : e.getChildren()) {
                childEntry.setName("node");
            }
        });

        try (Stream<MCRNode> streamMCRNode = getChildren()) {
            streamMCRNode.filter(MCRStoredNode.class::isInstance)
                .map(MCRStoredNode.class::cast)
                .sequential()
                .forEach(mcrStoredNode -> {
                    try {
                        mcrStoredNode.repairMetadata();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException ignoredUnchecked) {
            throw ignoredUnchecked.getCause();
        }

        writeData(e -> e.removeChildren("node"));
    }
}
