package org.mycore.importer.mapping.resolver.metadata;


public class MCRImportBooleanResolver extends MCRImportAbstractMetadataResolver {
    
    @Override
    protected boolean checkValidation() {
        String text = metadataChild.getText();
        if(Boolean.valueOf(text) == true)
            return true;
        return false;
    }

}