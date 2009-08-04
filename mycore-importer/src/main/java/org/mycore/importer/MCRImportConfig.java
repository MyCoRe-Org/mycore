package org.mycore.importer;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * This class is an xml abstraction of the config part from the
 * pica-import file. It contains the datamodel path, the path
 * where the mapped files will be saved and a list of resolvers
 * with their ids and class paths. The resolvers will be
 * automatically added to the MCRImportURIResolverMananger.
 * 
 * @author Matthias Eichner
 */
public class MCRImportConfig {

    private static final Logger LOGGER = Logger.getLogger(MCRImportConfig.class);

    private String projectName;
    private String saveToPath;
    private String datamodelPath;
    private boolean createClassificationMapping;

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
}