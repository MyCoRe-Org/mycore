package org.mycore.importer.mapping;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.importer.MCRImportConfig;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.classification.MCRImportClassificationMappingManager;
import org.mycore.importer.derivate.MCRImportDerivate;
import org.mycore.importer.event.MCRImportStatusEvent;
import org.mycore.importer.event.MCRImportStatusListener;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodel;
import org.mycore.importer.mapping.datamodel.MCRImportDatamodelManager;
import org.mycore.importer.mapping.mapper.MCRImportMapper;
import org.mycore.importer.mapping.mapper.MCRImportMapperManager;
import org.mycore.importer.mapping.processing.MCRImportMappingProcessor;
import org.mycore.importer.mapping.processing.MCRImportMappingProcessorBuilder;
import org.mycore.importer.mapping.resolver.uri.MCRImportIdGenerationURIResolver;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolver;
import org.mycore.importer.mapping.resolver.uri.MCRImportURIResolverManager;

/**
 * <p>
 * This singleton class manages and distributes all tasks associated with the
 * import mapping. Before the mapping can start, you have to call the init method
 * to set the xml import configuration file.
 * </p>
 * <p>
 * To start the mapping call <code>startMapping(..)</code>. This will map all records
 * and derivates to the <i>saveToPath</i> (defined in the mapping file). If you have
 * derivates, its important to set them before calling <code>startMapping(..)</code>.
 * The method to do this is <code>setDerivateList(..)</code>.
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

    /**
     * Contains all <code>MCRImportMappingProcessor</code> instances which
     * are important for the current import process.
     */
    private Map<String, MCRImportMappingProcessor> processorMap;

    /**
     * A list with all import ids where errors occur.
     */
    private List<String> errorList;

    private MCRImportConfig config;

    private MCRImportMapperManager mapperManager;

    private MCRImportMetadataResolverManager metadataResolverManager;

    private MCRImportURIResolverManager uriResolverManager;

    private MCRImportDatamodelManager datamodelManager;

    private MCRImportClassificationMappingManager classificationManager;

    private MCRImportMappingManager() {
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
    public boolean init(File file) throws IOException, JDOMException {
        this.outputter = new XMLOutputter(Format.getPrettyFormat());
        this.listenerList = new ArrayList<MCRImportStatusListener>();

        Element rootElement = getRootElement(file);
        // load the configuration part of the mapping file
        config = new MCRImportConfig(rootElement);

        Element mappingElement = rootElement.getChild("mapping");
        if (mappingElement == null) {
            LOGGER.error("No mapping element found in " + rootElement.getDocument().getBaseURI());
            return false;
        }

        // get the mcrobject list
        Element mcrobjectsElement = mappingElement.getChild("mcrobjects");
        if (mcrobjectsElement == null) {
            LOGGER.error("No mcrobjects element defined in mapping element at " + mappingElement.getDocument().getBaseURI());
            return false;
        }
        this.mcrObjectList = mcrobjectsElement.getContent(new ElementFilter("mcrobject"));

        // create the processor map as hash table
        processorMap = new Hashtable<String, MCRImportMappingProcessor>();

        // create error object list
        this.errorList = new ArrayList<String>();

        // create all mappers
        mapperManager = new MCRImportMapperManager();
        // create all metadata resolvers
        metadataResolverManager = new MCRImportMetadataResolverManager();
        // create datamodel manager
        datamodelManager = new MCRImportDatamodelManager(config.getDatamodelPath(), metadataResolverManager);
        // create the classification manager
        if (config.isCreateClassificationMapping())
            classificationManager = new MCRImportClassificationMappingManager(new File(config.getSaveToPath() + "classification/"));
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
        if (INSTANCE == null)
            INSTANCE = new MCRImportMappingManager();
        return INSTANCE;
    }

    private void preloadUriResolvers(Element mappingElement) {
        uriResolverManager = new MCRImportURIResolverManager();

        // load resolver
        Element resolversElement = mappingElement.getChild("resolvers");
        if (resolversElement == null) {
            LOGGER.info("No resolvers element defined.");
            return;
        }
        List<Element> resolverList = resolversElement.getContent(Filters.element());

        for (Element resolver : resolverList) {
            String prefix = resolver.getAttributeValue("prefix");
            String className = resolver.getAttributeValue("class");

            try {
                // try to create a instance of the class
                Class<?> c = Class.forName(className);
                Object o = c.newInstance();
                if (o instanceof MCRImportURIResolver) {
                    // add it to the uri resolver manager
                    uriResolverManager.addURIResolver(prefix, (MCRImportURIResolver) o);
                } else {
                    LOGGER.error("Class " + className + " doesnt extends MCRImportURIResolver!");
                }
            } catch (Exception exc) {
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
        for (Element mcrObjectElement : mcrObjectList) {
            String dmPath = mcrObjectElement.getAttributeValue("datamodel");
            try {
                getDatamodelManager().addDatamodel(config.getDatamodelPath() + dmPath);
            } catch (JDOMException e) {
                throw new JDOMException("Could not load datamodel " + dmPath, e);
            } catch (IOException e) {
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
        for (MCRImportRecord record : recordList) {
            mapAndSaveRecord(record);
        }

        // classification
        if (config.isCreateClassificationMapping())
            classificationManager.saveAllClassificationMaps();

        // derivates
        if (derivateList != null && config.isUseDerivates() && config.isCreateInImportDir())
            for (MCRImportDerivate derivate : derivateList)
                saveDerivate(derivate);

        // print error list
        if (errorList.size() > 0) {
            StringBuilder errorMsg = new StringBuilder("The following objects causes erros:");
            for (String id : errorList)
                errorMsg.append("-").append(id).append("\n");
            LOGGER.info(errorMsg.toString());
        }
    }

    /**
     * This method maps and saves a record to the file system. 
     * 
     * @param record record which have to be mapped and saved
     */
    public MCRImportObject mapAndSaveRecord(MCRImportRecord record) {
        // do the mapping
        MCRImportObject importObject = createMCRObject(record);
        // save the new import object
        if (importObject != null) {
            // test if the mcrobject has an id
            boolean idGeneration = isIdGenerationActivated(record.getName());
            if (idGeneration) {
                createDynamicIdForImportObject(importObject, record);
            }
            if (importObject.getId() == null || importObject.getId().equals("")) {
                StringBuffer errorString = new StringBuffer();
                errorString.append("No id defined for import object created by record ");
                errorString.append(record).append("!");
                if (idGeneration)
                    errorString.append(" For unknown reasons, the MCRImportMappingManager could'nt generate an Id.");
                LOGGER.error(errorString);
                return null;
            }
            // save it
            saveImportObject(importObject, record.getName());
            StringBuilder buf = new StringBuilder();
            buf.append(record.getName()).append(": ").append(importObject.getId());
            fireRecordMapped(buf.toString());
        }
        return importObject;
    }

    /**
     * This method checks if the id for a record is created by the
     * importer or from the mapping file. If the mcrobject element
     * contains an id-mapper (type attribute equals "id"), this
     * method returns false.
     * 
     * @param recordName the name of the record
     * @return true if the id is generated automatic by the importer,
     * otherwise false
     */
    private boolean isIdGenerationActivated(String recordName) {
        Element mappingElement = getMappingElement(recordName);
        List<Element> idMapList = mappingElement.getContent(new ElementFilter("map") {
            private static final long serialVersionUID = 1L;

            @Override
            public Element filter(Object obj) {
                Element e = super.filter(obj);
                if (e != null && "id".equals(e.getAttributeValue("type"))) {
                    return e;
                }
                return null;
            }
        });
        // is there an id mapping element in the mapping file?
        return idMapList.size() <= 0;
    }

    /**
     * This method creates a dynamic id for an import object based on the
     * <code>MCRImportIdGenerationURIResolver</code>. 
     * 
     * @see MCRImportIdGenerationURIResolver
     * @param importObject where to set the generated id
     * @param record
     */
    private void createDynamicIdForImportObject(MCRImportObject importObject, MCRImportRecord record) {
        String rN = record.getName();
        // set the new id by map
        Element idGenMapElement = new Element("map");
        idGenMapElement.setAttribute("type", "id");
        idGenMapElement.setAttribute("value", rN + "_");
        idGenMapElement.setAttribute("resolver", "idGen:" + rN);
        mapIt(importObject, record, idGenMapElement);
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
     * @param mappedString the record which is mapped
     */
    private void fireRecordMapped(String mappedString) {
        for (MCRImportStatusListener l : listenerList) {
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
        for (MCRImportStatusListener l : listenerList) {
            MCRImportStatusEvent e = new MCRImportStatusEvent(this, infoString);
            l.derivateSaved(e);
        }
    }

    /**
     * Saves an <code>MCRImportObject</code> to the specified folder. The save path is
     * generated by the <i>saveToPath</i> in configuration part of the import file +
     * subFolderName + the id of the import object + ".xml".
     * 
     * @param importObject the object which has to be saved 
     * @param subFolderName the path where the xml file will be saved
     */
    public void saveImportObject(MCRImportObject importObject, String subFolderName) {
        String id = importObject.getId();
        if (id == null || id.equals("")) {
            LOGGER.error("No id defined for an object of datamodel '" + importObject.getDatamodel().getPath() + "'!");
            return;
        }
        // check if the object is valid. this prints only errors
        // and does not interrupt the creation.
        if (!importObject.isValid())
            errorList.add(importObject.getId());

        // create the xml
        Element ioElement = importObject.createXML();
        StringBuilder savePath = new StringBuilder(config.getSaveToPath());
        savePath.append(subFolderName).append("/");

        FileOutputStream output = null;
        // save the new mapped object
        try {
            File folder = new File(savePath.toString());
            if (!folder.exists())
                if (!folder.mkdirs()) {
                    LOGGER.warn("Unable to create folder " + folder.getAbsolutePath() + ". Cannot save MyCoRe import object.");
                    return;
                }
            output = new FileOutputStream(folder.getAbsolutePath() + "/" + id + ".xml");
            outputter.output(new Document(ioElement), output);
        } catch (Exception e) {
            LOGGER.error("Error while saving import object.", e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ioExc) {
                    LOGGER.error("Error while closing output stream.", ioExc);
                }
            }
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
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                LOGGER.warn("Unable to create folder " + folder.getAbsolutePath() + ". Cannot save derivate " + derivate.getDerivateId());
                return;
            }
        }
        FileOutputStream output = null;
        try {
            // save the derivate xml file
            output = new FileOutputStream(folder.getAbsolutePath() + "/" + derivate.getDerivateId() + ".xml");
            outputter.output(new Document(derivateElement), output);
            // inform all listeners that a derivate is saved
            StringBuilder buf = new StringBuilder();
            buf.append("derivate: ").append(derivate.getDerivateId());
            fireDerivateSaved(buf.toString());
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ioExc) {
                    LOGGER.error("Error while closing output stream.", ioExc);
                }
            }
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
        Element mappedObject = getMappingElement(record.getName());

        if (mappedObject == null) {
            LOGGER.warn("Couldnt find match for mapping of mcrobject '" + record.getName() + "'!");
            return null;
        }
        // path to the datamodel
        String datamodelPath = mappedObject.getAttributeValue("datamodel");

        MCRImportDatamodel dm = datamodelManager.getDatamodel(datamodelPath);
        // create the MCRImportObject instance
        MCRImportObject mcrObject = new MCRImportObject(dm);

        // get the mapping processor
        MCRImportMappingProcessor mappingProcessor = null;
        String processorClassName = mappedObject.getAttributeValue("processor");
        if (processorClassName != null)
            mappingProcessor = getMappingProcessor(processorClassName);

        // preprocessing
        if (mappingProcessor != null)
            mappingProcessor.preProcessing(mcrObject, record);

        // go through every map element and map the containing fields
        List<Element> fieldMappings = mappedObject.getContent(new ElementFilter("map"));
        for (Element map : fieldMappings)
            mapIt(mcrObject, record, map);

        // postprocessing
        if (mappingProcessor != null)
            mappingProcessor.postProcessing(mcrObject, record);

        return mcrObject;
    }

    /**
     * Returns the <code>MCRImportMappingProcessor</code> instance for the className.
     * The instances are stored in the <code>processorMap</code>. If no instance was found
     * a new one is created and added to the map.
     * 
     * @param className the class name of the processor
     * @return instance of <code>MCRImportMappingProcessor</code>
     */
    private MCRImportMappingProcessor getMappingProcessor(String className) {
        MCRImportMappingProcessor processor = processorMap.get(className);
        if (processor == null) {
            processor = MCRImportMappingProcessorBuilder.createProcessorInstance(className);
            processorMap.put(className, processor);
        }
        return processor;
    }

    /**
     * Returns the mcrobject element by the given name
     * from the mapping file.
     * 
     * @param objectName the name of the mcrobject element
     * @return a mcrobject element
     */
    protected Element getMappingElement(String objectName) {
        for (Element mcrObjectElement : mcrObjectList) {
            if (objectName.equals(mcrObjectElement.getAttributeValue("name")))
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
        if (type == null || type.equals("")) {
            type = "metadata";

            // special case for classification
            String metadataName = map.getAttributeValue("to");
            if (config.isCreateClassificationMapping() && metadataName != null && !metadataName.equals("")) {
                MCRImportDatamodel dm = mcrObject.getDatamodel();
                String className = dm.getClassname(metadataName);
                if (className.equals("MCRMetaClassification"))
                    type = "classification";
            }
        }

        try {
            // try to get a mapper instance depending on the type
            MCRImportMapper mapper = mapperManager.createMapperInstance(type);
            if (mapper == null) {
                LOGGER.error("Couldnt resolve mapper " + type);
                return;
            }
            // do the mapping
            mapper.map(mcrObject, record, map);
        } catch (InstantiationException ie) {
            LOGGER.error(ie);
        } catch (IllegalAccessException iae) {
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
    public MCRImportURIResolverManager getURIResolverManager() {
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
     * Returns the configuration instance of this mapping mananager.
     * 
     * @return configuration instance
     */
    public MCRImportConfig getConfig() {
        return config;
    }

    /**
     * Returns a list with all import ids where the mapping causes error(s).
     * 
     * @return a list of import ids
     */
    public List<String> getErrorList() {
        return errorList;
    }
}
