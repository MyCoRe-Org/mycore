package org.mycore.importer.classification;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * This class manages all classification mapping objects. At instantiation
 * the specified directory will be searched for classification mapping files.
 * Each found file is represent by a <code>MCRImportClassificationMap</code>
 * object, which is put in a hash table. The manager supports adding, setting
 * and reading classification entries and saving the whole classification maps
 * back to the file system.
 * 
 * @author Matthias Eichner
 */
public class MCRImportClassificationMappingManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportClassificationMappingManager.class);

    private Hashtable<String, MCRImportClassificationMap> classificationMapTable;

    private File classMappingDir;

    public MCRImportClassificationMappingManager(File mappingDirectory) {
        if(!mappingDirectory.exists())
            mappingDirectory.mkdir();
        if(!mappingDirectory.isDirectory())
            LOGGER.error(mappingDirectory + " is not a directory");
        classMappingDir = mappingDirectory;
        classificationMapTable = new Hashtable<String, MCRImportClassificationMap>();
        init();
        
    }

    /**
     * Browse through the given directory (+all subdirectories) and
     * create for each classification mapping file a own instance of
     * MCRImportClassificationMap. Each classification map will
     * be added to the table.
     * 
     * @param mappingDirectory directory of the mapping files
     */
    @SuppressWarnings("unchecked")
    public void init() {
        List<Document> mappingDocuments = new ArrayList<Document>();
        buildClassificationMapDocumentList(classMappingDir, mappingDocuments);

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
    protected void buildClassificationMapDocumentList(File parentDirectory, List<Document> documentList) {
        File[] files = parentDirectory.listFiles();
        for(File file : files) {
            if(file.isDirectory())
                buildClassificationMapDocumentList(file, documentList);
            else if(file.getName().endsWith(".xml")) {
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
     * Checks if all import- and mycore values in all maps are set. If a value
     * is null or like "" false will be returned.
     * 
     * @return true if all values are set, otherwise false
     */
    public boolean isCompletelyFilled() {
        // go through all maps
        for(MCRImportClassificationMap map : classificationMapTable.values()) {
            // check if the map is completly filled
            if(!map.isCompletelyFilled())
                return false;
        }
        return true;
    }

    /**
     * Returns the associated mycore value from the classification mapping table.
     * 
     * @param classId the classification mapping id
     * @param importValue the associated import value
     * @return
     */
    public String getMyCoReValue(String classId, String importValue) {
        MCRImportClassificationMap map = classificationMapTable.get(classId);
        if(map == null)
            return null;
        return map.getMyCoReValue(importValue);
    }

    /**
     * Checks if the import value is set in the given classification.
     * Returns true if the classification is set in the table and
     * the import value was found
     * 
     * @param classId the classification id where to search the import value
     * @param importValue the value which have to be searched
     * @return true if the value is defined, otherwise false
     */
    public boolean containsImportValue(String classId, String importValue) {
        MCRImportClassificationMap map = classificationMapTable.get(classId);
        if(map == null)
            return false;
        return map.getTable().containsKey(importValue);
    }

    /**
     * Adds the import value to the classification map. The mycore value
     * will be null. If no classification map was found in the table a
     * new one will be generated. If the import value was already set,
     * this method do nothing.
     * 
     * @param classId the classification id where to add the import value
     * @param importValue the value which have to be added
     */
    public void addImportValue(String classId, String importValue) {
        MCRImportClassificationMap map = classificationMapTable.get(classId);
        if(map == null) {
            map = new MCRImportClassificationMap(classId);
            classificationMapTable.put(classId, map);
        }
        if(!map.getTable().contains(importValue))
            map.addPair(importValue, null);
    }

    /**
     * Adds the import value to the classification map. The mycore value
     * will be null. If no classification map was found in the table a
     * new one will be generated. If the import value was already set,
     * this method overwrittes the old one.
     * 
     * @param classId the classification id where to set the import value
     * @param importValue the value which have to be set
     */
    public void setImportValue(String classId, String importValue) {
        MCRImportClassificationMap map = classificationMapTable.get(classId);
        if(map == null) {
            map = new MCRImportClassificationMap(classId);
            classificationMapTable.put(classId, map);
        }
        if(map.getTable().contains(importValue)) {
            map.removePair(importValue);
        }
        map.addPair(importValue, null);
    }

    /**
     * Returns a collection of all loaded classification mapping files.
     * 
     * @return a collection of classification mapping files
     */
    public Collection<MCRImportClassificationMap> getClassificationMapList() {
        return classificationMapTable.values();
    }

    /**
     * Saves a single classification map to the file system. The folder is
     * set by the init method and the file name is the classId.
     * 
     * @param classId the id of the classification which have to be saved
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveClassificationMap(String classId) throws FileNotFoundException, IOException {
        MCRImportClassificationMap map = classificationMapTable.get(classId);
        if(map == null)
            return;
        Element rootElement = map.createXML();
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = new FileOutputStream(classMappingDir.getAbsoluteFile() + "/" + classId + ".xml");
        outputter.output(new Document(rootElement), output);
    }
    
    /**
     * Saves the whole classification mapping table to the file system.
     * The folder is set by the init method and the file name for each
     * classification map is the classId.
     */
    public void saveAllClassificationMaps() {
        for(String classId : classificationMapTable.keySet()) {
            try {
                saveClassificationMap(classId);
            } catch(Exception e) {
                LOGGER.error("Couldnt save classification mapping file with id " + classId, e);
            }
        }
    }
}