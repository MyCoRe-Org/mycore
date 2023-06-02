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

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.stream.Stream;

import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRPathContent;

/**
 * Represents a file, directory or file collection within a file store. Files
 * and directories can be either really stored, or virtually existing as a child
 * node contained within a stored container file like zip or tar.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRNode {
    /**
     * The path object representing this node in the underlying filesystem.
     */
    protected Path path;

    /**
     * The parent node owning this file, a directory or container file
     */
    protected MCRNode parent;

    /**
     * Creates a new node representing a child of the given parent
     * 
     * @param parent
     *            the parent node
     * @param path
     *            the file object representing this node in the underlying
     *            filesystem
     */
    protected MCRNode(MCRNode parent, Path path) {
        this.path = path;
        this.parent = parent;
    }

    /**
     * Returns the file or directory name
     * 
     * @return the node's filename
     */
    public String getName() {
        return path.getFileName().toString();
    }

    /**
     * Returns the complete path of this node up to the root file collection.
     * Path always start with a slash, slash is used as directory delimiter.
     * 
     * @return the absolute path of this node
     */
    public String getPath() {
        if (parent != null) {
            if (parent.parent == null) {
                return "/" + getName();
            } else {
                return parent.getPath() + "/" + getName();
            }
        } else {
            return "/";
        }
    }

    /**
     * Returns the parent node containing this node
     * 
     * @return the parent directory or container file
     */
    public MCRNode getParent() {
        return parent;
    }

    /**
     * Returns the root file collection this node belongs to
     * 
     * @return the root file collection
     */
    public MCRFileCollection getRoot() {
        return parent.getRoot();
    }

    /**
     * Returns true if this node is a file
     * 
     * @return true if this node is a file
     */
    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    /**
     * Returns true if this node is a directory
     * 
     * @return true if this node is a directory
     */
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    /**
     * For file nodes, returns the file content size in bytes, otherwise returns
     * 0.
     * 
     * @return the file size in bytes
     */
    public long getSize() throws IOException {
        BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
        return attr.isRegularFile() ? attr.size() : 0;
    }

    /**
     * Returns the time this node was last modified.
     * 
     * @return the time this node was last modified
     */
    public Date getLastModified() throws IOException {
        return Date.from(Files.getLastModifiedTime(path).toInstant());
    }

    /**
     * Returns true if this node has child nodes. Directories and container
     * files like zip or tar may have child nodes.
     * 
     * @return true if children exist
     */
    public boolean hasChildren() throws IOException {
        return !isFile() && Files.list(path).findAny().isPresent();
    }

    /**
     * Returns the number of child nodes of this node.
     * 
     * @return the number of child nodes of this node.
     */
    public int getNumChildren() throws IOException {
        if (isFile()) {
            return 0;
        }

        try (Stream<Path> streamPath = Files.list(path)) {
            return Math.toIntExact(streamPath.count());
        }
    }

    /**
     * Returns the children of this node.
     * 
     * @return a List of child nodes, which may be empty, in undefined order
     */
    public Stream<MCRNode> getChildren() throws IOException {
        if (isFile()) {
            return Stream.empty();
        }
        return Files.list(path)
            .map(this::buildChildNode);
    }

    /**
     * Creates a node instance for the given FileObject, which represents the
     * child
     * 
     * @param fo
     *            the FileObject representing the child in the underlying
     *            filesystem
     * @return the child node or null, if the fo does not exists
     * @throws IllegalArgumentException if fo is not valid path for a child of this
     */
    protected abstract MCRNode buildChildNode(Path fo);

    /**
     * Returns the child node with the given filename, or null
     * 
     * @param name
     *            the name of the child node
     * @return the child node with that name, or null when no such file exists
     */
    public MCRNode getChild(String name) {
        Path child = path.resolve(name);
        return buildChildNode(child);
    }

    /**
     * Returns the node with the given relative or absolute path in the file
     * collection this node belongs to. Slash is used as directory delimiter.
     * When the path starts with a slash, it is an absolute path and resolving
     * is startet at the root file collection. When the path is relative,
     * resolving starts with the current node. One dot represents the current
     * node, Two dots represent the parent node, like in paths used by typical
     * real filesystems.
     * 
     * @param path
     *            the absolute or relative path of the node to find, may contain
     *            . or ..
     * @return the node at the given path, or null
     */
    public MCRNode getNodeByPath(String path) {
        MCRNode current = path.startsWith("/") ? getRoot() : this;
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens() && current != null) {
            String name = st.nextToken();
            if (name.equals(".")) {
                continue;
            }
            if (name.equals("..")) {
                current = current.getParent();
            } else {
                current = current.getChild(name);
            }
        }
        return current;
    }

    /**
     * Returns the content of this node for output. For a directory, it will
     * return null.
     * 
     * @return the content of the file
     */
    public MCRContent getContent() throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return attrs.isRegularFile() ? doGetContent(attrs) : null;
    }

    private MCRPathContent doGetContent(BasicFileAttributes attrs) {
        return new MCRPathContent(path, attrs);
    }

    /**
     * Returns the content of this node for random access read. Be sure not to
     * write to the node using the returned object, use just for reading! For a
     * directory, it will return null.
     * 
     * @return the content of this file, for random access
     */
    public SeekableByteChannel getRandomAccessContent() throws IOException {
        return isFile() ? Files.newByteChannel(path, StandardOpenOption.READ) : null;
    }
}
