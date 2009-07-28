/**
 * 
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

import org.mycore.datamodel.classifications2.MCRObjectReference;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
class MCRCategoryLink {

    int id;

    private MCRCategoryImpl category;

    private MCRObjectReference objectReference;

    MCRCategoryLink() {
        super();
    }

    MCRCategoryLink(MCRCategoryImpl category, MCRObjectReference objectReference) {
        super();
        this.category = category;
        this.objectReference = objectReference;
    }

    public MCRCategoryImpl getCategory() {
        return category;
    }

    public void setCategory(MCRCategoryImpl category) {
        this.category = category;
    }

    public MCRObjectReference getObjectReference() {
        return objectReference;
    }

    public void setObjectReference(MCRObjectReference objectReference) {
        this.objectReference = objectReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + ((objectReference == null) ? 0 : objectReference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MCRCategoryLink))
            return false;
        final MCRCategoryLink other = (MCRCategoryLink) obj;
        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;
        if (objectReference == null) {
            if (other.objectReference != null)
                return false;
        } else if (!objectReference.equals(other.objectReference))
            return false;
        return true;
    }

}
