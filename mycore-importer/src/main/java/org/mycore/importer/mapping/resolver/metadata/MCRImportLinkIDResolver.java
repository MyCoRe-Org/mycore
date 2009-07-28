package org.mycore.importer.mapping.resolver.metadata;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConstants;

public class MCRImportLinkIDResolver extends MCRImportAbstractMetadataResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRImportLinkIDResolver.class);

    @Override
    protected void resolveAdditional() {
        setDefaultAttributes();
    }

    @Override
    protected boolean checkValidation() {
        String href = metadataChild.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
        if(href == null || href.equals("")) {
            return false;
        }

        /* TODO -   is xlink:title required? -> if so setDefaultAttributes
                    with metadataChild.setAttributeValue(title, href);*/
        return true;
    }

    @Override
    protected boolean hasText() {
        return false;
    }

    protected void setDefaultAttributes() {
        // default type is locator
        String type = metadataChild.getAttributeValue("type", MCRConstants.XLINK_NAMESPACE); 
        if(type == null || type.equals(""))
            metadataChild.setAttribute("type", "locator", MCRConstants.XLINK_NAMESPACE);

    }
}