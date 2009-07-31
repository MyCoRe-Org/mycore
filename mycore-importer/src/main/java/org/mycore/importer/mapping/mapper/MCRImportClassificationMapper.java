package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.MCRImportMetadata;
import org.mycore.importer.mapping.MCRImportObject;

/**
 * This mapping class does the same like the metadata
 * mapper. Additional it parses the classification
 * attribute part to write the classid and the categid
 * to an external xml file. This helps the import
 * developer to know which classification are defined
 * in the source data.
 * 
 * @author Matthias Eichner
 */
public class MCRImportClassificationMapper extends MCRImportMetadataMapper {

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        // do the default metadata mapping
        super.map(importObject, record, map);

        String to = map.getAttributeValue("to");
        MCRImportMetadata metadata = importObject.getMetadata(to);
        if(metadata == null)
            return;
        for(Element classElement : metadata.getChilds()) {
            // get the class- and categid
            String classid = classElement.getAttributeValue("classid");
            String categid = classElement.getAttributeValue("categid");

            // if classification mapping enabled add the value to the manager
            if(MCRImportMappingManager.getInstance().getConfig().isCreateClassificationMapping())
                MCRImportMappingManager.getInstance().getClassificationMappingManager().addImportValue(classid, categid);
        }
    }

}