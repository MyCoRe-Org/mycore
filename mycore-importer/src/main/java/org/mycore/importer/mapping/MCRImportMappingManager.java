package org.mycore.importer.mapping;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.importer.MCRImportConfig;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.classification.MCRImportClassificationMappingManager;
import org.mycore.importer.derivate.MCRImportDerivate;
import org.mycore.importer.derivate.MCRImportDerivateFileManager;
import org.mycore.importer.event.MCRImportStatusEvent;
import org.mycore.importer.event.MCRImportStatusListener;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodelManager;
import org.mycore.importer.mapping.mapper.MCRImportMapper;
import org.mycore.importer.mapping.mapper.MCRImportMapperManager;
import org.mycore.importer.mapping.processing.MCRImportMappingProcessor;
import org.mycore.importer.mapping.processing.MCRImportMappingProcessorBuilder;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolver;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolverMananger;

/**
 * This singleton class manages and distributes all tasks associated with the
 * import mapping.
 * 
 * @author Matthias Eichner
 */
public class MCRImportMappingManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportMappingManager.class);

    private static MCRImportMappingManager INSTANCE;

    private XMLOutputter outputter;

    private ArrayList<MCRImportStatusListener> listenerList;

    /**
     * A list of all jdom mcrobject-Elements from the mapping part
     * in the import file.
     */
    private List<Element> mcrObjectList;

    /**
     * A list of all MCRImportDerivates which have to be saved.
     */
    private List<MCRImportDerivate> derivateList;
    
    private MCRImportConfig config;

    private MCRImportMapperManager mapperManager;
    private MCRImportMetadataResolverManager metadataResolverManager;
    private MCRImportURIResolverMananger uriResolverManager;
    private MCRImportDatamodelManager datamodelManager;
    private MCRImportClassificationMappingManager classificationManager;
    private MCRImportDerivateFileManager derivateFileManager;

    private MCRImportMappingManager()  {
        this.outputter = new XMLOutputter(Format.getPrettyFormat());
        this.listenerList = new ArrayList<MCRImportStatusListener>();
    }

    /**
     * Initialize the singleton instance with the xml import
     * file. This method has to be called before mapping and
     * saving the generated xml files is possible.
     * 
     * @param file the path to the mapping file
     * @throws IOException
     * @throws JDOMException
     */
    @SuppressWarnings("unchecked")
    public boolean init(File file) throws IOException, JDOMException {
        Element rootElement = getRootElement(file);
        // load the configuration part of the mapping file
        config = new MCRImportConfig(rootElement);

        Element mappingElement = rootElement.getChild("mapping");
        if(mappingElement == null) {
            LOGGER.error("No mapping element found in " + rootElement.getDocument().getBaseURI());
            return false;
        }

        // get the mcrobject list
        Element mcrobjectsElement = mappingElement.getChild("mcrobjects");
        if(mcrobjectsElement == null) {
            LOGGER.error("No mcrobjects element defined in mapping element at " + mappingElement.getDocument().getBaseURI());
            return false;
        }
        this.mcrObjectList = mcrobjectsElement.getContent(new ElementFilter("mcrobject"));

        // create all mappers
        mapperManager = new MCRImportMapperManager();
        // create all metadata resolvers
        metadataResolverManager = new MCRImportMetadataResolverManager();
        // create datamodel manager
        datamodelManager = new MCRImportDatamodelManager(config.getDatamodelPath(), metadataResolverManager);
        // create the classification manager
        if(config.isCreateClassificationMapping())
            classificationManager = new MCRImportClassificationMappingManager(new File(config.getSaveToPath() + "classification/"));
        if(config.isUseDerivates())
            derivateFileManager = new MCRImportDerivateFileManager(new File(config.getSaveToPath() + "derivates/"), true);
        // preload all uri resolvers from the mapping element
        preloadUriResolvers(mappingElement);
        // preloads the datamodel files which are stated in the mcrobject elements
        preloadDatamodel();

        return true;
    }

    /**
     * Returns the jdom root element from the given file.
     *  
     * @param file the file which is parsed
     * @return the root element of the file
     * @throws IOException
     * @throws JDOMException
     */
    private Element getRootElement(File file) throws IOException, JDOMException {
        // load the mapping xml file document
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        // set the root element
        return document.getRootElement();
    }

    /**
     * Returns the singleton instance of this class.
     * 
     * @return instance of this class
     */
    public static MCRImportMappingManager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MCRImportMappingManager();
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private void preloadUriResolvers(Element mappingElement) {
        uriResolverManager = new MCRImportURIResolverMananger();

        // load resolver
        Element resolversElement = mappingElement.getChild("resolvers");
        if(resolversElement == null)
            return;
        List<Element> resolverList = resolversElement.getContent(new ElementFilter());

        for(Element resolver : resolverList) {
            String prefix = resolver.getAttributeValue("prefix");
            String className = resolver.getAttributeValue("class");

            try {
                // try to create a instance of the class
                Class<?> c = Class.forName(className);
                Object o = c.newInstance();
                if(o instanceof MCRImportURIResolver) {
                    // add it to the uri resolver manager
                    uriResolverManager.addURIResolver(prefix, (MCRImportURIResolver)o);
                } else {
                    LOGGER.error("Class " + className + " doesnt extends MCRImportURIResolver!");
                }
            } catch(Exception exc) {
                LOGGER.error(exc);
            }
        }
    }

    /**
     * Preloads the datamodels of each mcrobject element.
     * 
     * @throws IOException
     * @throws JDOMException
     */
    private void preloadDatamodel() throws IOException, JDOMException {
        for(Element mcrObjectElement : mcrObjectList) {
            String dmPath = mcrObjectElement.getAttributeValue("datamodel");
            try {
                getDatamodelManager().addDatamodel(config.getDatamodelPath() + dmPath);
            } catch(JDOMException e) {
                throw new JDOMException("Could not load datamodel " + dmPath, e);
            } catch(IOException e) {
                LOGGER.error(e);
                throw new IOException("Could not load datamodel " + dmPath);
            }
        }
    }

    /**
     * Sets a list of <code>MCRImportDerivate</code>s. The whole list will be
     * saved as xml files to the derivate folder at the import directory. So 
     * call this method before start the mapping.
     * 
     * @param derivateList a list of MCRImportDerivates
     */
    public void setDerivateList(List<MCRImportDerivate> derivateList) {
        this.derivateList = derivateList;
    }

    /**
     * Returns a list of all <code>MCRImportDerivate</code>s.
     * 
     * @return a list of MCRImportDerivates
     */
    public List<MCRImportDerivate> getDerivateList() {
        return derivateList;
    }

    /**
     * This method start the whole mapping part of the importer.
     * The given list of records will be mapped and saved to the
     * file system. In addition, if classification mapping
     * is activated, these files will be also saved. Furthermore,
     * if a derivate list is set, these xml files are generated.
     * 
     * @param recordList a list of records which will be mapped
     */
    public void startMapping(List<MCRImportRecord> recordList) {
        // records
        for(MCRImportRecord record : recordList) {
            mapAndSaveRecord(record);
        }

        // classification
        if(config.isCreateClassificationMapping())
            classificationManager.saveAllClassificationMaps();

        // derivates
        if(derivateList != null && config.isUseDerivates() && config.isCreateInImportDir()) {
            for(MCRImportDerivate derivate : derivateList) {
                saveDerivate(derivate);
            }
            try {
                derivateFileManager.save();
            } catch(IOException ioExc) {
                LOGGER.error(ioExc);
            }
        }
    }

    /**
     * This method maps and saves a record to the file system. 
     * 
     * @param record record which have to be mapped and saved
     */
    public void mapAndSaveRecord(MCRImportRecord record) {
        // do the mapping
        MCRImportObject importObject = createMCRObject(record);
        // save the new import object
        if(importObject != null) {
            saveImportObject(importObject, record.getName());
            StringBuffer buf = new StringBuffer();
            buf.append(record.getName()).append(": ").append(importObject.getId());
            fireRecordMapped(buf.toString());
        }
    }

    /**
     * Use this method to register a listener and get informed
     * about the mapping progress.
     * 
     * @param l the listener to add
     */
    public void addStatusListener(MCRImportStatusListener l) {
        listenerList.add(l);
    }

    /**
     * Remove a registerd listener.
     * 
     * @param l the listener to remove
     */
    public void removeStatusListener(MCRImportStatusListener l) {
        listenerList.remove(l);
    }

    /**
     * Sends all registerd listeners that a record is
     * successfully mapped and saved to the file system.
     * 
     * @param record the record which is mapped
     */
    private void fireRecordMapped(String mappedString) {
        for(MCRImportStatusListener l : listenerList) {
            MCRImportStatusEvent e = new MCRImportStatusEvent(this, mappedString);
            l.recordMapped(e);
        }
    }        

    /**
     * Sends all registerd listeners that a derivate is
     * successfully saved to the file system.
     * 
     * @param infoString a string to describe the event
     */
    private void fireDerivateSaved(String infoString) {
        for(MCRImportStatusListener l : listenerList) {
            MCRImportStatusEvent e = new MCRImportStatusEvent(this, infoString);
            l.derivateSaved(e);
        }        
    }
    
    /**
     * Saves an imports the object to the specified folder. The save path is
     * generated by the saveToPath in config part of the import file +
     * subFolderName + the id of the import object + ".xml"
     * 
     * @param importObject the object which has to be saved 
     * @param subFolderName the path where the xml file will be saved
     */
    public void saveImportObject(MCRImportObject importObject, String subFolderName) {
        Element ioElement = importObject.createXML();
        String savePath = config.getSaveToPath() + subFolderName + "/";

        // save the new mapped object
        String id = importObject.getId();
        try {
            File folder = new File(savePath + "/");
            if(!folder.exists())
                folder.mkdirs();
            FileOutputStream output = new FileOutputStream(folder.getAbsolutePath() + "/" + id + ".xml");
            outputter.output(new Document(ioElement), output);
            output.close();
        } catch(Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * This method saves a import derivate to the file system.
     * If successfull, all registerd listener are informed.
     * 
     * @param derivate the derivate to save
     */
    public void saveDerivate(MCRImportDerivate derivate) {
        Element derivateElement = derivate.createXML();
        File folder = new File(config.getSaveToPath() + "derivates/");
        if(!folder.exists())
            folder.mkdirs();
        try {
            // save the derivate xml file
            FileOutputStream output = new FileOutputStream(folder.getAbsolutePath() + "/" + derivate.getDerivateId() + ".xml");
            outputter.output(new Document(derivateElement), output);
            output.close();
            // save the files with the derivate manager 
            derivateFileManager.addDerivateFileLinking(derivate);
            // inform all listeners that a derivate is saved
            StringBuffer buf = new StringBuffer();
            buf.append("derivate: ").append(derivate.getDerivateId());
            fireRecordMapped(buf.toString());
            fireDerivateSaved(buf.toString());
        } catch(Exception e) {
            LOGGER.error(e);
        } 
    }

    /**
     * This method does the whole mapping for one record. As a
     * result a new instance of MCRImportObject will be created,
     * which is an abstraction of the mycore xml structure.
     * 
     * @param record the record which have to be mapped
     * @return a new instance of MCRImportObject
     */
    public MCRImportObject createMCRObject(MCRImportRecord record) {
        // get the right jdom mcrobject element from the mapping file
        Element mappedObject = getMappedObject(record.getName());

        if(mappedObject == null) {
            LOGGER.warn("Couldnt find match for mapping of mcrobject " + record.getName());
            return null;
        }
        // path to the datamodel
        String datamodelPath = mappedObject.getAttributeValue("datamodel");

        MCRImportDatamodel dm = datamodelManager.getDatamodel(datamodelPath);
        // create the MCRImportObject instance
        MCRImportObject mcrObject = new MCRImportObject(dm);

        // preprocessing
        String preProcessingClass = mappedObject.getAttributeValue("preprocessing");
        if(preProcessingClass != null) {
            MCRImportMappingProcessor processor = MCRImportMappingProcessorBuilder.createProcessorInstance(preProcessingClass);
            if(processor != null)
                processor.preProcessing(mcrObject, record);
        }

        // go through every map element and map the containing fields
        @SuppressWarnings("unchecked")
        List<Element> fieldMappings = mappedObject.getContent(new ElementFilter("map"));
        for(Element map : fieldMappings) {
            mapIt(mcrObject, record, map);
        }

        // postprocessing
        String postProcessingClass = mappedObject.getAttributeValue("preprocessing");
        if(postProcessingClass != null) {
            MCRImportMappingProcessor processor = MCRImportMappingProcessorBuilder.createProcessorInstance(postProcessingClass);
            if(processor != null)
                processor.postProcessing(mcrObject, record);
        }
        return mcrObject;
    }

    /**
     * Returns the mcrobject element by the given name
     * from the mapping file.
     * 
     * @param objectName the name of the mcrobject element
     * @return a mcrobject element
     */
    protected Element getMappedObject(String objectName) {
        for(Element mcrObjectElement : mcrObjectList) {
            if(objectName.equals(mcrObjectElement.getAttributeValue("name")))
                return mcrObjectElement;
        }
        return null;
    }

    /**
     * This method creates an instance of the class MCRImportMapper depending
     * on the type attribute of the jdom map. At this instance the method
     * map will be called to map the current map-Element.
     * 
     * @param mcrObject the mycore import xml abstraction
     * @param record the record which comes from the converter 
     * @param map the map of the import file
     */
    protected void mapIt(MCRImportObject mcrObject, MCRImportRecord record, Element map) {
        // get the type
        String type = map.getAttributeValue("type");
        // if the type is empty -> use the metadata mapper
        if(type == null || type.equals("")) {
            type = "metadata";

            // special case for classification
            String metadataName = map.getAttributeValue("to");
            if(config.isCreateClassificationMapping() && metadataName != null && !metadataName.equals("")) {
                MCRImportDatamodel dm = mcrObject.getDatamodel();
                String className = dm.getClassname(metadataName);
                if(className.equals("MCRMetaClassification"))
                    type = "classification";
            }
        }

        try {
            // try to get a mapper instance depending on the type
            MCRImportMapper mapper = mapperManager.createMapperInstance(type);
            if(mapper == null) {
                LOGGER.error("Couldnt resolve mapper " + type);
                return;
            }
            // do the mapping
            mapper.map(mcrObject, record, map);
        } catch(InstantiationException ie) {
            LOGGER.error(ie);
        } catch(IllegalAccessException iae) {
            LOGGER.error(iae);
        }
    }
 
    /**
     * Returns the mapper manager.
     * 
     * @return the mapper manager
     */
    public MCRImportMapperManager getMapperManager() {
        return mapperManager;
    }

    /**
     * Returns the metadata resolver manager.
     * 
     * @return the metadata resolver manager
     */
    public MCRImportMetadataResolverManager getMetadataResolverManager() {
        return metadataResolverManager;
    }
    
    /**
     * Returns the uri resolver manager.
     * 
     * @return the uri resolver manager
     */
    public MCRImportURIResolverMananger getURIResolverManager() {
        return uriResolverManager;
    }

    /**
     * Returns the datamodel manager.
     * 
     * @return the datamodel manager
     */
    public MCRImportDatamodelManager getDatamodelManager() {
        return datamodelManager;
    }

    /**
     * Returns the classification mapping manager.
     * 
     * @return the classification mapping manager
     */
    public MCRImportClassificationMappingManager getClassificationMappingManager() {
        return classificationManager;
    }
    
    /**
     * Returns the derivate file manager.
     * 
     * @return the derivate file manager
     */
    public MCRImportDerivateFileManager getDerivateFileManager() {
        return derivateFileManager;
    }
    
    /**
     * Returns the configuration instance of this mapping mananager.
     * 
     * @return configuration instance
     */
    public MCRImportConfig getConfig() {
        return config;
    }
}