/**
 * $RCSfile: MCRClassificationMigrationHelper.java,v $ $Revision: 1.0 $ $Date: 07.07.2008 09:11:45 $ This file is part of ** M y C o R e ** Visit our homepage
 * at http://www.mycore.de/ for details. This program is free software; you can use it, redistribute it and / or modify it under the terms of the GNU General
 * Public License (GPL) as published by the Free Software Foundation; either version 2 of the License or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program,
 * normally in the file license.txt. If not, write to the Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 **/
package org.mycore.services.migration;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRObjectReference;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRClassificationMigrationHelper {
    static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static final MCRXMLTableManager XML_TABLE = MCRXMLTableManager.instance();

    private static final MCRCategLinkService CATAG_LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    private static Logger LOGGER = Logger.getLogger(MCRClassificationMigrationHelper.class);

    static void createCategories() {
        List<String> classIds = MCRXMLTableManager.instance().retrieveAllIDs("class");
        LOGGER.info("Migrating classifications and categories...");
        for (String classID : classIds) {
            MCRObjectID objID = new MCRObjectID(classID);
            Document cl = MCRXMLTableManager.instance().readDocument(objID);
            MCRCategory cat = MCRXMLTransformer.getCategory(cl);
            DAO.addCategory(null, cat);
            MCRXMLTableManager.instance().delete(objID);
        }
    }

    @SuppressWarnings("unchecked")
    static void migrateCategoryLinks() throws JDOMException {
        LOGGER.info("Migrating object links....");
        XPath classSelector = XPath.newInstance("/mycoreobject/metadata/*[@class='MCRMetaClassification']/*");
        LOGGER.debug("Retrieving all relevant objects IDs.");
        List<String> objIDs = getAllObjectIDsForCategories();
        int objectsTotal = objIDs.size();
        LOGGER.debug(objectsTotal + "object IDs retrieved");

        // pass through found objects
        int pos = 0;
        long startTime = System.currentTimeMillis();
        Session session = MCRHIBConnection.instance().getSession();
        for (String id : objIDs) {
            pos++;
            LOGGER.debug("Processing object " + id);
            Collection<MCRCategoryID> categories = new HashSet<MCRCategoryID>();
            if (XML_TABLE.exist(new MCRObjectID(id))) {
                LOGGER.debug("Parsing XML of object " + id);
                Document obj = XML_TABLE.readDocument(new MCRObjectID(id));
                LOGGER.debug("executing XPATH " + classSelector.getXPath());
                List<Element> classElements = classSelector.selectNodes(obj);
                int categoryCount = classElements.size();
                LOGGER.debug("found " + categoryCount + " category ids");
                for (Element el : classElements) {
                    String clid = el.getAttributeValue("classid");
                    String catid = el.getAttributeValue("categid");
                    categories.add(new MCRCategoryID(clid, catid));
                }
                if (categories.size() > 0) {
                    LOGGER.debug("updating references");
                    String type = id.substring(id.indexOf("_") + 1, id.lastIndexOf("_") - 1);
                    MCRObjectReference objectReference = new MCRObjectReference(id, type);
                    try {
                        CATAG_LINK_SERVICE.setLinks(objectReference, categories);
                    } catch (Exception e) {
                        LOGGER.error("Error occured while creating category links for object " + id, e);
                    }
                }
                if (pos % 100 == 0 || pos == objectsTotal) {
                    long currentTime = System.currentTimeMillis();
                    long finishTime = currentTime + ((currentTime - startTime) * (objectsTotal - pos) / pos);
                    LOGGER.info((100d * (double) pos / (double) objectsTotal) + " % (" + pos + "/" + objectsTotal + "), estimated finish time is "
                            + new Date(finishTime));
                    //flush a batch of inserts and release memory:
                    session.flush();
                    session.clear();
                }
            } else
                LOGGER.warn("Object " + id + " linked to category, but it is not in database.");
        }
    }

    static void deleteOldCategoryLinks() {
        LOGGER.info("Deleting old object links...");
        final Session session = MCRHIBConnection.instance().getSession();
        int deleted = session.createQuery("DELETE FROM MCRLINKHREF WHERE key.mcrtype='classid'").executeUpdate();
        LOGGER.info(deleted + " object links deleted.");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getAllObjectIDsForCategories() {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(MCRLINKHREF.class);
        c.setProjection(Projections.distinct(Projections.property("key.mcrfrom")));
        c.add(Restrictions.eq("key.mcrtype", "classid"));
        return c.list();
    }

}
