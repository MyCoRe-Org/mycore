package org.mycore.importer.mcrimport.classification;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class MCRImportClassificationMappingManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportClassificationMappingManager.class);
    
    private static MCRImportClassificationMappingManager INSTANCE;

    private Hashtable<String, MCRImportClassificationMap> classificationMapTable;

    private MCRImportClassificationMappingManager() {
        classificationMapTable = new Hashtable<String, MCRImportClassificationMap>();
    }

    public static MCRImportClassificationMappingManager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MCRImportClassificationMappingManager();
        return INSTANCE;
    }

    /**
     * Call this method only if you are certain that you no longer need
     * this singleton.
     */
    public void destroy() {
        classificationMapTable.clear();
        INSTANCE = null;
    }

    /**
     * Browse through the given directory (+all subdirectories) and
     * create for each classification mapping file a own instance of
     * MCRImportClassificationMapper. Each classification mapper will
     * be added to the table.
     * 
     * @param mappingDirectory directory of the mapping files
     */
    @SuppressWarnings("unchecked")
    public void init(File mappingDirectory) {
        if(!mappingDirectory.isDirectory())
            LOGGER.error(mappingDirectory + " is not a directory");

        List<Document> mappingDocuments = new ArrayList<Document>();
        buildClassificationMappingDocumentList(mappingDirectory, mappingDocuments);

        // go through all files
        for(Document doc : mappingDocuments) {
            Element rootElement = doc.getRootElement();
            String id = rootElement.getAttributeValue("id");
            MCRImportClassificationMap valueMapper = new MCRImportClassificationMap(id);

            List<Element> maps = rootElement.getChildren("map");
            for(Element map : maps) {
                String importValue = map.getAttributeValue("importValue");
                String mycoreValue = map.getAttributeValue("mycoreValue");
                valueMapper.addPair(importValue, mycoreValue);
            }
            classificationMapTable.put(id, valueMapper);
        }
    }

    /**
     * Creates recursive a list of classification mapping documents from
     * the given directory.
     * 
     * @param parentDirectory directory which is browsed
     * @param documentList return list of documents
     */
    protected void buildClassificationMappingDocumentList(File parentDirectory, List<Document> documentList) {
        File[] files = parentDirectory.listFiles();
        for(File file : files) {
            if(file.isDirectory())
                buildClassificationMappingDocumentList(file, documentList);
            else {
                try {
                    SAXBuilder builder = new SAXBuilder();
                    Document document = builder.build(file);
                    Element rootElement = document.getRootElement();
                    if(rootElement.getName().equals("classificationMapping"))
                        documentList.add(document);
                    else
                        LOGGER.warn("The root tag of " + document.getBaseURI() + " is not a valid!");
                } catch(Exception e) {
                    // do nothing here, because we need only valid mapping files
                }
            }
        }
    }

    /**
     * Returns the associated mycore value from the classification mapping table.
     * 
     * @param classId the classification mapping id
     * @param importValue the associated import value
     * @return
     */
    public String getMyCoReValue(String classId, String importValue) {
        MCRImportClassificationMap mapper = classificationMapTable.get(classId);
        if(mapper == null)
            return null;
        return mapper.getMyCoReValue(importValue);
    }

    /**
     * Returns a collection of all loaded classification mapping files.
     * 
     * @return a collection of classification mapping files
     */
    public Collection<MCRImportClassificationMap> getClassificationMapList() {
        return classificationMapTable.values();
    }
}