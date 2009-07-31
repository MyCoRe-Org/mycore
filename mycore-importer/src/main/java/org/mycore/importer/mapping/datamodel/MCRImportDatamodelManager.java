package org.mycore.importer.mapping.datamodel;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.importer.mapping.MCRImportMappingManager;

/**
 * This class holds a hash table of all datamodels from the current import.
 * The key is the path to the datamodel and the value is an instance of
 * MCRImportDatamodel.
 *  
 * @author Matthias Eichner
 */
public class MCRImportDatamodelManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDatamodelManager.class);

    private Hashtable<String, MCRImportDatamodel> datamodelTable;

    public MCRImportDatamodelManager() {
        datamodelTable = new Hashtable<String, MCRImportDatamodel>();
    }

    /**
     * Opens and builds a datamodel file and add it to the hash table.
     * 
     * @param datamodelPath the path to the datamodel file.
     * @throws IOException
     * @throws JDOMException
     */
    public MCRImportDatamodel addDatamodel(String datamodelPath) throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(datamodelPath);

        // if datamodel 1 or datamodel 2
        String rootTag = document.getRootElement().getName();
        MCRImportAbstractDatamodel datamodel = null;
        if(rootTag.equals("configuration"))
            datamodel = new MCRImportDatamodel1(document);
        else if(rootTag.equals("objecttype"))
            datamodel = new MCRImportDatamodel2(document);
        else
            return null;
        datamodelTable.put(datamodelPath, datamodel);
        return datamodel;
    }

    /**
     * Checks if the datamodel is already in the hashtable.
     * 
     * @param datamodelPath the path to the datamodel
     * @return true if the datamodel is in the table, otherwise false
     */
    public boolean hasDatamodel(String datamodelPath) {
        return datamodelTable.contains(datamodelPath);
    }

    /**
     * Returns a datamodel from the hash table by the specified
     * datamodel path. If the datamodel is not found, it is
     * created automatically.
     * 
     * @param datamodelPath the path to the datamodel
     * @return instance of MCRImportDatamodel
     */
    public MCRImportDatamodel getDatamodel(String datamodelPath) {
        // first try with the path
        MCRImportDatamodel dm = datamodelTable.get(datamodelPath);
        // if null second try with datamodel path of the mapping manager
        if(dm == null) {
            String absolutPath = MCRImportMappingManager.getInstance().getConfig().getDatamodelPath();
            dm = datamodelTable.get(absolutPath + datamodelPath);

            // if the second try is null - add it if its valid
            if(dm == null)
                try {
                    dm = addDatamodel(datamodelPath);
                } catch(Exception e) {
                    LOGGER.error(e);
                }                  
        }
        return dm;
    }
}