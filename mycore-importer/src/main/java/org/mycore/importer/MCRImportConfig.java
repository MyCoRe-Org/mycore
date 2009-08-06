package org.mycore.importer;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.importer.mapping.MCRImportObject;

/**
 * This class is an xml abstraction of the config part from the
 * pica-import file. It contains the project name, the datamodel
 * path, the path where the mapped files will be saved, if
 * classification mapping is enabled and if derivates are created. 
 * 
 * @author Matthias Eichner
 */
public class MCRImportConfig {

    private static final Logger LOGGER = Logger.getLogger(MCRImportConfig.class);
    
    private String projectName;
    private String saveToPath;
    private String datamodelPath;
    private boolean createClassificationMapping;
    
    private boolean useDerivates;
    private boolean createInImportDir;
    private boolean importToMycore;
    private boolean importFilesToMycore;

    /**
     * Reads the config part of the mapping file.
     * 
     * @param rootElement
     */
    public MCRImportConfig(Element rootElement) {
        Element configElement = rootElement.getChild("config");

        // project name
        projectName = configElement.getChildText("projectName");
        // get the datamodel path
        datamodelPath = configElement.getChildText("datamodelPath");
        // save to path
        saveToPath = configElement.getChildText("saveToPath");
        // create a automatically classification mapping, default is true
        createClassificationMapping = true;
        String classString = configElement.getChildText("createClassificationMapping");
        if(classString != null && !classString.equals(""))
            createClassificationMapping = Boolean.valueOf(classString); 

        // derivate config part
        useDerivates = false;
        createInImportDir = false;
        importToMycore = false;
        importFilesToMycore = false;
        Element derivatesElement = configElement.getChild("derivates");
        if(derivatesElement != null) {
            String helpString = derivatesElement.getAttributeValue("use");
            if(helpString != null && !helpString.equals(""))
                useDerivates = Boolean.valueOf(helpString);
    
            if(useDerivates == true) {
                helpString = derivatesElement.getChildText("createInImportDir");
                if(helpString != null && !helpString.equals(""))
                    createInImportDir = Boolean.valueOf(helpString);
                helpString = derivatesElement.getChildText("importToMycore");
                if(helpString != null && !helpString.equals(""))
                    importToMycore = Boolean.valueOf(helpString);
                helpString = derivatesElement.getChildText("importFilesToMycore");
                if(helpString != null && !helpString.equals(""))
                    importFilesToMycore = Boolean.valueOf(helpString);
            }
        }
        printConfigInfo();
    }

    public void printConfigInfo() {
        LOGGER.info("************* LOGGER INFO *************");
        LOGGER.info("* project name: " + projectName);
        LOGGER.info("* datamodel path: " + datamodelPath);
        LOGGER.info("* save to path: " + saveToPath);
        LOGGER.info("* create classification mapping: " + createClassificationMapping);
        LOGGER.info("* use derivates: " + useDerivates);
        if(useDerivates) {
            LOGGER.info("* |- create in import directory: " + createInImportDir);
            LOGGER.info("* |- import to mycore: " + importToMycore);
            LOGGER.info("* |- import files to mycore: " + importFilesToMycore);
        }
        LOGGER.info("***************************************");
    }

    /**
     * Returns the name of the project.
     * 
     * @return name of the project
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the absolute path to the datamodel files.
     * 
     * @return the datamodel path
     */
    public String getDatamodelPath() {
        return datamodelPath;
    }

    /**
     * Returns the path where all generated MyCoRe xml files
     * will be saved.
     * 
     * @return path of the generated files
     */
    public String getSaveToPath() {
        return saveToPath;
    }
    
    /**
     * Returns true if the classifcation mapping files will
     * be automatically generated.
     * 
     * @return true if the classification mapping files will be created
     */
    public boolean isCreateClassificationMapping() {
        return createClassificationMapping;
    }

    /**
     * Returns true if the derivate support is enabled.
     * By default this is false.
     * 
     * @return true if derivates are used
     */
    public boolean isUseDerivates() {
        return useDerivates;
    }
    
    /**
     * Checks if derivates are saved to the import directory.
     * 
     * @return true if the derivates are saved
     */
    public boolean isCreateInImportDir() {
        return createInImportDir;
    }
    
    /**
     * Checks if the generated derivate files from the import
     * directory are imported to mycore.
     * 
     * @return true all derivates have to import to mycore
     */
    public boolean isImportToMycore() {
        return importToMycore;
    }
    
    /**
     * Returns true if the containing derivates files (jpg, tiff,
     * zip etc.) are imported.
     * 
     * @return if the files from the derivate are imported
     */
    public boolean isImportFilesToMycore() {
        return importFilesToMycore;
    }
}