/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.ifs;

import org.mycore.common.*;
import java.util.*;
import java.io.*;
import javax.servlet.http.*;

/**
 * Represents a directory node with its metadata and content.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRDirectory extends MCRFilesystemNode {
    /** The IDs of all child nodes in this directory * */
    private Vector childrenIDs;

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
     * the given ownerID. The directory is assumed to be a standalone "root
     * directory" that has no parent.
     * 
     * @param name
     *            the name of the new MCRDirectory
     * @param ownerID
     *            any ID String of the logical owner of this file
     */
    public MCRDirectory(String name, String ownerID) {
        super(name, ownerID);
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
     * 
     * @throws MCRUsageException
     *             if that directory already contains a child with that name
     */
    public MCRDirectory(String name, MCRDirectory parent) {
        super(name, parent);
        storeNew();
    }

    /**
     * Internal constructor, do not use on your own.
     */
    MCRDirectory(String ID, String parentID, String ownerID, String name,
            String label, long size, GregorianCalendar date, int numchdd,
            int numchdf, int numchtd, int numchtf) {
        super(ID, parentID, ownerID, name, label, size, date);

        this.numChildDirsHere = numchdd;
        this.numChildFilesHere = numchdf;
        this.numChildFilesTotal = numchtf;
        this.numChildDirsTotal = numchtd;
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
        return (MCRDirectory) (MCRFilesystemNode.getNode(ID));
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
        return (MCRDirectory) (MCRFilesystemNode.getRootNode(ownerID));
    }

    /**
     * Adds a child node to this directory.
     * 
     * @param child
     *            the new child
     */
    protected void addChild(MCRFilesystemNode child) {
        if (child.parentID.equals(this.ID)) {
            if (child instanceof MCRFile)
                this.numChildFilesHere++;
            else
                this.numChildDirsHere++;

            if (childrenIDs != null)
                childrenIDs.addElement(child.getID());
        }

        if (child instanceof MCRFile)
            this.numChildFilesTotal++;
        else
            this.numChildDirsTotal++;

        this.lastModified = new GregorianCalendar();

        manager.storeNode(this);

        if (hasParent())
            getParent().addChild(child);
    }

    /**
     * Removes a child node from this directory
     * 
     * @param child
     *            the child to be removed from this directory
     */
    protected void removeChild(MCRFilesystemNode child) {
        if (child.parentID.equals(this.ID)) {
            if (child instanceof MCRFile)
                this.numChildFilesHere--;
            else
                this.numChildDirsHere--;

            if (childrenIDs != null)
                childrenIDs.removeElement(child.getID());
        }

        if (child instanceof MCRFile)
            this.numChildFilesTotal--;
        else
            this.numChildDirsTotal--;

        this.lastModified = new GregorianCalendar();

        manager.storeNode(this);

        if (hasParent())
            getParent().removeChild(child);
    }

    /**
     * Returns all direct child nodes in this directory.
     * 
     * @return a possibly empty array of MCRFilesystemNode objects
     */
    public MCRFilesystemNode[] getChildren() {
        ensureNotDeleted();

        if (childrenIDs == null)
            childrenIDs = manager.retrieveChildrenIDs(ID);

        MCRFilesystemNode[] children = new MCRFilesystemNode[childrenIDs.size()];
        for (int i = 0; i < childrenIDs.size(); i++) {
            String childID = (String) (childrenIDs.get(i));
            children[i] = manager.retrieveNode(childID);
        }

        return children;
    }

    /**
     * Returns all direct child nodes in this directory, sorted by the given
     * Comparator implementation. You may use one of the Comparators defined as
     * static constants in this class, or build your own Comparator.
     * 
     * @param sortOrder
     *            the Comparator to be used to sort the children
     * @return a possibly empty array of MCRFilesystemNode objects
     */
    public MCRFilesystemNode[] getChildren(Comparator sortOrder) {
        ensureNotDeleted();

        MCRArgumentChecker.ensureNotNull(sortOrder, "sort order");

        MCRFilesystemNode[] array = getChildren();
        Arrays.sort(array, sortOrder);
        return array;
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
        return (child != null);
    }

    /**
     * Returns the direct child of this directory at the given position in the
     * natural order of the children
     * 
     * @param the
     *            index of the child in the list of children
     */
    public MCRFilesystemNode getChild(int index) {
        ensureNotDeleted();

        if (childrenIDs == null)
            return getChildren()[index];
        else
            return manager.retrieveNode((String) (childrenIDs.get(index)));
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

        if (name.equals("."))
            return this;
        else if (name.equals(".."))
            return (hasParent() ? getParent() : null);
        else
            return manager.retrieveChild(ID, name);
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
            if (path.equals("/"))
                return base;
            else
                path = path.substring(1);
        }

        int index = path.indexOf("/");
        int end = (index == -1 ? path.length() : index);
        String name = path.substring(0, end);

        MCRFilesystemNode child = getChild(name);
        if (child == null)
            return null; // Not found
        if (path.indexOf("/", index) == -1)
            return child; // Found
        if (!(child instanceof MCRDirectory))
            return null; // Not a directory

        MCRDirectory dir = (MCRDirectory) child;
        return dir.getChildByPath(path.substring(end + 1)); // Look in child dir
    }

    /**
     * Returns true if this directory is not empty and therefore contains any
     * subdirectories or files.
     */
    public boolean hasChildren() {
        ensureNotDeleted();
        return (getNumChildren(NODES, HERE) > 0);
    }

    /** Constant for choosing file nodes * */
    public final static int FILES = 1;

    /** Constant for choosing directory nodes * */
    public final static int DIRECTORIES = 2;

    /** Constant for choosing any node type * */
    public final static int NODES = 3;

    /** Constant for choosing only direct child nodes of this directory * */
    public final static int HERE = 1;

    /**
     * Constant for choosing both direct and indirect child nodes contained in
     * subdirectories of this directory *
     */
    public final static int TOTAL = 2;

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
            if (nodetype == FILES)
                return this.numChildFilesTotal;
            else if (nodetype == DIRECTORIES)
                return this.numChildDirsTotal;
            else if (nodetype == (DIRECTORIES + FILES))
                return this.numChildDirsTotal + this.numChildFilesTotal;
            else
                return 0;
        } else {
            if (nodetype == FILES)
                return this.numChildFilesHere;
            else if (nodetype == DIRECTORIES)
                return this.numChildDirsHere;
            else if (nodetype == (DIRECTORIES + FILES))
                return this.numChildDirsHere + this.numChildFilesHere;
            else
                return 0;
        }
    }

    /**
     * Internal method that is called when the size of a child node has changed,
     * to update the total size of the parent directory.
     */
    protected void sizeOfChildChanged(long oldSize, long newSize) {
        this.size -= oldSize;
        this.size += newSize;
        this.lastModified = new GregorianCalendar();

        manager.storeNode(this);

        if (hasParent())
            getParent().sizeOfChildChanged(oldSize, newSize);
    }

    /**
     * Updates the date of last modification to the current date and time,
     * without changing anything else.
     */
    protected void touch() {
        this.lastModified = new GregorianCalendar();
        manager.storeNode(this);
        if (hasParent())
            getParent().touch();
    }

    /**
     * Deletes this directory and its content stored in the system
     */
    public void delete() throws MCRPersistenceException {
        ensureNotDeleted();

        for (int i = getNumChildren(NODES, HERE) - 1; i >= 0; i--)
            getChild(i).delete();

        super.delete();

        this.childrenIDs = null;
        this.numChildDirsHere = 0;
        this.numChildDirsTotal = 0;
        this.numChildFilesHere = 0;
        this.numChildFilesTotal = 0;
    }

    /** Sorts children by filename, case insensitive * */
    public final static Comparator SORT_BY_NAME_IGNORECASE = new Comparator() {
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        public int compare(Object a, Object b) {
            return ((MCRFilesystemNode) a).getName().compareToIgnoreCase(
                    ((MCRFilesystemNode) b).getName());
        }
    };

    /** Sorts children by filename, case sensitive * */
    public final static Comparator SORT_BY_NAME = new Comparator() {
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        public int compare(Object a, Object b) {
            return ((MCRFilesystemNode) a).getName().compareTo(
                    ((MCRFilesystemNode) b).getName());
        }
    };

    /** Sorts children by file size or total directory size * */
    public final static Comparator SORT_BY_SIZE = new Comparator() {
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        public int compare(Object a, Object b) {
            return (int) (((MCRFilesystemNode) a).getSize() - ((MCRFilesystemNode) b)
                    .getSize());
        }
    };

    /** Sorts children by date of last modification * */
    public final static Comparator SORT_BY_DATE = new Comparator() {
        public boolean equals(Object obj) {
            return super.equals(obj);
        }

        public int compare(Object a, Object b) {
            return ((MCRFilesystemNode) a)
                    .getLastModified()
                    .getTime()
                    .compareTo(
                            ((MCRFilesystemNode) b).getLastModified().getTime());
        }
    };

    /**
     * Creates a list of all MD5 checksums of all files that are direct or
     * indirect children of this directory and adds them to the given list
     * object.
     */
    protected void collectMD5Lines(List list) {
        MCRFilesystemNode[] nodes = getChildren();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] instanceof MCRDirectory) {
                MCRDirectory dir = (MCRDirectory) (nodes[i]);
                dir.collectMD5Lines(list);
            } else {
                MCRFile file = (MCRFile) (nodes[i]);
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

        List list = new Vector();
        collectMD5Lines(list);
        Collections.sort(list);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < list.size(); i++)
            sb.append(list.get(i)).append('\n');
        String s = sb.toString();

        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException shouldNeverBeThrown) {
            return null;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        sb.append("NumChildDirectoriesHere  = ").append(this.numChildDirsHere);
        sb.append("NumChildFilesHere        = ").append(this.numChildFilesHere);
        sb.append("NumChildDirectoriesTotal = ").append(this.numChildDirsTotal);
        sb.append("NumChildFilesTotal       = ")
                .append(this.numChildFilesTotal);
        return sb.toString();
    }
}