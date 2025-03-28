/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.hibernate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.backend.jpa.links.MCRLINKHREF;
import org.mycore.backend.jpa.links.MCRLINKHREFPK;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRLinkTableInterface;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * This class implements the MCRLinkTableInterface.
 *
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 */
public class MCRHIBLinkTableStore implements MCRLinkTableInterface {

    private static final Logger LOGGER = LogManager.getLogger();

    private String classname = MCRLINKHREF.class.getCanonicalName();

    /**
     * The constructor for the class MCRHIBLinkTableStore.
     */

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
    @Override
    public final void create(String from, String to, String type, String attr) {
        String fromChecked = checkAttributeIsNotEmpty(from, "from");
        String toChecked = checkAttributeIsNotEmpty(to, "to");
        String typeChecked = checkAttributeIsNotEmpty(type, "type");
        String attrChecked = MCRUtils.filterTrimmedNotEmpty(attr).orElse("");
        EntityManager entityMananger = MCREntityManagerProvider.getCurrentEntityManager();
        LOGGER.debug("Inserting {}/{}/{} into database MCRLINKHREF", fromChecked, toChecked, typeChecked);

        MCRLINKHREFPK key = getKey(fromChecked, toChecked, typeChecked);
        MCRLINKHREF linkHref = entityMananger.find(MCRLINKHREF.class, key);
        if (linkHref != null) {
            linkHref.setMcrattr(attrChecked);
        } else {
            linkHref = new MCRLINKHREF();
            linkHref.setKey(key);
            linkHref.setMcrattr(attrChecked);
            entityMananger.persist(linkHref);
        }
    }

    private static String checkAttributeIsNotEmpty(String attr, String name) {
        return MCRUtils.filterTrimmedNotEmpty(attr)
            .orElseThrow(() -> new MCRPersistenceException("The " + name + " value is null or empty."));
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
    @Override
    public final void delete(String from, String to, String type) {
        String fromChecked = checkAttributeIsNotEmpty(from, "from");
        StringBuilder sb = new StringBuilder();
        sb.append("from ").append(classname).append(" where key.mcrfrom = '").append(fromChecked).append('\'');
        MCRUtils.filterTrimmedNotEmpty(to)
            .ifPresent(trimmedTo -> sb.append(" and key.mcrto = '").append(trimmedTo).append('\''));
        MCRUtils.filterTrimmedNotEmpty(type)
            .ifPresent(trimmedType -> sb.append(" and key.mcrtype = '").append(trimmedType).append('\''));
        LOGGER.debug("Deleting {} from database MCRLINKHREF", fromChecked);
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        em.createQuery(sb.toString(), MCRLINKHREF.class)
            .getResultList()
            .forEach(em::remove);
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
    @Override
    public final int countTo(String fromtype, String to, String type, String restriction) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        Number returns;
        StringBuilder qBf = new StringBuilder(1024);
        qBf.append("select count(key.mcrfrom) from ").append(classname).append(" where key.mcrto like ").append('\'')
            .append(to).append('\'');

        if (type != null && type.length() != 0) {
            qBf.append(" and key.mcrtype = '").append(type).append('\'');
        }
        if (restriction != null && restriction.length() != 0) {
            qBf.append(" and key.mcrto like '").append(restriction).append('\'');
        }
        if (fromtype != null && fromtype.length() != 0) {
            qBf.append(" and key.mcrfrom like '%_").append(fromtype).append("_%'");
        }

        TypedQuery<Number> q = em.createQuery(qBf.toString(), Number.class);
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
    @Override
    public Map<String, Number> getCountedMapOfMCRTO(String mcrtoPrefix) {
        Map<String, Number> map = new HashMap<>();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<Object[]> groupQuery = em.createNamedQuery("MCRLINKHREF.group", Object[].class);
        groupQuery.setParameter("like", mcrtoPrefix + '%');
        groupQuery.getResultList()
            .stream()
            .forEach(row -> map.put((String) row[1], (Number) row[0]));
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
    @Override
    public Collection<String> getSourcesOf(String to, String type) {
        boolean withType = type != null && !type.isBlank();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> toQuery = em.createNamedQuery(
            withType ? "MCRLINKHREF.getSourcesWithType" : "MCRLINKHREF.getSources", String.class);
        toQuery.setParameter("to", to);
        if (withType) {
            toQuery.setParameter("type", type);
        }
        return toQuery.getResultList();
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
    @Override
    public Collection<String> getDestinationsOf(String source, String type) {
        boolean withType = type != null && !type.isBlank();
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        TypedQuery<String> toQuery = em.createNamedQuery(
            withType ? "MCRLINKHREF.getDestinationsWithType" : "MCRLINKHREF.getDestinations", String.class);
        toQuery.setParameter("from", source);
        if (withType) {
            toQuery.setParameter("type", type);
        }
        return toQuery.getResultList();
    }

}
