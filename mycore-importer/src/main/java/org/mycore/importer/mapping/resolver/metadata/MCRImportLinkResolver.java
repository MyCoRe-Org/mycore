package org.mycore.importer.mapping.resolver.metadata;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConstants;

public class MCRImportLinkResolver extends MCRImportAbstractMetadataResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRImportLinkResolver.class);
    
    @Override
    protected void resolveAdditional() {
        setDefaultAttributes();
    }

    @Override
    protected boolean isValid() {
        String href = saveToElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
        if(href == null || href.equals("")) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean hasText() {
        return false;
    }

    protected void setDefaultAttributes() {
        // default type is locator
        String type = saveToElement.getAttributeValue("type", MCRConstants.XLINK_NAMESPACE); 
        if(type == null || type.equals(""))
            saveToElement.setAttribute("type", "locator", MCRConstants.XLINK_NAMESPACE);

    }

}