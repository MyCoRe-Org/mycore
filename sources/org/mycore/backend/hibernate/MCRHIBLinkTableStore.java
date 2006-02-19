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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.mycore.backend.hibernate.tables.MCRLINKCLASS;
import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRLinkTableInterface;
import org.mycore.datamodel.metadata.MCRLinkTableManager;

/**
 * This class implements the MCRLinkTableInterface.
 * 
 */
public class MCRHIBLinkTableStore implements MCRLinkTableInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBLinkTableStore.class.getName());

    // internal data
    private String mtype;

    private String classname;

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The constructor for the class MCRHIBLinkTableStore.
     */
    public MCRHIBLinkTableStore() {
    }

    /**
     * The initializer for the class MCRHIBLinkTableStore.
     * 
     * @exception MCRPersistenceException
     *                if the type is not correct
     */
    public final void init(String type) throws MCRPersistenceException {
        if (type == null) {
            throw new MCRPersistenceException("The type of the constructor is null");
        }

        type = type.trim();

        if ((MCRLinkTableManager.LINK_TABLE_TYPES.length != 2) || !"class".equals(MCRLinkTableManager.LINK_TABLE_TYPES[0]) || !"href".equals(MCRLinkTableManager.LINK_TABLE_TYPES[1])) {
            throw new IllegalStateException("if you change MCRLinkTableManager, you have to change MCRHIBLinkTableStore too");
        }

        if ("class".equals(type)) {
            this.classname = "org.mycore.backend.hibernate.tables.MCRLINKCLASS";
        } else if ("href".equals(type)) {
            this.classname = "org.mycore.backend.hibernate.tables.MCRLINKHREF";
        } else {
            throw new MCRPersistenceException("The type of the constructor doesn't match 'class' or 'href'.");
        }

        mtype = type;
    }

    /**
     * The method drop the table.
     */
    public final void dropTables() {
        /* not supported for hibernate */
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void create(String from, String to, String type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            if (mtype.equals("class")) {
                MCRLINKCLASS l = new MCRLINKCLASS(from, to);
                session.saveOrUpdate(l);
            } else {
                MCRLINKHREF l = new MCRLINKHREF(from, to, type);
                session.saveOrUpdate(l);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method create a new item in the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void create(String from, String[] to, String[] type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if (to == null) {
            throw new MCRPersistenceException("The to value is null.");
        }

        if (type == null) {
            throw new MCRPersistenceException("The type value is null.");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            for (int i = 0; i < to.length; i++) {
                if (mtype.equals("class")) {
                    MCRLINKCLASS l = new MCRLINKCLASS(from, to[i]);
                    session.saveOrUpdate(l);
                } else {
                    MCRLINKHREF l = new MCRLINKHREF(from, to[i], type[i]);
                    session.saveOrUpdate(l);
                }
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method removes a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     */
    public final void delete(String from) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            List l = session.createQuery("from " + classname + " where MCRFROM = '" + from + "'").list();

            for (int t = 0; t < l.size(); t++) {
                session.delete(l.get(t));
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method removes a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void delete(String from, String[] to, String[] type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if (to == null) {
            throw new MCRPersistenceException("The to value is null.");
        }

        if (type == null) {
            throw new MCRPersistenceException("The type value is null.");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            for (int i = 0; i < to.length; i++) {
                if (mtype.equals("class")) {
                    MCRLINKCLASS l = new MCRLINKCLASS(from, to[i]);
                    session.delete(l);
                } else {
                    MCRLINKHREF l = new MCRLINKHREF(from, to[i], type[i]);
                    session.delete(l);
                }
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method removes a item for the from ID from the datastore.
     * 
     * @param from
     *            a string with the link ID MCRFROM
     * @param to
     *            a string with the link ID MCRTO
     * @param type
     *            a string with the link ID MCRTYPE
     */
    public final void delete(String from, String to, String type) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            if (mtype.equals("class")) {
                MCRLINKCLASS l = new MCRLINKCLASS(from, to);
                session.delete(l);
            } else {
                MCRLINKHREF l = new MCRLINKHREF(from, to, type);
                session.delete(l);
            }

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error(e);
        } finally {
            session.close();
        }
    }

    /**
     * The method count the number of references to the 'to' value of the table.
     * 
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public final int countTo(String to) {
        Session session = getSession();
        List l = new LinkedList();

        try {
            l = session.createQuery("from " + classname + " where MCRTO = '" + to + "'").list();
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("Error during countTo(" + to + ")", e);
        } finally {
            session.close();
        }

        return l.size();
    }

    /**
     * The method count the number of references to the 'to' and the 'type'
     * value of the table.
     * 
     * @param to
     *            the object ID as String, they was referenced
     * @param type
     *            the refernce type
     * @return the number of references
     */
    public final int countTo(String to, String type) {
        Session session = getSession();
        List l = new LinkedList();

        try {
            l = session.createQuery("from " + classname + " where MCRTO = '" + to + "' and MCRTYPE = '" + type + "'").list();
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("Error during countTo(" + to + " and " + type + ")", e);
        } finally {
            session.close();
        }

        return l.size();
    }

    public final int countTo(String to, String docType, String restriction) {
        Session session = getSession();
        List l = new LinkedList();

        String query = "from " + classname + " where MCRTO like '" + to + "'";

        if (restriction != null) {
            query += (" and MCRTO like '" + restriction + "%'");
        }

        if (docType != null) {
            query += (" and MCRFROM like '%_" + docType + "_%'");
        }

        try {
            l = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error(e);
            throw new MCRException("Error during countTo(" + to + "," + docType + "," + restriction + ")", e);
        } finally {
            session.close();
        }

        return l.size();
    }

    /**
     * The method returns a Map of all counted distinct references
     * 
     * @param mcrtoPrefix
     * @return
     * 
     * the result-map of (key,value)-pairs can be visualized as<br />
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref where mcrto like mcrtoPrefix + '%' group by
     * mcrto;
     * 
     */
    public Map getCountedMapOfMCRTO(String mcrtoPrefix) {
        Map map = new HashMap();
        Session session = getSession();
        String query = "select count(key.mcrfrom), key.mcrto from " + classname + " where MCRTO like '" + mcrtoPrefix + "%' group by key.mcrto";
        logger.debug("HQL-Statement: " + query);
        try {
            Iterator results = session.createQuery(query).list().iterator();
            while (results.hasNext()) {
                Object[] row = (Object[]) results.next();
                map.put(row[1], row[0]);
            }
        } catch (Exception e) {
            logger.error("catched error@getCountedMapOfMCRTO:", e);
            throw new MCRException("Error during getCountedMapOfMCRTO", e);
        } finally {
            session.close();
        }
        return map;
    }

    public List getSourcesOf(String destination) {
        Session session = getSession();
        String query = "select key.mcrfrom from " + classname + " where MCRTO='" + destination + "'";
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getSourceOf:", e);
            throw new MCRException("Error during getSourceOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

    /**
     * Returns a List of all link sources of <code>destination</code>
     * 
     * @param destinations
     *            Destination-ID
     * @return List of Strings (Source-IDs)
     */
    public List getSourcesOf(String[] destinations) {
        Session session = getSession();
        String query = "select key.mcrfrom from " + classname + " where MCRTO IN " + getSQLArray(destinations);
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getSourceOf:", e);
            throw new MCRException("Error during getSourceOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

    /**
     * Returns a List of all link sources of <code>destination</code>
     * 
     * @param destinations
     *            Destination-ID
     * @return List of Strings (Source-IDs)
     */
    public List getDestinationsOf(String source) {
        Session session = getSession();
        String query = "select key.mcrto from " + classname + " where MCRFROM='" + source + "'";
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getDestinationOf:", e);
            throw new MCRException("Error during getDestinationOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

    /**
     * Returns a List of all link destination of <code>source</code>
     * 
     * @param sources
     *            Source-ID
     * @return List of Strings (Destination-IDs)
     */
    public List getDestinationsOf(String[] sources) {
        Session session = getSession();
        String query = "select key.mcrto from " + classname + " where MCRFROM IN " + getSQLArray(sources);
        logger.debug("HQL-Statement: " + query);
        List returns;
        try {
            returns = session.createQuery(query).list();
        } catch (Exception e) {
            logger.error("catched error@getSourceOf:", e);
            throw new MCRException("Error during getSourceOf", e);
        } finally {
            session.close();
        }
        return returns;
    }

    private static final String getSQLArray(String[] values) {
        StringBuffer returns = new StringBuffer();
        returns.append("( ");
        for (int i = 0; i < values.length; i++) {
            returns.append('\'').append(values[i]).append('\'');
            if (i < (values.length - 1)) {
                returns.append(", ");
            }
        }
        returns.append(" )");
        return returns.toString();
    }
}