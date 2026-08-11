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

package org.mycore.datamodel.common;

import java.util.Collection;

import javax.naming.OperationNotSupportedException;

import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager.MCRLinkReference;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Interface which tells all the link connections of an object.
 */
public interface MCRBaseLinkProvider {

    /**
     * This method returns all the categories of an object.
     * @param object the object or derivate
     * @return a collection of categories of the object or derivate
     * @throws OperationNotSupportedException if the object is not an instance of MCRObject or MCRDerivate or if the
     * implementation {@link MCRBaseLinkProvider} does not support the provided object
     */
    default Collection<MCRCategoryID> getCategories(MCRBase object) throws OperationNotSupportedException {
        if(object instanceof MCRObject) {
            return getCategoriesOfObject((MCRObject) object);
        } else if (object instanceof MCRDerivate) {
            return getCategoriesOfDerivate((MCRDerivate) object);
        }
        throw new OperationNotSupportedException("The object is not an instance of MCRObject or MCRDerivate");
    }

    /**
     * This method returns all the categories of an object.
     * @param object the object
     * @return a collection of categories of the object
     * @throws OperationNotSupportedException if the object is not supported by the implementation
     */
    Collection<MCRCategoryID> getCategoriesOfObject(MCRObject object) throws OperationNotSupportedException;

    /**
     * This method returns all the categories of a derivate.
     * @param object the derivate
     * @return a collection of categories of the derivate
     * @throws OperationNotSupportedException if the object is not supported by the implementation
     */
    Collection<MCRCategoryID> getCategoriesOfDerivate(MCRDerivate object) throws OperationNotSupportedException;


    /**
     * This method returns all the links of an object.
     * @param object the object or derivate
     * @return a collection of links of the object or derivate
     * @throws OperationNotSupportedException if the object is not an instance of MCRObject or MCRDerivate or if the
     * implementation {@link MCRBaseLinkProvider} does not support the provided object
     */
    default Collection<MCRLinkReference> getLinks(MCRBase object) throws OperationNotSupportedException {
        if(object instanceof MCRObject obj) {
            return getLinksOfObject(obj);
        } else if (object instanceof MCRDerivate der) {
            return getLinksOfDerivate(der);
        }
        throw new OperationNotSupportedException("The object is not an instance of MCRObject or MCRDerivate");
    }

    /**
     * This method returns all the links of an object.
     * @param obj the object
     * @return a collection of links of the object
     * @throws OperationNotSupportedException if the object is not supported by the implementation
     */
    Collection<MCRLinkReference> getLinksOfObject(MCRObject obj) throws OperationNotSupportedException;

    /**
     * This method returns all the links of a derivate.
     * @param der the derivate
     * @return a collection of links of the derivate
     * @throws OperationNotSupportedException if the object is not supported by the implementation
     */
    Collection<MCRLinkReference> getLinksOfDerivate(MCRDerivate der) throws OperationNotSupportedException;


}
