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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.links.MCRLINKHREF;
import org.mycore.backend.jpa.links.MCRLINKHREFPK;
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
    static Logger LOGGER = LogManager.getLogger(MCRHIBLinkTableStore.class);

    private String classname = MCRLINKHREF.class.getCanonicalName();

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
        if (from == null || (from = from.trim()).length() == 0) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }
        if (to == null || (to = to.trim()).length() == 0) {
            throw new MCRPersistenceException("The to value is null or empty.");
        }
        if (type == null || (type = type.trim()).length() == 0) {
            throw new MCRPersistenceException("The type value is null or empty.");
        }
        if (attr == null || (attr = attr.trim()).length() == 0) {
            attr = "";
        }
        EntityManager entityMananger = MCREntityManagerProvider.getCurrentEntityManager();
        LOGGER.debug("Inserting " + from + "/" + to + "/" + type + " into database MCRLINKHREF");

        MCRLINKHREFPK key = getKey(from, to, type);
        MCRLINKHREF linkHref = entityMananger.find(MCRLINKHREF.class, key);
        if (linkHref != null) {
            linkHref.setMcrattr(attr);
        } else {
            linkHref = new MCRLINKHREF();
            linkHref.setKey(key);
            linkHref.setMcrattr(attr);
            entityMananger.persist(linkHref);
        }
    }

    private static MCRLINKHREFPK getKey(String from, String to, String type) {
        MCRLINKHREFPK pk = new MCRLINKHREFPK();
        pk.setMcrfrom(from);
        pk.setMcrto(to);
        pk.setMcrtype(type);
        return pk;
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
        if (from == null || (from = from.trim()).length() == 0) {
            throw new MCRPersistenceException("The from value is null or empty.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("from ").append(classname).append(" where MCRFROM = '").append(from).append("'");
        if (to != null && (to = to.trim()).length() > 0) {
            sb.append(" and MCRTO = '").append(to).append("'");
        }
        if (type != null && (type = type.trim()).length() > 0) {
            sb.append(" and MCRTYPE = '").append(type).append("'");
        }
        LOGGER.debug("Deleting " + from + " from database MCRLINKHREF");
        Session session = getSession();
        Iterator<MCRLINKHREF> it = session.createQuery(sb.toString(), MCRLINKHREF.class).getResultList().iterator();
        while (it.hasNext()) {
            session.delete(it.next());
        }
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
        StringBuilder qBf = new StringBuilder(1024);
        qBf.append("select count(key.mcrfrom) from ").append(classname).append(" where MCRTO like ").append('\'')
            .append(to).append('\'');

        if (type != null && type.length() != 0) {
            qBf.append(" and MCRTYPE = \'").append(type).append('\'');
        }
        if (restriction != null && restriction.length() != 0) {
            qBf.append(" and MCRTO like \'").append(restriction).append('\'');
        }
        if (fromtype != null && fromtype.length() != 0) {
            qBf.append(" and MCRFROM like \'%_").append(fromtype).append("_%\'");
        }

        Query<Number> q = session.createQuery(qBf.toString(), Number.class);
        returns = q.getSingleResult();

        return returns.intValue();
    }

    /**
     * The method returns a Map of all counted distinct references
     * 
     * @return
     * 
     * the result-map of (key,value)-pairs can be visualized as<br>
     * select count(mcrfrom) as value, mcrto as key from
     * mcrlinkclass|mcrlinkhref where mcrto like mcrtoPrefix + '%' group by
     * mcrto;
     * 
     */
    @SuppressWarnings("unchecked")
    public Map<String, Number> getCountedMapOfMCRTO(String mcrtoPrefix) {
        Map<String, Number> map = new HashMap<String, Number>();
        Session session = getSession();
        String query = "select count(key.mcrfrom), key.mcrto from " + classname + " where MCRTO like '" + mcrtoPrefix
            + "%' group by key.mcrto";
        LOGGER.debug("HQL-Statement: " + query);
        for (Object[] row : (List<Object[]>) session.createQuery(query).list()) {
            map.put((String) row[1], (Number) row[0]);
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
    @SuppressWarnings("unchecked")
    public Collection<String> getSourcesOf(String to, String type) {
        Session session = getSession();
        StringBuilder querySB = new StringBuilder("select key.mcrfrom from ").append(classname).append(" where MCRTO='")
            .append(to).append("'");
        if (type != null && type.trim().length() > 0) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        LOGGER.debug("HQL-Statement: " + query);
        List<String> returns = session.createQuery(query).list();
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
    @SuppressWarnings("unchecked")
    public Collection<String> getDestinationsOf(String source, String type) {
        Session session = getSession();
        StringBuilder querySB = new StringBuilder("select key.mcrto from ").append(classname).append(" where MCRFROM='")
            .append(source).append("'");
        if (type != null && type.trim().length() != 0) {
            querySB.append(" and MCRTYPE = '").append(type).append("'");
        }
        String query = querySB.toString();
        LOGGER.debug("HQL-Statement: " + query);
        List<String> returns = session.createQuery(query).list();
        return returns;
    }

}
