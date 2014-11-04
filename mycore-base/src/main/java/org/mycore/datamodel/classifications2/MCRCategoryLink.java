package org.mycore.datamodel.classifications2;

/**
 * Link between a category and an object.
 * 
 * @author Matthias Eichner
 */
public interface MCRCategoryLink {

    public MCRCategory getCategory();

    public MCRCategLinkReference getObjectReference();

}
