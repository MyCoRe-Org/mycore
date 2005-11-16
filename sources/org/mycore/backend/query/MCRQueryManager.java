/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.query;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.mycore.backend.query.helper.GenClasses;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRMetadata2Fields;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcherBase;

/**
 * MCRQueryManager handles all Events needed für the sql/hibernate-indexer
 * (create/update/delete objects of the index)
 * 
 * @author Arne Seifert
 * 
 */
public class MCRQueryManager extends MCRSearcherBase {
    /** the logger */
    static Logger LOGGER = Logger.getLogger(MCRQueryManager.class.getName());

    private Document doc = new Document();

    private static String searchfield = "";

    protected HashMap searchfields = new HashMap();

    private static MCRConfiguration config;

    private static MCRQueryManager singleton;

    public static MCRQueryManager getInstance() {
        if (singleton == null) {
            singleton = new MCRQueryManager();
        }

        return singleton;
    }

    private MCRQueryManager() {
        try {
            config = MCRConfiguration.instance();
            searchfield = config.getString("MCR.QuerySearchFields", "searchfields.xml");
            loadFields();
            if (MCRQueryIndexer.queryManager != null) {
            	MCRQueryIndexer.getInstance().updateConfiguration();
            }
        } catch (Exception e) {
            LOGGER.error("catched error: ", e);
            throw new MCRException("MCRQueryManager error", e);
        }
    }

    public void createDataBase() {
        MCRQueryIndexer.getInstance().initialLoad();
    }

    /**
     * method creates new object for indexer
     * 
     * @param objBase
     */
    public void create(MCRBase obj) {
        try{
            MCRXMLTableManager manager = MCRXMLTableManager.instance();
            Document metadata = (Document) MCRXMLHelper.parseXML(manager.retrieve(obj.getId()), false);

            List values = MCRMetadata2Fields.buildFields(metadata, obj.getId().getTypeId(), MCRMetadata2Fields.METADATA);
           
            HashMap tempMap = new HashMap();

            for (int i=0; i<values.size(); i++){
                Element tmpel= (Element) values.get(i);
                Attribute objtype = tmpel.getAttribute("objects");
                
                /** check correct object type **/
                if (objtype == null || objtype.getValue().indexOf(obj.getId().getTypeId())!=-1){
                    if (tempMap.containsKey(tmpel.getAttributeValue("name"))){
                        /** add value to field **/
                        Element field = (Element) tempMap.get(tmpel.getAttributeValue("name")); 
                        Attribute att = field.getAttribute("value");
                        att.setValue(att.getValue() + "|" + tmpel.getAttributeValue("value"));
                        field.removeAttribute("value");
                        field.setAttribute(att);
                    }else{
                        /** insert new field **/
                        tempMap.put(tmpel.getAttributeValue("name"),tmpel);
                    }   
                }
            }
            
            values.clear();
            Iterator it = tempMap.keySet().iterator();
            while (it.hasNext()){
                values.add((Element) tempMap.get(it.next()));
            }
            MCRQueryIndexer.getInstance().insertInQuery(obj.getId().getId(), values);
        }catch(Exception e){
            LOGGER.error("catched error: ", e);
        }
    }
    
    private void loadFields() {
        try {
            SAXBuilder builder = new SAXBuilder();
            InputStream in = this.getClass().getResourceAsStream("/" + searchfield);

            if (in == null) {
                String msg = "Could not find configuration file " + searchfield + " in CLASSPATH";
                throw new MCRConfigurationException(msg);
            }

            builder.setValidation(false);
            doc = builder.build(in);
            in.close();
            searchfields = GenClasses.loadFields(doc);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public Element getField(String name) {
        if (searchfields.containsKey(name)) {
            return (Element) searchfields.get(name);
        } else {
            return null;
        }
    }

    public HashMap getQueryFields() {
        return searchfields;
    }

    public void loadType(String type) {
        try {
            ArrayList objectID = new ArrayList();
            objectID = MCRXMLTableManager.instance().retrieveAllIDs(type);

            // object loop
            for (int i = 0; i < objectID.size(); i++) {
                MCRObjectID objectid = new MCRObjectID((String) objectID.get(i));
                MCRObject obj = new MCRObject();
                obj.receiveFromDatastore(objectid);
                create(obj);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * temporary Method only for test purposes
     * 
     */
    public String getQuery() {
        String ret = "";
        InputStream in = this.getClass().getResourceAsStream("/query1.xml");

        try {
            ret = new XMLOutputter().outputString(new SAXBuilder().build(in));
            in.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return ret;
    }

    /**
     * This class builds indexes of meta data objects.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        try {
            MCRQueryManager.getInstance().create(obj);
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * Updates Object in SQL index.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleObjectDeleted(evt, obj);
        handleObjectCreated(evt, obj);
    }

    /**
     * Deletes Object in SQL index.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        try {
            MCRQueryIndexer.getInstance().deleteObject(obj.getId());
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public MCRResults search(MCRCondition condition, List order, int maxResults) {
        return MCRQuerySearcher.getInstance().search(condition, order, maxResults);
    }

}
