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
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
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
            MCRQueryIndexer.getInstance().updateConfiguration();
        } catch (Exception e) {
            LOGGER.error(e);
            throw new MCRException("MCRQueryManager error", e);
        }
    }

    public void createDataBase(String mcr_type, Document mcr_conf) {
        MCRQueryIndexer.getInstance().initialLoad();
    }

    /**
     * method creates new object for indexer
     * 
     * @param objBase
     */
    public void create(MCRBase obj) {
        MCRXMLTableManager manager = MCRXMLTableManager.instance();
        Document metadata = (Document) MCRXMLHelper.parseXML(manager.retrieve(obj.getId()), false);
        Iterator it = searchfields.keySet().iterator();
        List values = new LinkedList();

        String strValue = "";

        // field loop: childs of searchfields = field
        while (it.hasNext()) {
            String key = (String) it.next();
            Element el = (Element) searchfields.get(key);

            // path loop: children of fields = path
            String path = "";

            if (el.getAttributeValue("value") != null) {
                StringTokenizer tokenizer = new StringTokenizer(el.getAttributeValue("xpath"), "|");
                strValue = "";

                while (tokenizer.hasMoreTokens()) {
                    path = "." + tokenizer.nextToken();

                    if (el.getAttributeValue("value").indexOf("normalize-space(text())") != -1) {
                        path += "/text()";
                    } else if (el.getAttributeValue("value").indexOf(":") != -1) {
                        path += ("/@" + el.getAttributeValue("value").substring(el.getAttributeValue("value").indexOf(":") + 1));
                    } else {
                        path += ("/" + el.getAttributeValue("value"));
                    }

                    try {
                        XPath xpath = XPath.newInstance(path);
                        List list = xpath.selectNodes(metadata);

                        // element loop: children of given xpath in metadata
                        if (list.size() > 0) {
                            for (int k = 0; k < list.size(); k++) {
                                Object tmpobj = list.get(k);

                                if (tmpobj.getClass().getName().equals("org.jdom.Text")) {
                                    Text t = (Text) list.get(k);
                                    strValue += t.getText();
                                } else {
                                    Attribute a = (Attribute) list.get(k);
                                    strValue += a.getValue();
                                }

                                if (k < (list.size() - 1)) {
                                    strValue += "|";
                                }
                            }
                        }
                    } catch (Exception e) {
                        // strValue="";
                    }
                }
            }

            values.add(strValue);
        }

        // save values in db
        MCRQueryIndexer.getInstance().insertInQuery(obj.getId().getId(), values);
    }

    public MCRResults runQuery(Document doc) {
        XMLOutputter out = new XMLOutputter();

        return runQuery(out.outputString(doc));
    }

    public MCRResults runQuery(String xml) {
        MCRResults res = MCRQuerySearcher.getInstance().runQuery(xml);

        return res;
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

    public Document getSearchDefinition() {
        return doc;
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
     * temporary Method
     * 
     */
    public String getQuery() {
        String ret = "";
        SAXBuilder builder = new SAXBuilder();
        InputStream in = this.getClass().getResourceAsStream("/query1.xml");

        try {
            XMLOutputter out = new XMLOutputter();
            Document doc = builder.build(in);
            in.close();
            ret = out.outputString(doc);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return ret;
    }

    public Element getQueryClause() {
        Element ret = null;
        SAXBuilder builder = new SAXBuilder();
        InputStream in = this.getClass().getResourceAsStream("/query1.xml");

        try {
            Document doc1 = builder.build(in);
            in.close();
            ret = ((Element) doc1.getRootElement().getChild("conditions").getChildren().get(0));
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

    public MCRResults search(MCRCondition condition, int maxResults) {
        return null;
    }
}
