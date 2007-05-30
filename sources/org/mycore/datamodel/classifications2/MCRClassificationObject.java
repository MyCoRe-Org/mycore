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
package org.mycore.datamodel.classifications2;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MCRClassificationObject {

    private MCRClassificationObject root;

    private MCRClassificationID id;

    private URI URI;

    private Set<MCRLabel> labels;

    private List<MCRClassificationObject> children;

    private int left, right;

    private final ReentrantReadWriteLock childrenLock = new ReentrantReadWriteLock();

    public MCRClassificationObject() {
    }

    public final boolean isClassification() {
        return (root != null && this.id.equals(root.id));
    }

    public final boolean isCategory() {
        return !isClassification();
    }

    public boolean hasChildren() {
        childrenLock.readLock().lock();
        try {
            if (children != null) {
                return (children.size() == 0) ? false : true;
            }
        } finally {
            childrenLock.readLock().unlock();
        }
        return MCRClassificationServiceFactory.getInstance().hasChildren(this.id);
    }

    /**
     * @return the children
     */
    public List<MCRClassificationObject> getChildren() {
        childrenLock.readLock().lock();
        if (children == null) {
            childrenLock.readLock().unlock();
            childrenLock.writeLock().lock();
            children = MCRClassificationServiceFactory.getInstance().getChildren(this.id);
            childrenLock.writeLock().unlock();
        }
        return children;
    }

    /**
     * @param children
     *            the children to set
     */
    public void setChildren(List<MCRClassificationObject> children) {
        childrenLock.writeLock().lock();
        this.children = children;
        childrenLock.writeLock().unlock();
    }

    /**
     * @return the id
     */
    public MCRClassificationID getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(MCRClassificationID id) {
        this.id = id;
    }

    /**
     * @return the labels
     */
    public Set<MCRLabel> getLabels() {
        return labels;
    }

    /**
     * @param labels
     *            the labels to set
     */
    public void setLabels(Set<MCRLabel> labels) {
        this.labels = labels;
    }

    /**
     * @return the left
     */
    public int getLeft() {
        return left;
    }

    /**
     * @param left
     *            the left to set
     */
    public void setLeft(int left) {
        this.left = left;
    }

    /**
     * @return the right
     */
    public int getRight() {
        return right;
    }

    /**
     * @param right
     *            the right to set
     */
    public void setRight(int right) {
        this.right = right;
    }

    /**
     * @return the root
     */
    public MCRClassificationObject getRoot() {
        return root;
    }

    /**
     * @param root
     *            the root to set
     */
    public void setRoot(MCRClassificationObject root) {
        this.root = root;
    }

    /**
     * @return the uRI
     */
    public URI getURI() {
        return URI;
    }

    /**
     * @param uri
     *            the uRI to set
     */
    public void setURI(URI uri) {
        URI = uri;
    }

}
