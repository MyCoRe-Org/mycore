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

package org.mycore.urn.hibernate;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.urn.services.MCRURNStore;

/**
 * This class implements the MCRXMLInterface.
 */
@Deprecated
public class MCRHIBURNStore implements MCRURNStore {
    // logger
    static Logger logger = LogManager.getLogger(MCRHIBURNStore.class.getName());

    private String classname = MCRURN.class.getName();

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
    public synchronized final void create(String urn, String id, String path, String filename)
        throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            throw new MCRPersistenceException("The URN is null.");
        }
        if (id == null || id.length() == 0) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
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
     * The method removes an entry for the given URN from the datastore.
     *
     * @param urn
     *            a URN
     */
    public synchronized final void delete(String urn) {
        if (urn == null || urn.length() == 0) {
            logger.warn("Cannot delete for urn " + urn);
            return;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRURN> query = cb.createQuery(MCRURN.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        try {
            MCRURN entry = em
                .createQuery(
                    query.where(
                        cb.equal(root.get(MCRURN_.key).get(MCRURNPK_.mcrurn), urn)))
                .getSingleResult();
            em.remove(entry);
        } catch (NoResultException e) {
            logger.warn("URN " + urn + " is unknown and cannot be deleted");
        }
    }

    /**
     * The method remove a item for the URN from the datastore.
     *
     * @param objID
     *            an object id
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public synchronized final void deleteByObjectID(String objID) {
        if (objID == null || objID.length() == 0) {
            logger.warn("Do not provide a null value as object id");
            return;
        }

        Query<?> q = getSession().createQuery("delete from " + classname + " where MCRID = :theObjectId");
        q.setParameter("theObjectId", objID);
        int rowCount = q.executeUpdate();
        logger.info(rowCount + " entries were deleted for object " + objID + " from " + MCRURN.class.getSimpleName());
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
    public final String getURNforDocument(String id) throws MCRPersistenceException {
        if (id == null || id.length() == 0) {
            return null;
        }

        Session session = getSession();
        String querySB = "select key.mcrurn from " + classname + " where key.mcrid='" + id + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString(), String.class).getResultList();
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
    public final String getDocumentIDforURN(String urn) throws MCRPersistenceException {
        if (urn == null || urn.length() == 0) {
            return null;
        }

        Session session = getSession();
        String querySB = "select key.mcrid from " + classname + " where key.mcrurn='" + urn + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString(), String.class).getResultList();
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
    public final boolean hasURNAssigned(String id) throws MCRPersistenceException {
        if (id == null || id.length() == 0) {
            return false;
        }

        Session session = getSession();
        String querySB = "select key.mcrurn from " + classname + " where key.mcrid='" + id + "'";
        logger.debug("HQL-Statement: " + querySB.toString());
        List<String> returns = session.createQuery(querySB.toString(), String.class).getResultList();
        return !(returns == null || returns.isEmpty());
    }

    /**
     * This method check that the URN exist in this store.
     *
     * @return true if the URN exist, else return false
     */
    public final boolean exist(String urn) {
        boolean exists = false;
        if (urn == null || urn.length() == 0) {
            return exists;
        }

        Session session = getSession();
        String query = "select key.mcrid from " + classname + " where key.mcrurn = '" + urn + "'";
        List<String> l = session.createQuery(query.toString(), String.class).getResultList();
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

    public String getURNForFile(String derivateId, String path, String fileName) {
        if (derivateId == null || fileName == null) {
            return null;
        }
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        List<String> returns = em.createQuery(
            query
                .select(root.get(MCRURN_.key).get(MCRURNPK_.mcrurn))
                .where(
                    cb.equal(root.get(MCRURN_.key).get(MCRURNPK_.mcrid), derivateId),
                    cb.equal(root.get(MCRURN_.path), path),
                    cb.equal(root.get(MCRURN_.filename), fileName)))
            .getResultList();
        if (returns.isEmpty()) {
            return null;
        }
        if (returns.size() != 1) {
            logger.warn(
                "There are more than just one urn for file \"" + fileName + "\" in derivate \"" + derivateId + "\"");
        }
        String urn = returns.get(0);

        return urn;
    }

    /**
     * @return the count of urn matching the given 'registered' attribute
     */
    public long getCount(boolean registered) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Number> query = cb.createQuery(Number.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        try {
            return em.createQuery(
                query
                    .select(cb.count(root))
                    .where(cb.equal(root.get(MCRURN_.registered), registered)))
                .getSingleResult()
                .longValue();
        } catch (PersistenceException e) {
            logger.error("Could not execute query", e);
        }
        return 0;
    }

    public List<MCRURN> get(boolean registered, int start, int rows) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRURN> query = cb.createQuery(MCRURN.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        try {
            return em
                .createQuery(
                    query.where(
                        cb.equal(root.get(MCRURN_.registered), registered))
                        .orderBy(cb.asc(root.get(MCRURN_.key))))
                .setFirstResult(start)
                .setMaxResults(rows)
                .getResultList();
        } catch (Exception ex) {
            logger.error("Could not execute query", ex);
        }
        // return an empty list
        return new ArrayList<MCRURN>();
    }

    public void update(MCRURN urn) {
        Session session = MCRHIBConnection.instance().getSession();
        session.saveOrUpdate(urn);
    }

    /**
     * Get all URN for the given object id.
     */
    public List<MCRURN> get(MCRObjectID id) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRURN> query = cb.createQuery(MCRURN.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        return em
            .createQuery(
                query.where(
                    cb.equal(root.get(MCRURN_.key).get(MCRURNPK_.mcrid), id.toString()))
                    .orderBy(cb.asc(root.get(MCRURN_.key))))
            .getResultList();
    }

    /**
     * @return a {@link List} of {@link MCRURN} where path and file name are just blanks or null;
     */
    public List<MCRURN> getBaseURN(boolean registered, boolean dfg, int start, int rows) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRURN> query = cb.createQuery(MCRURN.class);
        Root<MCRURN> root = query.from(MCRURN.class);
        try {
            return em
                .createQuery(
                    query.where(
                        cb.isNull(root.get(MCRURN_.path)),
                        cb.isNull(root.get(MCRURN_.filename)),
                        cb.equal(root.get(MCRURN_.registered), registered),
                        cb.equal(root.get(MCRURN_.dfg), dfg))
                        .orderBy(cb.asc(root.get(MCRURN_.key))))
                .setFirstResult(start)
                .setMaxResults(rows)
                .getResultList();
        } catch (Exception ex) {
            logger.error("Could not execute query", ex);
        }
        // return an empty list
        return new ArrayList<MCRURN>();
    }
}
