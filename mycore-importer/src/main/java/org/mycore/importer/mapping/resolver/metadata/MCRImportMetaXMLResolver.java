package org.mycore.importer.mapping.resolver.metadata;


public class MCRImportMetaXMLResolver extends MCRImportAbstractMetadataResolver {

    protected boolean checkValidation() {
        if(metadataChild.getChildren().size() > 0)
            return true;
        return false;
    }

    protected boolean hasChildren() {
        return true;
    }
}