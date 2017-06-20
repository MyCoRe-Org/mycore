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

package org.mycore.backend.jpa.access;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.jpa.MCREntityManagerProvider;

/**
 * JPA implementation of acceess store to manage access rights
 * 
 * @author Arne Seifert
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRJPAAccessStore extends MCRAccessStore {
    private final DateFormat dateFormat = new SimpleDateFormat(sqlDateformat, Locale.ROOT);

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getRuleID(String objID, String acPool) {

        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<MCRACCESS> ac = query.from(MCRACCESS.class);
        try {
            return em.createQuery(
                query.select(ac.get(MCRACCESS_.rule).get(MCRACCESSRULE_.rid))
                    .where(cb.equal(ac.get(MCRACCESS_.key).get(MCRACCESSPK_.objid), objID),
                        cb.equal(ac.get(MCRACCESS_.key).get(MCRACCESSPK_.acpool), acPool)))
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * method creates a new AccessDefinition in db
     * 
     * @param rulemapping
     *            with values
     */
    @Override
    public void createAccessDefinition(MCRRuleMapping rulemapping) {

        if (!existAccessDefinition(rulemapping.getPool(), rulemapping.getObjId())) {
            Session session = MCRHIBConnection.instance().getSession();
            MCRACCESSRULE accessRule = getAccessRule(rulemapping.getRuleId());
            if (accessRule == null) {
                throw new NullPointerException("Cannot map a null rule.");
            }
            MCRACCESS accdef = new MCRACCESS();

            accdef.setKey(new MCRACCESSPK(rulemapping.getPool(), rulemapping.getObjId()));
            accdef.setRule(accessRule);
            accdef.setCreator(rulemapping.getCreator());
            accdef.setCreationdate(Timestamp.valueOf(dateFormat.format(rulemapping.getCreationdate())));
            session.save(accdef);
        }
    }

    /**
     * internal helper method to check existance of object
     * 
     * @param ruleid
     * @param pool
     * @param objid
     * @return boolean value
     */
    private boolean existAccessDefinition(String pool, String objid) {
        MCRACCESSPK key = new MCRACCESSPK(pool, objid);
        return MCREntityManagerProvider.getCurrentEntityManager().find(MCRACCESS.class, key) != null;
    }

    @Override
    public boolean existsRule(String objid, String pool) {
        if (objid == null || objid.equals("")) {
            LOGGER.warn("empty parameter objid in existsRule");
            return false;
        }
        try {
            return getRuleID(objid, pool) != null;
        } catch (NoResultException e) {
            return false;
        }
    }

    /**
     * delete given definition in db
     * 
     * @param rulemapping
     *            rule to be deleted
     */
    @Override
    public void deleteAccessDefinition(MCRRuleMapping rulemapping) {

        Session session = MCRHIBConnection.instance().getSession();
        session.createQuery(
            "delete MCRACCESS " + "where ACPOOL = '" + rulemapping.getPool() + "'" + " AND OBJID = '"
                + rulemapping.getObjId() + "'")
            .executeUpdate();
    }

    /**
     * update AccessDefinition in db for given MCRAccessData
     */
    @Override
    public void updateAccessDefinition(MCRRuleMapping rulemapping) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRACCESSRULE accessRule = getAccessRule(rulemapping.getRuleId());
        if (accessRule == null) {
            throw new NullPointerException("Cannot map a null rule.");
        }
        // update
        MCRACCESS accdef = (MCRACCESS) session.get(MCRACCESS.class,
            new MCRACCESSPK(rulemapping.getPool(), rulemapping.getObjId()));
        accdef.setRule(accessRule);
        accdef.setCreator(rulemapping.getCreator());
        accdef.setCreationdate(Timestamp.valueOf(dateFormat.format(rulemapping.getCreationdate())));
        session.update(accdef);
    }

    /**
     * method returns AccessDefinition for given key values
     * 
     * @param pool
     *            name of accesspool
     * @param objid
     *            objectid of MCRObject
     * @return MCRAccessData
     */
    @Override
    public MCRRuleMapping getAccessDefinition(String pool, String objid) {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<MCRACCESS> query = cb.createQuery(MCRACCESS.class);
        Root<MCRACCESS> root = query.from(MCRACCESS.class);
        MCRRuleMapping rulemapping = new MCRRuleMapping();
        try {
            MCRACCESS data = em
                .createQuery(query.where(cb.equal(root.get(MCRACCESS_.key), new MCRACCESSPK(pool, objid))))
                .getSingleResult();
            rulemapping.setCreationdate(data.getCreationdate());
            rulemapping.setCreator(data.getCreator());
            rulemapping.setObjId(data.getKey().getObjid());
            rulemapping.setPool(data.getKey().getAcpool());
            rulemapping.setRuleId(data.getRule().getRid());
            em.detach(data);
        } catch (NoResultException e) {
            //returning empty rulemapping is fine
        }
        return rulemapping;
    }

    @Override
    public ArrayList<String> getMappedObjectId(String pool) {

        Session session = MCRHIBConnection.instance().getSession();
        List<MCRACCESS> l = session.createQuery("from MCRACCESS where ACPOOL = '" + pool + "'", MCRACCESS.class)
            .getResultList();
        return l.stream()
            .map(aL -> aL.getKey().getObjid())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public ArrayList<String> getPoolsForObject(String objid) {

        Session session = MCRHIBConnection.instance().getSession();
        List<MCRACCESS> l = session.createQuery("from MCRACCESS where OBJID = '" + objid + "'", MCRACCESS.class)
            .getResultList();
        return l.stream()
            .map(access -> access.getKey().getAcpool())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public ArrayList<String> getDatabasePools() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaQuery<MCRACCESS> query = em.getCriteriaBuilder().createQuery(MCRACCESS.class);
        return em.createQuery(query.select(query.from(MCRACCESS.class)))
            .getResultList()
            .stream()
            .map(MCRACCESS::getKey)
            .map(MCRACCESSPK::getAcpool)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<String> getDistinctStringIDs() {
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Path<String> selection = query.from(MCRACCESS.class).get(MCRACCESS_.key).get(MCRACCESSPK_.objid);
        return em.createQuery(query.select(selection).distinct(true).orderBy(cb.asc(selection))).getResultList();
    }

    /**
     * Checks if a rule mappings uses the rule.
     * 
     * @param ruleid the rule id to check
     * @return true if the rule exists and is used, otherwise false
     */
    @Override
    public boolean isRuleInUse(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        Query<MCRACCESS> query = session
            .createQuery("from MCRACCESS as accdef where accdef.rule.rid = '" + ruleid + "'", MCRACCESS.class);
        return !query.getResultList().isEmpty();
    }

    private static MCRACCESSRULE getAccessRule(String rid) {
        Session session = MCRHIBConnection.instance().getSession();
        return (MCRACCESSRULE) session.get(MCRACCESSRULE.class, rid);
    }
}
