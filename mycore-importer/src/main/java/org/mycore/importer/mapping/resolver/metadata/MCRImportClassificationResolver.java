package org.mycore.importer.mapping.resolver.metadata;

import org.apache.log4j.Logger;

public class MCRImportClassificationResolver extends MCRImportAbstractMetadataResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRImportClassificationResolver.class);

    protected boolean isValid() {
        // check if classid & categid are set
        String classid = saveToElement.getAttributeValue("classid");
        if(classid == null || classid.equals("")) {
            String msg =    "Couldnt map classification element fields \"" + map.getAttributeValue("fields") +
                            "\" to \"" + map.getAttributeValue("to") +"\" because classid is empty";
            LOGGER.error(msg);
            return false;
        }
        // its possible that the categ id is empty. in this case return null,
        // so that the classification is not added to the xml structure.
        String categid = saveToElement.getAttributeValue("categid");
        if(categid == null || categid.equals(""))
            return false;
        return true;
    }

    @Override
    protected boolean hasText() {
        return false;
    }
}