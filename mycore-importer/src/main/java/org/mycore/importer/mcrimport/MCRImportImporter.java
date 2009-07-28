package org.mycore.importer.mcrimport;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.importer.MCRImportConfig;

public class MCRImportImporter {

    private static final Logger LOGGER = Logger.getLogger(MCRImportImporter.class);
    
    private MCRImportConfig config;

    private SAXBuilder builder;

    private Hashtable<String, MCRImportFileStatus> idTable = new Hashtable<String, MCRImportFileStatus>();

    public MCRImportImporter(MCRImportConfig config) {
        this.config = config;
        this.builder = new SAXBuilder();
        File mainDirectory = new File(config.getSaveToPath());
        buildIdTable(mainDirectory);
    }

    protected void buildIdTable(File dir) {
        for(File file : dir.listFiles()) {
            if(file.isDirectory())
                buildIdTable(file);
            else {
                // if is a valid import file
                Document doc = null;
                try {
                    doc = builder.build(file);
                } catch(Exception exc) {
                    continue;
                }
                Element rE = doc.getRootElement();
                if(!rE.getName().equals("mycoreobject"))
                    continue;
                String importId = rE.getAttributeValue("ID");
                if(importId == null || importId.equals(""))
                    continue;
                idTable.put(importId, new MCRImportFileStatus(importId, file.getAbsolutePath()));
            }
        }
    }

    /**
     * This method starts the import. The whole id table
     * will be passed through and every entry will be imported.
     */
    protected void startImport() {
        for(MCRImportFileStatus fs : idTable.values()) {
            // object is already imported to mycore
            if(fs.isImported())
                continue;
            try {
                importFile(fs.getFilePath());
            } catch(Exception e) {
                LOGGER.error(e);
            }
        }
    }

    protected void importFile(String filePath) throws IOException, JDOMException, MCRActiveLinkException {
        Document doc = builder.build(filePath);
        resolveReferences(doc);

        // use the xsi:noNamespaceSchemaLocation to get the type
        String type = doc.getRootElement().getAttributeValue("xsi:noNamespaceSchemaLocation", MCRConstants.XSI_NAMESPACE);
        if(type == null) {
            LOGGER.error("Couldnt get object type because there is no xsi:noNamespaceSchemaLocation defined for object " + doc.getBaseURI());
            return;
        }
        // create the next id
        MCRObjectID mcrObjId = new MCRObjectID();
        mcrObjId.setNextFreeId(config.getProjectName() + "_" + type);
        // create a new mycore object
        MCRObject mcrObject = new MCRObject();
        // set the xml part
        mcrObject.setFromJDOM(doc);
        // set the new id
        mcrObject.setId(mcrObjId);
        // save it to the database
        mcrObject.createInDatastore();
    }

    protected void importObject(String importId) throws IOException, JDOMException, MCRActiveLinkException {
        MCRImportFileStatus fs = idTable.get(importId);
        if(fs == null) {
            LOGGER.error("there is no object with the id " + importId + " defined!");
            return;
        }
        importFile(fs.getFilePath());
    }

    /**
     * Parses the document to resolve all links to other
     * objects and tries to resolve all classification
     * values.
     * 
     * @param doc the document where the references will be resolved
     */
    @SuppressWarnings("unchecked")
    protected void resolveReferences(Document doc) throws IOException, JDOMException, MCRActiveLinkException {
        // resolve links        
        Iterator<Element> it = doc.getRootElement().getDescendants(new LinkIdFilter());
        while(it.hasNext()) {
            Element linkElement = it.next();
            String linkId = linkElement.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            // try to get the mycore id from the hashtable
            MCRImportFileStatus fs = idTable.get(linkId);
            // if null -> the linked object is currently not imported -> do it
            if(fs.getMycoreId() == null)
                importObject(linkId);
            // set the new mycoreId
            if(fs.getMycoreId() != null) {
                linkElement.setAttribute("href", fs.getMycoreId(), MCRConstants.XLINK_NAMESPACE);
            } else {
                LOGGER.error("Couldnt resolve reference for link " + linkId + " in " + doc.getBaseURI());
            }
        }
        // resolve classifications
    }

    /**
     * Internal filter class which returns only true
     * if the element is a xlink. 
     */
    private class LinkIdFilter implements Filter {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean matches(Object arg0) {
            // only elements
            if(!(arg0 instanceof Element))
                return false;
            Element e = (Element)arg0;
            Element p = e.getParentElement();
            // check the class attribute of the parent element
            if(p == null || !p.getAttributeValue("class").equals("MCRMetaLinkID"))
                return false;
            // exists a href attribute and if its not empty
            String href = e.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
            if(href == null || href.equals(""))
                return false;
            return true;
        }
    }
}