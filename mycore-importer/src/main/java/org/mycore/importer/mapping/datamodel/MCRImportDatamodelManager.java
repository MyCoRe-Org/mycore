package org.mycore.importer.mapping.datamodel;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.importer.MCRImportManager;
import org.mycore.importer.mapping.mapper.MCRImportAbstractMapper;

public class MCRImportDatamodelManager {

    private static final Logger LOGGER = Logger.getLogger(MCRImportDatamodelManager.class);
    
    private static MCRImportDatamodelManager INSTANCE;

    private Hashtable<String, MCRImportDatamodel> datamodelTable;

    private MCRImportDatamodelManager() {
        datamodelTable = new Hashtable<String, MCRImportDatamodel>();
    }

    public static MCRImportDatamodelManager getInstance() {
        if(INSTANCE == null)
            INSTANCE = new MCRImportDatamodelManager();
        return INSTANCE;
    }

    /**
     * Clears all references of the singleton. Call this method only
     * if you are sure the singleton will be no more used.
     */
    public void destroy() {
        datamodelTable.clear();
        INSTANCE = null;
    }

    /**
     * Opens and builds the datamodel file to hold it in memory.
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

    public boolean hasDatamodel(String datamodelPath) {
        return datamodelTable.contains(datamodelPath);
    }

    public MCRImportDatamodel getDatamodel(String datamodelPath) {
        // first try with the path
        MCRImportDatamodel dm = datamodelTable.get(datamodelPath);
        // if null second try with datamodel path of the mapping manager
        if(dm == null) {
            String absolutPath = MCRImportManager.getInstance().getConfig().getDatamodelPath();
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