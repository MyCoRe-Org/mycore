package org.mycore.importer.mapping.datamodel;

import java.util.Hashtable;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.importer.mapping.MCRImportMetadataResolverManager;

public abstract class MCRImportAbstractDatamodel implements MCRImportDatamodel {

    protected Document datamodel;

    protected MCRImportMetadataResolverManager metadataResolverManager;
    
    /**
     * A list of cached metadata elements
     */
    protected Hashtable<String, Element> cachedMetadataTable;

    public MCRImportAbstractDatamodel(Document datamodel, MCRImportMetadataResolverManager metadataResolverManager) {
        this.datamodel = datamodel;
        this.metadataResolverManager = metadataResolverManager;
        this.cachedMetadataTable = new Hashtable<String, Element>();
    }

    protected void addCachedMetadataElement(String id, Element newCachedElement) {
        this.cachedMetadataTable.put(id, newCachedElement);
    }

    protected Hashtable<String, Element> getCachedMetadataTable() {
        return cachedMetadataTable;
    }

    public String getPath() {
        return datamodel.getBaseURI();
    }

    public MCRImportMetadataResolverManager getMetadataResolverManager() {
        return metadataResolverManager;
    }

    /**
     * Checks if the element with the metadataName is cached.
     * 
     * @param metadataName the name of the metadata element
     * @return true if the element is cached, otherwise false
     */
    protected boolean isCached(String metadataName) {
        return cachedMetadataTable.containsKey(metadataName);
    }

}