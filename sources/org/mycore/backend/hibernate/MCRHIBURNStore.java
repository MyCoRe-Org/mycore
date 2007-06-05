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

package org.mycore.backend.hibernate;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.tables.MCRURN;
import org.mycore.common.MCRException;
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
        if (urn == null || (urn.length() == 0)) {
            throw new MCRPersistenceException("The URN is null.");
        }
        if (id == null || (id.length() == 0)) {
            throw new MCRPersistenceException("The MCRObjectID is null.");
        }

        Session session = getSession();
        MCRURN tab = new MCRURN(id, urn);
        logger.debug("Inserting " + id + "/" + urn + " into database");
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
     * The method remove a item for the URN from the datastore.
     * 
     * @param urn
     *            a URN
     * @exception MCRPersistenceException
     *                the method argument is not correct
     */
    public synchronized final void delete(String urn) throws MCRPersistenceException {
        if (urn == null || (urn.length() == 0)) {
            throw new MCRPersistenceException("The URN is null.");
        }

        StringBuffer sb = new StringBuffer();
        sb.append("delete from ").append(classname).append(" where MCRURN = '").append(urn).append("'");

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
    public final String getURNforDocument(String id) throws MCRPersistenceException {
        if (id == null || (id.length() == 0)) {
            return null;
        }

        Session session = getSession();
        String ret = null;
        StringBuffer querySB = new StringBuffer("select key.mcrid from ").append(classname).append(" where MCRID='").append(id).append("'");
        logger.debug("HQL-Statement: " + querySB.toString());
        List returns;
        returns = session.createQuery(querySB.toString()).list();
        if (returns.size() != 1)
            return ret;
        ret = (String) returns.get(0);
        return ret;
    }

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @param urn
     *            the URN as String
     * @return the document ID, or null if no urn is assigned to this ID
     */
    public final String getDocumentIDforURN(String urn) throws MCRPersistenceException {
        if (urn == null || (urn.length() == 0)) {
            return null;
        }

        Session session = getSession();
        String ret = null;
        StringBuffer querySB = new StringBuffer("select key.mcrurn from ").append(classname).append(" where MCRURN='").append(urn).append("'");
        logger.debug("HQL-Statement: " + querySB.toString());
        List returns;
        returns = session.createQuery(querySB.toString()).list();
        if (returns.size() != 1)
            return ret;
        ret = (String) returns.get(0);
        return ret;
    }

    /**
     * This method check that the URN exist in this store.
     * 
     * @return true if the URN exist, else return false
     */
    public final boolean exist(String urn) {
        boolean exists = false;
        if (urn == null || (urn.length() == 0)) {
            return exists;
        }

        Session session = getSession();
        StringBuffer query = new StringBuffer("select key.mcrid from ").append(classname).append(" where key.mcrurn = '").append(urn).append("'");
        List l = session.createQuery(query.toString()).list();
        if (l.size() > 0) {
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

}
