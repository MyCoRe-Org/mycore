package org.mycore.datamodel.classifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.frontend.cli.MCRObjectCommands;

/**
 * @author Radi Radichev
 * 
 */

public class MCRClassificationPool {

    /**
     * stores all edited classifications
     */
    private HashMap<MCRCategoryID, MCRCategory> classifications = new HashMap<MCRCategoryID, MCRCategory>();

    private HashSet<MCRCategoryID> movedCategories = new HashSet<MCRCategoryID>();

    static Logger LOGGER = Logger.getLogger(MCRClassificationPool.class);

    private static MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    public MCRClassificationPool() {
        //store reference of classifications in current session
        MCRSessionMgr.getCurrentSession().put("classifications", classifications); // Put
    }

    /**
     * This method returns all ClassificationIDs. From the database and pool
     * together.
     * 
     * @return
     */
    public Set<MCRCategoryID> getAllIDs() {
        Set<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        ids.addAll(DAO.getRootCategoryIDs());
        ids.addAll(classifications.keySet());
        return ids;
    }

    /**
     * Save all changes to the database.
     * 
     * @return
     */
    public boolean saveAll() {
        synchronized (classifications) {
            HashMap<MCRCategoryID, MCRCategory> classCopy = new HashMap<MCRCategoryID, MCRCategory>();
            classCopy.putAll(classifications);
            Iterator<MCRCategory> rootCategories = classifications.values().iterator();
            try {
                while (rootCategories.hasNext()) {
                    MCRCategory clas = rootCategories.next();
                    LOGGER.debug("Classification to be saved: " + clas.getId());
                    if (DAO.exist(clas.getId())) {
                        DAO.replaceCategory(clas);
                    } else {
                        DAO.addCategory(null, clas);
                    }
                    rootCategories.remove();
                }
            } catch (Exception e) {
                LOGGER.warn("Error while saving all classifications.", e);
                return false;
            }
            LOGGER.debug("Getting all categories that where moved to left or right");
            HashSet<MCRCategoryID> modifiedCategories = new HashSet<MCRCategoryID>();
            for (MCRCategoryID categID : getMovedCategories()) {
                LOGGER.debug("Getting sub categories of "+categID);
                MCRCategory cat = findCategory(classCopy.get(MCRCategoryID.rootID(categID.getRootID())), categID);
                modifiedCategories.addAll(getSubTree(cat));
            }
            LOGGER.debug("Getting all objects that where affected my category movements");
            HashSet<String> linkedObjects = new HashSet<String>();
            MCRCategLinkService linkService = MCRCategLinkServiceFactory.getInstance();
            for (MCRCategoryID cat:modifiedCategories){
                LOGGER.debug("Getting linked objects for "+cat);
                linkedObjects.addAll(linkService.getLinksFromCategory(cat));
            }
            for (String objectID:linkedObjects){
                MCRObjectCommands.repairMetadataSearchForID(objectID);
            }
            movedCategories.clear();
        }
        return true;
    }

    private static Collection<MCRCategoryID> getSubTree(MCRCategory subTreeNode) {
        ArrayList<MCRCategoryID> children = new ArrayList<MCRCategoryID>();
        children.add(subTreeNode.getId());
        for (MCRCategory child : subTreeNode.getChildren()) {
            children.addAll(getSubTree(child));
        }
        return children;
    }

    private static MCRCategory findCategory(MCRCategory parent, MCRCategoryID id) {
        MCRCategory found = null;
        for (MCRCategory cat : parent.getChildren()) {
            if (cat.getId().equals(id)) {
                found = cat;
                LOGGER.debug("Found Category: " + found.getId().getID());
                break;
            }
            MCRCategory rFound = findCategory(cat, id);
            if (rFound != null) {
                found = rFound;
                break;
            }
        }

        return found;
    }

    /**
     * Cancel all changes. Clear the hashtable.
     * 
     * @return
     */
    public boolean purgeAll() {

        LOGGER.debug("Purging all in progress...");
        synchronized (classifications) {
            classifications.clear();
            movedCategories.clear();
        }
        return true;
    }

    /**
     * Check if the Classification with classID is already edited and in the
     * session.
     * 
     * @param classID
     *            Classification ID to check for
     * @return <code>true</code> when Classification is edited,
     *         <code>false</code> when not.
     */
    public boolean isEdited(MCRCategoryID classID) {
        return classifications.containsKey(classID);
    }

    /**
     * Put the classification in the Session. This method is executed every time
     * a Classification or a Category is edited
     * 
     * @param cl
     *            Classification to be stored in the session.
     */
    public void updateClassification(MCRCategory cl) {
        classifications.put(cl.getId(), cl);
        LOGGER.info("Classification: " + cl.getId() + " added to session!");
    }

    /**
     * Delete a classfication from the pool
     * @param cl
     */
    public void deleteClassification(MCRCategoryID cl) {
        if (classifications.containsKey(cl))
            classifications.remove(cl);
    }

    /**
     * This method checks to see if the classification which is expected is in
     * the Session (when edited) or it takes the classification from the
     * database
     * 
     * @param clid
     * @param writeAccess
     * @return MCRCategory classif
     */

    public MCRCategory getClassificationAsPojo(MCRCategoryID clid, boolean writeAccess) {
        MCRCategory classif = classifications.get(clid);
        if (classif != null) {
            return classif;
        } else {
            MCRCategory cl = DAO.getCategory(clid, writeAccess ? -1 : 0);
            return cl;
        }
    }

    public HashSet<MCRCategoryID> getMovedCategories() {
        return movedCategories;
    }

}
