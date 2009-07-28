package org.mycore.importer;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mcrimport.MCRImportImporter;

/**
 * The import manager is the main administration singleton for
 * mapping and import the generated xml files to mycore.
 * 
 * @author Matthias Eichner
 */
public class MCRImportManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportManager.class);

    private static MCRImportManager INSTANCE;

    protected Element rootElement;
    protected MCRImportConfig config;

    protected MCRImportMappingManager mappingManager;

    protected MCRImportImporter importer;

    /**
     * Returns the singleton instance of this class.
     * 
     * @return instance of this class
     */
    public static MCRImportManager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MCRImportManager();
        return INSTANCE;
    }

    /**
     * Initialize the singleton instance with the mapping
     * file. This method has to be called before mapping,
     * saving or an import is possible.
     * 
     * @param file the path to the mapping file
     * @throws IOException
     * @throws JDOMException
     */
    public void init(File file) throws IOException, JDOMException {
        // load the mapping xml file document
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        // set the root element
        this.rootElement = document.getRootElement();
        // load the configuration part of the mapping file
        loadConfig();
        
        // reset internal managers
        mappingManager = null;
        importer = null;
    }

    /**
     * Loads the configuration part of the mapping file and resolves
     * it with a instance of MCRImportConfig. Saved informations are
     * the datamodel path, the save to path and uri resolver classes.
     */
    protected void loadConfig() {
        config = new MCRImportConfig(rootElement);
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
     * Returns the current mapping manager.
     * 
     * @return the mapping manager
     */
    public MCRImportMappingManager getMappingManager() {
        if(mappingManager == null) {
            try {
                mappingManager = new MCRImportMappingManager(rootElement, config);
            } catch(Exception exc) {
                LOGGER.error(exc);
            }
        }
        return mappingManager;
    }

    /**
     * Returns the current importer object.
     * 
     * @return the importer
     */
    public MCRImportImporter getImporter() {
        if(importer == null) {
            importer = new MCRImportImporter(config);
        }
        return importer;
    }
}