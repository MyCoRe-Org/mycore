package org.mycore.datamodel.classifications;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.jdom.Document;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;
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

    private MCRCategoryDAO categoryDAO;

    private MCRCategLinkService linkService;

    public MCRClassificationPool() {
        this(MCRCategoryDAOFactory.getInstance(), MCRCategLinkServiceFactory.getInstance());
    }

    MCRClassificationPool(MCRCategoryDAO categoryDAO, MCRCategLinkService linkService) {
        //store reference of classifications in current session
        MCRSessionMgr.getCurrentSession().put("classifications", classifications); // Put
        this.categoryDAO = categoryDAO;
        this.linkService = linkService;
    }

    /**
     * This method returns all ClassificationIDs. From the database and pool
     * together.
     * 
     * @return
     */
    public Set<MCRCategoryID> getAllIDs() {
        Set<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        ids.addAll(categoryDAO.getRootCategoryIDs());
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
            HashSet<MCRCategoryID> modifiedCategories = getMovedLeftRightCategories();

            try {
                persistAllCategories();
            } catch (Exception e) {
                LOGGER.warn("Error while saving all classifications.", e);
                return false;
            }

            updateSearchIndexForMovedCategories(modifiedCategories);
        }
        return true;
    }

    private void updateSearchIndexForMovedCategories(HashSet<MCRCategoryID> modifiedCategories) {
        LOGGER.debug("Getting all objects that where affected my category movements");
        for (MCRCategoryID cat : modifiedCategories) {
            LOGGER.debug("Getting linked objects for " + cat);
            for (String objectID : linkService.getLinksFromCategory(cat)) {
                MCRObjectCommands.repairMetadataSearchForID(objectID);
            }
        }

        movedCategories.clear();
    }

    /**
     * Retrieve categories which are moved left or right
     * 
     * @return a map with category ids
     */
    private HashSet<MCRCategoryID> getMovedLeftRightCategories() {
        LOGGER.debug("Getting all categories that where moved to left or right");
        HashSet<MCRCategoryID> modifiedCategories = new HashSet<MCRCategoryID>();
        for (MCRCategoryID categID : getMovedCategories()) {
            LOGGER.info("Getting sub categories of " + categID);
            MCRCategory cat = MCRCategoryTools.findCategory(classifications.get(MCRCategoryID.rootID(categID.getRootID())), categID);
            modifiedCategories.addAll(getSubTree(cat));
        }
        return modifiedCategories;
    }

    /**
     * make all changes persistent
     * 
     */
    private void persistAllCategories() {
        for (Iterator rootCategories = classifications.values().iterator(); rootCategories.hasNext();) {
            MCRCategory classification = (MCRCategory) rootCategories.next();
            LOGGER.debug("Classification to be saved: " + classification.getId());

            if (categoryDAO.exist(classification.getId())) {
                categoryDAO.replaceCategory(classification);
            } else {
                categoryDAO.addCategory(null, classification);
            }

            rootCategories.remove();
        }
    }

    private static Collection<MCRCategoryID> getSubTree(MCRCategory subTreeNode) {
        ArrayList<MCRCategoryID> children = new ArrayList<MCRCategoryID>();
        children.add(subTreeNode.getId());
        for (MCRCategory child : subTreeNode.getChildren()) {
            children.addAll(getSubTree(child));
        }
        return children;
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
    public void deleteClassification(MCRCategoryID mcrClassificationID) {
        //only delete with DAO if Classification really exists.
        if (categoryDAO.exist(mcrClassificationID)) {
            categoryDAO.deleteCategory(mcrClassificationID);
        }

        classifications.remove(mcrClassificationID);
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
            MCRCategory cl = categoryDAO.getCategory(clid, writeAccess ? -1 : 0);
            //serialize and rebuild so that we do not work on database backed objects
            Document serialized = MCRCategoryTransformer.getMetaDataDocument(cl, false);
            try {
                cl = MCRXMLTransformer.getCategory(serialized);
            } catch (URISyntaxException e) {
                throw new MCRException(e);
            }
            return cl;
        }
    }

    public HashSet<MCRCategoryID> getMovedCategories() {
        return movedCategories;
    }

    public Map<MCRCategoryID, Boolean> hasLinks(MCRCategory category) {
        return linkService.hasLinks(category);
    }

}
