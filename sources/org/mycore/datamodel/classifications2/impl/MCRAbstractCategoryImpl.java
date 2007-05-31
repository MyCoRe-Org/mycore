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
package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRClassificationDAOFactory;
import org.mycore.datamodel.classifications2.MCRClassificationID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public abstract class MCRAbstractCategoryImpl implements MCRCategory {

    protected MCRCategory root;

    private MCRClassificationID id;

    private URI URI;

    protected Set<MCRLabel> labels;

    protected List<MCRCategory> children;

    protected final ReentrantReadWriteLock childrenLock = new ReentrantReadWriteLock();

    public MCRAbstractCategoryImpl() {
        super();
    }

    public List<MCRCategory> getChildren() {
        childrenLock.readLock().lock();
        if (children == null) {
            childrenLock.readLock().unlock();
            childrenLock.writeLock().lock();
            children = MCRClassificationDAOFactory.getInstance().getChildren(this.id);
            childrenLock.writeLock().unlock();
        }
        return children;
    }

    public MCRClassificationID getId() {
        return id;
    }

    public Set<MCRLabel> getLabels() {
        return labels;
    }

    public MCRCategory getRoot() {
        return root;
    }

    public URI getURI() {
        return URI;
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
        return MCRClassificationDAOFactory.getInstance().hasChildren(this.id);
    }

    public final boolean isCategory() {
        return !isClassification();
    }

    public final boolean isClassification() {
        return (root != null && this.id.equals(root.getId()));
    }

    public void setId(MCRClassificationID id) {
        this.id = id;
    }

    public void setURI(URI uri) {
        URI = uri;
    }

}