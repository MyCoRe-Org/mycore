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
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.mycore.backend.query.helper.GenClasses;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.services.fieldquery.MCRData2Fields;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;

/**
 * MCRQueryManager handles all Events needed für the sql/hibernate-indexer
 * (create/update/delete objects of the index)
 * 
 * @author Arne Seifert
 * 
 */
public class MCRQueryManager {

    static Logger LOGGER = Logger.getLogger(MCRQueryManager.class.getName());

    private static MCRQueryManager singleton;

    public static MCRQueryManager getInstance() {
        if (singleton == null) {
            singleton = new MCRQueryManager();
        }

        return singleton;
    }

    private MCRQueryManager() {
        try {
            String uri = "resource:searchfields.xml";
            Element def = MCRURIResolver.instance().resolve(uri);
            GenClasses.loadFields(def);
            
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
            List values = MCRData2Fields.buildFields( (MCRObject)obj, "metadata" );
           
            HashMap tempMap = new HashMap();

            for (int i=0; i<values.size(); i++){
                MCRFieldValue fvNew= (MCRFieldValue)(values.get(i));
                MCRFieldDef fd = fvNew.getField();

                if (tempMap.containsKey(fd)){
                        /** add value to field **/
                        MCRFieldValue fvOld = (MCRFieldValue) tempMap.get(fd);
                        fvNew = new MCRFieldValue( fd, fvNew.getValue() + "|" + fvOld.getValue() );
                    }
                    tempMap.put(fd,fvNew);
            }
            
            values.clear();
            Iterator it = tempMap.keySet().iterator();
            while (it.hasNext()){
                values.add(tempMap.get(it.next()));
            }
            MCRQueryIndexer.getInstance().insertInQuery(obj.getId().getId(), values);
        }catch(Exception e){
            LOGGER.error("catched error: ", e);
        }
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
}
