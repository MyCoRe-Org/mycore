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
