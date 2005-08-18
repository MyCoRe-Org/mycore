package org.mycore.backend.query;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.mycore.backend.query.helper.GenClasses;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;


public class MCRQueryManager {
    
    /** the logger */
    static Logger LOGGER = Logger.getLogger(MCRQueryManager.class.getName());
    
    
    private static MCRQueryIndexerInterface indexer;
    private static MCRQuerySearcherInterface searcher;
    
    private static String searchfield = "";
    protected HashMap searchfields = new HashMap();
    
    private static MCRConfiguration config;
    protected static MCRQueryManager singleton;
    
    public static MCRQueryManager getInstance(){
        if (singleton == null){
            singleton = new MCRQueryManager();
            indexer.updateConfiguration();
        }
        return singleton;
    }
    
    public MCRQueryManager(){
        try {
            config = MCRConfiguration.instance();
            indexer = (MCRQueryIndexerInterface) Class.forName(config.getString("MCR.QueryIndexer_class_name")).newInstance();
            searchfield = config.getString("MCR.QuerySearchFields", "searchfields.xml");
            searcher = (MCRQuerySearcherInterface) Class.forName(config.getString("MCR.QuerySearcher_class_name")).newInstance();
            loadFields();
        } catch (Exception e) {
            LOGGER.error(e);
            throw new MCRException("MCRQueryManager error", e);
        }

    }

    public static void initialLoad(){
        getInstance();
        indexer.initialLoad();
    }
    
    public static void refreshObject(String objectID){
        getInstance();
        indexer.updateObject(new MCRObjectID(objectID));
    }
    
    public static void deleteObject(String objectID){
        getInstance();
        indexer.deleteObject(new MCRObjectID(objectID));
    }
    
    public static void runQuery(int no){
        getInstance();
        searcher.runQuery(no);
    }
    
    private void loadFields(){
        try{
            SAXBuilder builder = new SAXBuilder();
            Document doc = new Document();
            InputStream in = this.getClass().getResourceAsStream("/" + searchfield);

            if (in == null) {
                String msg = "Could not find configuration file " + searchfield + " in CLASSPATH";
                throw new MCRConfigurationException(msg);
            }
            doc = builder.build(in);
            in.close();
            searchfields = GenClasses.loadFields(doc);
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    public Element getField(String name){
        if (searchfields.containsKey(name)){
            return (Element) searchfields.get(name);
        }else{
            return null;
        }
    }

    public HashMap getQueryFields(){
        return searchfields;
    }
    
    
    public void insertObject(MCRObjectID objectid){
        LOGGER.info("  insert object with id: "+ objectid.getId());
        MCRXMLTableManager manager = MCRXMLTableManager.instance();
        Document metadata = (Document) MCRXMLHelper.parseXML(manager.retrieve(objectid), false);
        Iterator it = searchfields.keySet().iterator();
        List values = new LinkedList();
        
        String strValue = "";
        // field loop: childs of searchfields = field
        while (it.hasNext()){
            String key = (String) it.next();
            Element el = (Element) searchfields.get(key);
            
            // path loop: children of fields = path
            String path="";
            if (el.getAttributeValue("value")!= null){

                StringTokenizer tokenizer = new StringTokenizer(el.getAttributeValue("xpath"),"|");
                strValue="";
                while ( tokenizer.hasMoreTokens() ){
                    path="." + tokenizer.nextToken();

                    if (el.getAttributeValue("value").contains("normalize-space(text())")){
                        path += "/text()";
                    }else if(el.getAttributeValue("value").contains(":")){
                        path +=  "/@" + el.getAttributeValue("value").substring(el.getAttributeValue("value").indexOf(":")+1);
                    }else{
                        path += "/" + el.getAttributeValue("value");
                    }
                    
                    try{
                        XPath xpath = XPath.newInstance(path);
                        List list = xpath.selectNodes(metadata);
                        
                        // element loop: children of given xpath in metadata
                        if (list.size()>0){
                            for (int k=0; k<list.size(); k++){
                                Object obj = list.get(k);
                                if(obj.getClass().getName().equals("org.jdom.Text")){
                                    Text t = (Text) list.get(k);
                                    strValue += t.getText();
                                }else{
                                    Attribute a = (Attribute) list.get(k);
                                    strValue += a.getValue();
                                }
                                if (k < list.size()-1)
                                    strValue += "|";
                            }
                        }
                    }catch(Exception e){
                        //strValue="";
                    }
                }
                
            }
            
            values.add(strValue);
        }
        // save values in db
        indexer.insertInQuery((String) objectid.getId(), values);
    }
    
    public void loadType(String type){
        try{
            ArrayList objectID = new ArrayList();
            objectID = MCRXMLTableManager.instance().retrieveAllIDs(type);
            
            // object loop
            for (int i=0; i< objectID.size(); i++){
                MCRObjectID objectid = new MCRObjectID ((String) objectID.get(i));
                insertObject(objectid);
            }
        }catch(Exception e){
            LOGGER.error(e);
            e.printStackTrace();
        }  
    }

}
