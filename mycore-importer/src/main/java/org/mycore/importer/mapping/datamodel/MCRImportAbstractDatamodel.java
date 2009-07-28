package org.mycore.importer.mapping.datamodel;

import org.jdom.Document;
import org.jdom.Element;

public abstract class MCRImportAbstractDatamodel implements MCRImportDatamodel {

    protected Document datamodel;

    /**
     * Temporary element to store the last metadata entry. Used to increase
     * performance.
     */
    protected Element cachedMetadataElement;

    public MCRImportAbstractDatamodel(Document datamodel) {
        this.datamodel = datamodel;
        this.cachedMetadataElement = null;
    }

    protected void setCachedMetadataElement(Element newCachedElement) {
        this.cachedMetadataElement = newCachedElement;
    }

    protected Element getCachedMetadataElement() {
        return cachedMetadataElement;
    }

    public String getPath() {
        return datamodel.getBaseURI();
    }

    /**
     * Checks if the element with the metadataName is cached.
     * 
     * @param metadataName the name of the metadata element
     * @return true if the element is cached, otherwise false
     */
    protected abstract boolean isCached(String metadataName);

}