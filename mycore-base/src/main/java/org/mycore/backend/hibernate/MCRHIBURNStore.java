/*
 * 
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

package org.mycore.backend.hibernate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.tables.MCRURN;
import org.mycore.common.MCRPersistenceException;
import org.mycore.services.urn.MCRURNStore;

/**
 * This class implements the MCRXMLInterface.
 */
public class MCRHIBURNStore implements MCRURNStore {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBURNStore.class.getName());

    private String classname = "org.mycore.backend.hibernate.tables.MCRURN";

    /**
     * The constructor for the class MCRHIBURLStore.
     */
    public MCRHIBURNStore() {
    }

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param urn
     *            a URN
     * @param id
     *            the MCRObjectID as String
     * @exception MCRPersistenceException
     *                the method arguments are not correct
     */
    public synchronized final void create(String urn, String id) throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            throw new MCRPersistenceException("The URN is null.");
        }
        if (id == null || id.length() == 0) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        Session session = getSession();
        MCRURN tab = new MCRURN(id, urn);
        logger.debug("Inserting " + id + "/" + urn + " into database");
        session.saveOrUpdate(tab);
        session.flush();
    }

    /**
     * The method creates a new item in the datastore.
     * 
     * @param urn
     *            a URN
     * @param id
     *            the MCRObjectID as String
     * @param path
     *            the path of the derivate in the IFS
     * @exception MCRPersistenceException
     *                the method arguments are not correct
     */
    public synchronized final void create(String urn, String id, String path, String filename) throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            throw new MCRPersistenceException("The URN is null.");
        }
        if (id == null || id.length() == 0) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }
        if (path == null || path.length() == 0) {
            throw new MCRPersistenceException("The Path is null.");
        }

        Session session = getSession();
        MCRURN tab = new MCRURN(id, urn, path, filename);
        logger.debug("Inserting " + id + "/" + urn + "(" + path + filename + ") into database");
        session.saveOrUpdate(tab);
    }

    /**
     * Assigns the given urn to the given document ID
     * 
     * @param urn
     *            a URN
     * @param id
     *            the MCRObjectID as String
     * @exception MCRPersistenceException
     *                the method arguments are not correct
     */
    public void assignURN(String urn, String id) {
        create(urn, id);
    }

    /**
     * Assigns the given urn to the given derivate ID
     * 
     * @param urn
     *            a URN
     * @param id
     *            the MCRObjectID as String
     * @param path
     *            the path of the derivate
     * @exception MCRPersistenceException
     *                the method arguments are not correct
     */

    public void assignURN(String urn, String id, String path, String filename) {
        create(urn, id, path, filename);
    }

    /**
     * The method remove a item for the URN from the datastore.
     * 
     * @param urn
     *            a URN
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public synchronized final void delete(String urn) throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            throw new MCRPersistenceException("The URN is null.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(classname).append(" where MCRURN = '").append(urn).append("'");

        Session session = getSession();
        int deleted = session.createQuery(sb.toString()).executeUpdate();
        logger.debug(deleted + " references deleted.");
    }

    /**
     * The method remove a item for the URN from the datastore.
     * 
     * @param objID
     *            an object id 
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public synchronized final void deleteByObjectID(String objID) throws MCRPersistenceException {
        if (objID == null || objID.length() == 0) {
            throw new MCRPersistenceException("The object id is null.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(classname).append(" where MCRID = '").append(objID).append("'");

        Session session = getSession();
        int deleted = session.createQuery(sb.toString()).executeUpdate();
        logger.debug(deleted + " references deleted.");
    }

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     * 
     * @param urn
     *            a URN
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public void removeURN(String urn) {
        delete(urn);
    }

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @param id
     *            the MCRObjectID as String
     * @return the urn, or null if no urn is assigned to this ID
     */
    @SuppressWarnings("unchecked")
    public final String getURNforDocument(String id) throws MCRPersistenceException {
        if (id == null || id.length() == 0) {
            return null;
        }

        Session session = getSession();
        String querySB = "select key.mcrurn from " + classname + " where key.mcrid='" + id + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString()).list();
        if (returns.size() != 1) {
            return null;
        }
        return returns.get(0);
    }

    /**
     * Removes the urn (and assigned document ID) from the persistent store by the given 
     * object id
     * 
     * @param objID
     *            object id
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public void removeURNByObjectID(String objID) {
        deleteByObjectID(objID);
    }

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @param urn
     *            the URN as String
     * @return the document ID, or null if no urn is assigned to this ID
     */
    @SuppressWarnings("unchecked")
    public final String getDocumentIDforURN(String urn) throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            return null;
        }

        Session session = getSession();
        String querySB = "select key.mcrid from " + classname + " where key.mcrurn='" + urn + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString()).list();
        if (returns.size() != 1) {
            return null;
        }
        return returns.get(0);
    }

    /**
     * Checks wether an object has an urn assigned or not
     * 
     * @param id
     *            the MCRObjectID as String
     * @return true if an urn is assigned to the given object, false otherwise
     */
    @SuppressWarnings("unchecked")
    public final boolean hasURNAssigned(String id) throws MCRPersistenceException {
        if (id == null || id.length() == 0) {
            return false;
        }

        Session session = getSession();
        String querySB = "select key.mcrurn from " + classname + " where key.mcrid='" + id + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString()).list();
        return !(returns == null || returns.isEmpty());
    }

    /**
     * This method check that the URN exist in this store.
     * 
     * @return true if the URN exist, else return false
     */
    @SuppressWarnings("unchecked")
    public final boolean exist(String urn) {
        boolean exists = false;
        if (urn == null || urn.length() == 0) {
            return exists;
        }

        Session session = getSession();
        String query = "select key.mcrid from " + classname + " where key.mcrurn = '" + urn + "'";
        List<String> l = session.createQuery(query.toString()).list();
        if (!l.isEmpty()) {
            exists = true;
        }
        return exists;
    }

    /**
     * Returns true if the given urn is assigned to a document ID
     * 
     * @param urn
     *            a URN
     * @return true if the URN exist, else return false
     */
    public boolean isAssigned(String urn) {
        return exist(urn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.urn.MCRURNStore#getURNForFile(java.lang.String,
     * java.lang.String)
     */
    public String getURNForFile(String derivateId, String fileName) {
        if (derivateId == null || fileName == null) {
            return null;
        }

        Session session = getSession();
        StringBuilder querySB = new StringBuilder();
        querySB.append("select key.mcrurn from ");
        querySB.append(classname).append(" where key.mcrid='");
        querySB.append(derivateId).append("'");
        querySB.append(" and filename='").append(fileName).append("'");
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString()).list();
        if (returns.isEmpty()) {
            return null;
        }
        if (returns.size() != 1) {
            logger.warn("There are more than just one urn for file \"" + fileName + "\" in derivate \"" + derivateId + "\"");
        }
        String urn = returns.get(0);

        return urn;
    }

    /**
     * @param derivateId 
     * @param path
     * @param fileName
     * @return
     */
    public String getURNForFile(String derivateId, String path, String fileName) {
        if (derivateId == null || fileName == null) {
            return null;
        }

        Session session = getSession();
        Criteria criteria = session.createCriteria(MCRURN.class);
        criteria.setProjection(Projections.property("key.mcrurn"));
        Map<String, String> propertyNameValues = new HashMap<String, String>();
        propertyNameValues.put("key.mcrid", derivateId);
        propertyNameValues.put("path", path);
        propertyNameValues.put("filename", fileName);
        criteria.add(Restrictions.allEq(propertyNameValues));
        if (logger.isDebugEnabled()) {
            logger.debug("HQL-Statement: " + criteria.toString());
        }
        @SuppressWarnings("unchecked")
        List<String> returns = criteria.list();
        if (returns.isEmpty()) {
            return null;
        }
        if (returns.size() != 1) {
            logger.warn("There are more than just one urn for file \"" + fileName + "\" in derivate \"" + derivateId + "\"");
        }
        String urn = returns.get(0);

        return urn;
    }
}
