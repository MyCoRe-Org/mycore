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

package org.mycore.datamodel.ifs;

import java.io.UnsupportedEncodingException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Represents a directory node with its metadata and content.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2010-11-08 15:05:28 +0100 (Mon, 08 Nov
 *          2010) $
 */
public class MCRDirectory extends MCRFilesystemNode {
    /** Constant for choosing file nodes * */
    public static final int FILES = 1;

    /** Constant for choosing directory nodes * */
    public static final int DIRECTORIES = 2;

    /** Constant for choosing any node type * */
    public static final int NODES = 3;

    /** Constant for choosing only direct child nodes of this directory * */
    public static final int HERE = 1;

    /**
     * Constant for choosing both direct and indirect child nodes contained in
     * subdirectories of this directory *
     */
    public static final int TOTAL = 2;

    /** Sorts children by filename, case insensitive * */
    public static final Comparator<MCRFilesystemNode> SORT_BY_NAME_IGNORECASE = Comparator
        .comparing(MCRFilesystemNode::getName, String::compareToIgnoreCase);

    /** Sorts children by filename, case sensitive * */
    public static final Comparator<MCRFilesystemNode> SORT_BY_NAME = Comparator.comparing(MCRFilesystemNode::getName);

    /** Sorts children by file size or total directory size * */
    public static final Comparator<MCRFilesystemNode> SORT_BY_SIZE = Comparator
        .comparingLong(MCRFilesystemNode::getSize);

    /** Sorts children by date of last modification * */
    public static final Comparator<MCRFilesystemNode> SORT_BY_DATE = Comparator
        .comparing(MCRFilesystemNode::getLastModified);

    /** The child nodes in this directory * */
    private ConcurrentMap<String, MCRFilesystemNode> children;

    /** The number of direct subdirectories here */
    private int numChildDirsHere = 0;

    /** The total number of subdirectories below * */
    private int numChildDirsTotal = 0;

    /** The number of files that are direct children of this directory * */
    private int numChildFilesHere = 0;

    /** The total number of files below * */
    private int numChildFilesTotal = 0;

    /**
     * Creates a new, empty root MCRDirectory with the given name, belonging to
     * the given ownerID. The directory is assumed to be a standalone
     * "root directory" that has no parent.
     * @param ownerID
     *            any ID String of the logical owner of this file
     */
    public MCRDirectory(String ownerID) {
        super(ownerID, ownerID);
        storeNew();
    }

    /**
     * Creates a new, empty MCRDirectory with the given name in the parent
     * MCRDirectory.
     *
     * @param name
     *            the name of the new MCRDirectory
     * @param parent
     *            the parent directory that will contain the new child
     * @throws MCRUsageException
     *             if that directory already contains a child with that name
     */
    public MCRDirectory(String name, MCRDirectory parent) {
        this(name, parent, true);
    }

    /**
     * Creates a new, empty MCRDirectory with the given name in the parent
     * MCRDirectory.
     *
     * @param name
     *            the name of the new MCRDirectory
     * @param parent
     *            the parent directory that will contain the new child
     * @param doExistCheck
     *            checks if file with that Name already exists
     * @throws MCRUsageException
     *             if that directory already contains a child with that name
     */
    public MCRDirectory(String name, MCRDirectory parent, boolean doExistCheck) {
        super(name, parent, doExistCheck);
        storeNew();
    }

    /**
     * Internal constructor, do not use on your own.
     */
    MCRDirectory(String ID, String parentID, String ownerID, String name, String label, long size,
        GregorianCalendar date, int numchdd, int numchdf, int numchtd, int numchtf) {
        super(ID, parentID, ownerID, name, label, size, date);

        numChildDirsHere = numchdd;
        numChildFilesHere = numchdf;
        numChildFilesTotal = numchtf;
        numChildDirsTotal = numchtd;
    }

    /**
     * Returns the MCRDirectory with the given ID.
     *
     * @param ID
     *            the unique ID of the MCRDirectory to return
     * @return the MCRDirectory with the given ID, or null if no such directory
     *         exists
     */
    public static MCRDirectory getDirectory(String ID) {
        return (MCRDirectory) MCRFilesystemNode.getNode(ID);
    }

    /**
     * Returns the root MCRDirectory that has no parent and is logically owned
     * by the object with the given ID.
     *
     * @param ownerID
     *            the ID of the logical owner of that directory
     * @return the root MCRDirectory stored for that owner ID, or null if no
     *         such directory exists
     */
    public static MCRDirectory getRootDirectory(String ownerID) {
        return (MCRDirectory) MCRFilesystemNode.getRootNode(ownerID);
    }

    /**
     * Adds a child node to this directory.
     *
     * @param child
     *            the new child
     */
    protected void addChild(MCRFilesystemNode child) {
        if (child.parentID.equals(ID)) {
            if (child instanceof MCRFile) {
                numChildFilesHere++;
            } else {
                numChildDirsHere++;
            }

            if (children != null) {
                children.put(child.getName(), child);
            }
        }

        if (child instanceof MCRFile) {
            numChildFilesTotal++;
        } else {
            numChildDirsTotal++;
        }

        touch(false);
        if (hasParent()) {
            getParent().addChild(child);
        }
    }

    /**
     * Removes a child node from this directory
     *
     * @param child
     *            the child to be removed from this directory
     */
    protected void removeChild(MCRFilesystemNode child) {
        if (child.parentID.equals(ID)) {
            if (child instanceof MCRFile) {
                numChildFilesHere--;
            } else {
                numChildDirsHere--;
            }

            if (children != null) {
                children.remove(child.getName());
            }
        }

        if (child instanceof MCRFile) {
            numChildFilesTotal--;
        } else {
            numChildDirsTotal--;
        }

        touch(false);

        if (hasParent()) {
            getParent().removeChild(child);
        }
    }

    /**
     * Returns all direct child nodes in this directory.
     *
     * @return a possibly empty array of MCRFilesystemNode objects
     */
    public MCRFilesystemNode[] getChildren() {
        ensureNotDeleted();

        if (children == null) {
            children = manager.retrieveChildren(ID).stream()
                .collect(Collectors.toMap(MCRFilesystemNode::getName, f -> f, (u, v) -> {
                    throw new IllegalStateException(String.format(Locale.ROOT, "Duplicate key %s", u));
                }, ConcurrentHashMap::new));
        }

        return children.values()
            .stream()
            .toArray(i -> new MCRFilesystemNode[children.size()]);
    }

    /**
     * Returns all direct child nodes in this directory, sorted by the given
     * Comparator implementation. You may use one of the following comparators
     * constants: {@link MCRDirectory#SORT_BY_DATE},
     * {@link MCRDirectory#SORT_BY_NAME},
     * {@link MCRDirectory#SORT_BY_NAME_IGNORECASE} or
     * {@link MCRDirectory#SORT_BY_DATE}
     *
     * @param sortOrder
     *            the Comparator to be used to sort the children
     * @return a possibly empty array of MCRFilesystemNode objects
     */
    public MCRFilesystemNode[] getChildren(Comparator<MCRFilesystemNode> sortOrder) {
        ensureNotDeleted();

        return children.values()
            .stream()
            .sorted(sortOrder)
            .toArray(i -> new MCRFilesystemNode[children.size()]);
    }

    /**
     * Returns true, if this directory contains a direct child with the given
     * filename.
     *
     * @param name
     *            the name of the child file or directory
     */
    public boolean hasChild(String name) {
        ensureNotDeleted();

        MCRFilesystemNode child = getChild(name);

        return child != null;
    }

    /**
     * Returns the child node with the given filename. This method also accepts
     * the aliases "." for the current directory or ".." for the parent
     * directory.
     *
     * @param name
     *            the name of the child file or directory
     */
    public MCRFilesystemNode getChild(String name) {
        ensureNotDeleted();

        switch (name) {
            case ".":
                return this;
            case "..":
                return hasParent() ? getParent() : null;
            default:
                return Optional.ofNullable(children)
                    .map(m -> m.get(name))
                    .orElseGet(() -> manager.retrieveChild(ID, name));
        }
    }

    /**
     * Returns the node that is addressed by the given absolute or relative
     * path. If an absolute path starting with a slash "/" is given, the node
     * will be located relative to the root directory of this directory. If a
     * relative path is given, the path may contain the aliases "." for the
     * current directory or ".." for the parent directory, and the node will be
     * located relative to this directory.
     *
     * @param path
     *            the absolute path (starting with a "/") or path relative to
     *            this directory, possibly containing "." or ".." aliases
     * @return the node that matches this path, or null if no such node exists
     */
    public MCRFilesystemNode getChildByPath(String path) {
        ensureNotDeleted();

        MCRDirectory base = this;

        if (path.startsWith("/")) {
            base = getRootDirectory();

            if (path.equals("/")) {
                return base;
            }
            path = path.substring(1);
        }

        int index = path.indexOf("/");
        int end = index == -1 ? path.length() : index;
        String name = path.substring(0, end);

        MCRFilesystemNode child = getChild(name);

        if (child == null) {
            return null; // Not found
        }

        if (path.indexOf("/", index) == -1) {
            return child; // Found
        }

        if (!(child instanceof MCRDirectory)) {
            return null; // Not a directory
        }

        MCRDirectory dir = (MCRDirectory) child;

        return dir.getChildByPath(path.substring(end + 1)); // Look in child dir
    }

    /**
     * Returns true if this directory is not empty and therefore contains any
     * subdirectories or files.
     */
    public boolean hasChildren() {
        ensureNotDeleted();

        return getNumChildren(NODES, HERE) > 0;
    }

    @Override
    public MCRFileAttributes<String> getBasicFileAttributes() {
        return MCRFileAttributes
            .directory(getID(), getSize(), FileTime.fromMillis(getLastModified().getTimeInMillis()));
    }

    @Override
    public long getSize() {
        ensureNotDeleted();

        return 0;
    }

    /**
     * Returns the number of child nodes in this directory. The additional
     * parameters control what type of nodes will be counted and if only direct
     * children or all children will be counted.
     *
     * @param nodetype
     *            one of the constants FILES, DIRECTORIES, or NODES for both
     *            types
     * @param where
     *            either HERE to only count direct children, or TOTAL to count
     *            also indirect children
     */
    public int getNumChildren(int nodetype, int where) {
        ensureNotDeleted();

        if ((where & TOTAL) > 0) {
            if (nodetype == FILES) {
                return numChildFilesTotal;
            } else if (nodetype == DIRECTORIES) {
                return numChildDirsTotal;
            } else if (nodetype == DIRECTORIES + FILES) {
                return numChildDirsTotal + numChildFilesTotal;
            } else {
                return 0;
            }
        }
        if (nodetype == FILES) {
            return numChildFilesHere;
        } else if (nodetype == DIRECTORIES) {
            return numChildDirsHere;
        } else if (nodetype == DIRECTORIES + FILES) {
            return numChildDirsHere + numChildFilesHere;
        } else {
            return 0;
        }
    }

    /**
     * Internal method that is called when the size of a child node has changed,
     * to update the total size of the parent directory.
     */
    protected void sizeOfChildChanged(long sizeDiff) {
        size += sizeDiff;
        touch(false);
        if (hasParent()) {
            getParent().sizeOfChildChanged(sizeDiff);
        }
    }

    /**
     * Deletes this directory and its content stored in the system
     */
    @Override
    public void delete() throws MCRPersistenceException {
        ensureNotDeleted();

        Stream.of(getChildren()).forEach(MCRFilesystemNode::delete);
        BasicFileAttributes attrs = getBasicFileAttributes();
        MCRPath path = toPath();
        super.delete();
        MCREvent evt = new MCREvent(MCREvent.PATH_TYPE, MCREvent.DELETE_EVENT);
        evt.put(MCREvent.PATH_KEY, path);
        evt.put(MCREvent.FILEATTR_KEY, attrs);
        MCREventManager.instance().handleEvent(evt);

        children = null;
        numChildDirsHere = 0;
        numChildDirsTotal = 0;
        numChildFilesHere = 0;
        numChildFilesTotal = 0;
    }

    /**
     * Creates a list of all MD5 checksums of all files that are direct or
     * indirect children of this directory and adds them to the given list
     * object.
     */
    protected void collectMD5Lines(List<String> list) {
        MCRFilesystemNode[] nodes = getChildren();

        for (MCRFilesystemNode node : nodes) {
            if (node instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) node;
                dir.collectMD5Lines(list);
            } else {
                MCRFile file = (MCRFile) node;
                String line = file.getMD5() + " " + file.getSize();
                list.add(line);
            }
        }
    }

    /**
     * Builds a fingerprint of all file's contents of this directory, by
     * collecting the MD5 checksums of all direct or indirect child files of
     * this directory, sorting them to ascending order and writing this list
     * line by line as UTF-8 encoded bytes. This method can be used to digitally
     * sign the content of a directory in the future
     *
     * @return the fingerprint that changes when any content of a file in this
     *         directory changes
     */
    public byte[] buildFingerprint() {
        ensureNotDeleted();

        List<String> list = new Vector<>();
        collectMD5Lines(list);
        Collections.sort(list);

        StringBuilder sb = new StringBuilder();

        for (Object aList : list) {
            sb.append(aList).append('\n');
        }

        String s = sb.toString();

        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException shouldNeverBeThrown) {
            return null;
        }
    }

    @Override
    public String toString() {

        return super.toString() + "NumChildDirectoriesHere  = " + numChildDirsHere + "NumChildFilesHere        = "
            + numChildFilesHere + "NumChildDirectoriesTotal = " + numChildDirsTotal + "NumChildFilesTotal       = "
            + numChildFilesTotal;
    }
}
