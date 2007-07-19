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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import org.mycore.backend.hibernate.tables.MCRLINKHREF;
import org.mycore.backend.hibernate.tables.MCRLINKHREFPK;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.common.MCRLinkTableInterface;

/**
 * This class implements the MCRLinkTableInterface.
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 */
public class MCRHIBLinkTableStore implements MCRLinkTableInterface {
    // logger
    static Logger logger = Logger.getLogger(MCRHIBLinkTableStore.class.getName());

    private String classname = "org.mycore.backend.hibernate.tables.MCRLINKHREF";

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }

    /**
     * The constructor for the class MCRHIBLinkTableStore.
     */
    public MCRHIBLinkTableStore() {
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
     * @param attr
     *            a string with the link ID MCRATTR
     */
    public final void create(String from, String to, String type, String attr) {
        if ((from == null) || ((from = from.trim()).length() == 0)) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }
        if ((to == null) || ((to = to.trim()).length() == 0)) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }
        if ((attr == null) || ((attr = attr.trim()).length() == 0)) {
            attr = "";
        }

        Session session = getSession();
        MCRLINKHREFPK pk = new MCRLINKHREFPK();
        pk.setMcrfrom(from);
        pk.setMcrto(to);
        pk.setMcrtype(type);
        MCRLINKHREF l = (MCRLINKHREF) session.get(MCRLINKHREF.class, pk);
        if (l == null) {
            l = new MCRLINKHREF();
            l.setKey(pk);
        }
        l.setMcrattr(attr);
        logger.debug("Inserting " + from + "/" + to + "/" + type + " into database MCRLINKHREF");
        session.saveOrUpdate(l);
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
        StringBuffer sb = new StringBuffer();
        sb.append("delete from ").append(classname).append(" where MCRFROM = '").append(from).append("'");
        if ((to != null) && ((to = to.trim()).length() > 0)) {
            sb.append(" and MCRTO = '").append(to).append("'");
        }
        if ((type != null) && ((type = type.trim()).length() > 0)) {
            sb.append(" and MCRTYPE = '").append(type).append("'");
        }
        logger.debug("Deleting " + from + " from database MCRLINKHREF");
        Session session = getSession();
        int deleted = session.createQuery(sb.toString()).executeUpdate();
        logger.debug((new Integer(deleted)).toString() + " items deleted.");
    }

    /**
     * The method count the number of references with '%from%' and 'to' and
     * optional 'type' and optional 'restriction%' values of the table.
     * 
     * @param fromtype
     *            a substing in the from ID as String, it can be null
     * @param to
     *            the object ID as String, which is referenced
     * @param type
     *            the refernce type, it can be null
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public final int countTo(String fromtype, String to, String type, String restriction) {
        Session session = getSession();
        Number returns;
        StringBuffer qBf = new StringBuffer(1024);
        qBf.append("select count(key.mcrfrom) from ").append(classname).append(" where MCRTO like ").append('\'').append(to).append('\'');

        if ((type != null) && (type.length() != 0)) {
            qBf.append(" and MCRTYPE = \'").append(type).append('\'');
        }
        if ((restriction != null) && (restriction.length() != 0)) {
            qBf.append(" and MCRTO like \'").append(restriction).append('\'');
        }
        if ((fromtype != null) && (fromtype.length() != 0)) {
            qBf.append(" and MCRFROM like \'%_").append(fromtype).append("_%\'");
        }

        Query q = session.createQuery(qBf.toString());
        returns = (Number) q.uniqueResult();

        return returns.intValue();
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
        Map<Object, Object> map = new HashMap<Object, Object>();
        Session session = getSession();
        String query = "select count(key.mcrfrom), key.mcrto from " + classname + " where MCRTO like '" + mcrtoPrefix + "%' group by key.mcrto";
        logger.debug("HQL-Statement: " + query);
        Iterator results = session.createQuery(query).list().iterator();
        while (results.hasNext()) {
            Object[] row = (Object[]) results.next();
            map.put(row[1], row[0]);
        }
        return map;
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            child, classid, parent, reference and derivate.
     * @return List of Strings (Source-IDs)
     */
    public List getSourcesOf(String to, String type) {
        Session session = getSession();
        StringBuffer querySB = new StringBuffer("select key.mcrfrom from ").append(classname).append(" where MCRTO='").append(to).append("'");
        if ((type != null) && (type.trim().length() > 0)) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        logger.debug("HQL-Statement: " + query);
        List returns;
        returns = session.createQuery(query).list();
        return returns;
    }

    /**
     * Returns a List of all link destinations of <code>destination</code>
     * 
     * @param source
     *            source-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            child, classid, parent, reference and derivate.
     * @return List of Strings (Destination-IDs)
     */
    public List getDestinationsOf(String source, String type) {
        Session session = getSession();
        StringBuffer querySB = new StringBuffer("select key.mcrto from ").append(classname).append(" where MCRFROM='").append(source).append("'");
        if ((type != null) && (type.trim().length() != 0)) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        logger.debug("HQL-Statement: " + query);
        List returns;
        returns = session.createQuery(query).list();
        return returns;
    }

}