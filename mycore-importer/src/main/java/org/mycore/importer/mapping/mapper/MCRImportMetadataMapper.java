package org.mycore.importer.mapping.mapper;

import java.util.Hashtable;

import org.jdom2.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportMetadata;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.metadata.MCRImportAbstractMetadataResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;

/**
 * This class does the default metadata mapping.
 * 
 * @author Matthias Eichner
 */
public class MCRImportMetadataMapper extends MCRImportAbstractMapper {

    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        MCRImportMetadataResolver resolver = createResolverInstance();

        if(resolver != null) {
            Element metadataChild = new Element(map.getAttributeValue("to"));
            if(resolver.resolve(map, fields, metadataChild)) {
                MCRImportMetadata metadataObject = importObject.addMetadataChild(metadataChild);
                // resolve enclosing attributes
                if(resolver instanceof MCRImportAbstractMetadataResolver) {
                    Hashtable<String, String> enclosingAttributes = ((MCRImportAbstractMetadataResolver)resolver).getEnclosingAttributes();
                    metadataObject.addAttributeMap(enclosingAttributes);
                }
            }
        }
    }

    public String getType() {
        return "metadata";
    }
}