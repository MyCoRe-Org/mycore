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

import java.util.LinkedList;
import java.util.List;

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
     *            a string with the link ID TO
     */
    public final void create(String from, String to) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }

        Session session = getSession();
        Transaction tx = session.beginTransaction();

        try {
            if (mtype.equals("class")) {
                MCRLINKCLASS l = new MCRLINKCLASS(from, to);
                session.saveOrUpdate(l);
            } else {
                MCRLINKHREF l = new MCRLINKHREF(from, to);
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
     * The method remove a item for the from ID from the datastore.
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
}
