package org.mycore.importer.mapping.resolver.metadata;

public class MCRImportMetaXMLResolver extends MCRImportAbstractMetadataResolver {

    protected boolean isValid() {
        return saveToElement.getChildren().size() > 0;
    }

    protected boolean hasChildren() {
        return true;
    }
}