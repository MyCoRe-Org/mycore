package org.mycore.datamodel.classifications2;

/**
 * Link between a category and an object.
 * 
 * @author Matthias Eichner
 */
public interface MCRCategoryLink {

    MCRCategory getCategory();

    MCRCategLinkReference getObjectReference();

}
