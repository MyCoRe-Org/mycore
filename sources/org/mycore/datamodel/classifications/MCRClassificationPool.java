package org.mycore.datamodel.classifications;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;

import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;

/**
 * @author Radi Radichev
 * 
 */

public class MCRClassificationPool {

    private HashMap<MCRCategoryID, MCRCategory> classifications = new HashMap<MCRCategoryID, MCRCategory>(); // A

    // Hash
    // map
    // to
    // store
    // all
    // edited
    // classifications

    static Logger LOGGER = Logger.getLogger(MCRClassificationPool.class);

    private static MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    public MCRClassificationPool() {
        MCRSessionMgr.getCurrentSession().put("classifications", classifications); // Put
        // the
        // hash
        // map
        // in
        // the
        // current
        // session
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

        for (MCRCategory clas : classifications.values()) {
            try {
                LOGGER.debug("Classification to be saved: " + clas.getId());
                if (DAO.exist(clas.getId())) {
                    DAO.replaceCategory(clas);
                } else {
                    DAO.addCategory(null, clas);
                }
                classifications.remove(clas.getId());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
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
        if(classifications.containsKey(cl))
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
            MCRCategory cl = DAO.getCategory(clid, writeAccess ? -1 : 1);
            return cl;
        }
    }

}
