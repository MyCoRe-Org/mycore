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

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface MCRCategory {

    /**
     * @return true if this is a root category
     */
    public abstract boolean isClassification();

    /**
     * @return true if this is not a root category
     * @see #isClassification()
     */
    public abstract boolean isCategory();

    /**
     * Tells if this category has subcategories.
     * 
     * @return true if this category has subcategories
     * @see #getChildren()
     */
    public abstract boolean hasChildren();

    /**
     * Returns a list of subcategories.
     * 
     * Implementors must never return <code>null</code> if no children are
     * present. As this method may need a new call the underlaying persistence
     * layer use hasChildren() if you just want to know if subcategories are
     * present. Changes to the list may not affect the underlaying persistence
     * layer.
     * 
     * @return subcategories
     * @see #hasChildren()
     */
    public abstract List<MCRCategory> getChildren();

    /**
     * @return the id
     */
    public abstract MCRClassificationID getId();

    /**
     * @param id
     *            the id to set
     */
    public abstract void setId(MCRClassificationID id);

    /**
     * @return the labels
     */
    public abstract Set<MCRLabel> getLabels();

    /**
     * Returns the hierarchie level of this category.
     * 
     * @return 0 if this is the root category
     */
    public abstract int getLevel();

    /**
     * Returns root category (the classification).
     * 
     * @return the root category
     */
    public abstract MCRCategory getRoot();

    /**
     * Returns the URI associated with this category.
     * 
     * @return the URI
     */
    public abstract URI getURI();

    /**
     * @param uri
     *            the URI to set
     */
    public abstract void setURI(URI uri);

}