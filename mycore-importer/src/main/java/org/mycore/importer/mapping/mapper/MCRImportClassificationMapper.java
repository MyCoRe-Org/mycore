package org.mycore.importer.mapping.mapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.importer.MCRImportManager;
import org.mycore.importer.MCRImportRecord;
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

    private static final Logger LOGGER = Logger.getLogger(MCRImportClassificationMapper.class);
    
    private Hashtable<String, Element> rootElementTable= new Hashtable<String, Element>();
    
    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        // do the default metadata mapping
        super.map(importObject, record, map);
        
        String saveToPath = MCRImportManager.getInstance().getConfig().getSaveToPath() + 
                            "classifcations/";
        
        String to = map.getAttributeValue("to");
        MCRImportMetadata metadata = importObject.getMetadata(to);
        for(Element classElement : metadata.getChilds()) {
            // get the class- and categid
            String classid = classElement.getAttributeValue("classid");
            String categid = classElement.getAttributeValue("categid");
            
            // get the rootElement from the hashtable or if no
            // defined create a new one
            Element rootElement = rootElementTable.get(classid);
            if(rootElement == null)
                rootElement = createClassificationElement(classid);
            
            // check if the categ id is always in the xml file
            if(!containsSourceClassificationValue(rootElement, categid)) {
                // is not, add it
                Element mapElement = new Element("map");
                mapElement.setAttribute("importValue", categid);
                mapElement.setAttribute("mycoreValue", "");
                rootElement.addContent(mapElement);

                // save the root element to the file system
                try {
                    saveToFile(saveToPath + classid + ".xml" , rootElement);
                } catch(IOException ioException) {
                    LOGGER.error(ioException);
                }
            }
        }
    }

    /**
     * Checks if categid is already set in the map list.
     * 
     * @param rootElement the root element where to search
     * @param categid the categid to test
     * @return true if the categid is set, otherwise false
     */
    protected boolean containsSourceClassificationValue(Element rootElement, String categid) {
        List<Element> mapList = rootElement.getContent(new ElementFilter("map"));
        for(Element mapElement : mapList) {
            if(categid.equals(mapElement.getAttributeValue("importValue")))
                return false;
        }
        return true;
    }

    /**
     * Create a new classification root element and add it
     * to the hashtable.
     * 
     * @param type the type of the root element
     * @return the new generated root element
     */
    protected Element createClassificationElement(String type) {
        Element rootElement = new Element("classificationMapping");
        rootElement.setAttribute("id", type);
        rootElementTable.put(type, rootElement);
        return rootElement;
    }

    /**
     * Saves  the root element to the file system.
     * 
     * @param fileName the absolut path of the file
     * @param rootElement the root element which have to be saved
     * @throws IOException
     */
    protected void saveToFile(String fileName, Element rootElement) throws IOException {
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = new FileOutputStream(fileName);
        outputter.output(new Document(rootElement), output);
    }
}