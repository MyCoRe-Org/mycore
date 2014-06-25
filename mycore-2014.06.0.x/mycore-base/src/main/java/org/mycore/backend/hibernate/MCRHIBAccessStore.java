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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

/**
 * Hibernate implementation of acceess store to manage access rights
 * 
 * @author Arne Seifert
 * 
 */
public class MCRHIBAccessStore extends MCRAccessStore {
    private final DateFormat dateFormat = new SimpleDateFormat(sqlDateformat);
    private static final Logger LOGGER = Logger.getLogger(MCRHIBAccessStore.class);

    @Override
    public String getRuleID(String objID, String ACPool) {

        Session session = MCRHIBConnection.instance().getSession();
        Criteria c = session.createCriteria(MCRACCESS.class).setProjection(Projections.property("rule.rid")).add(
                Restrictions.eq("key.objid", objID)).add(Restrictions.eq("key.acpool", ACPool));
        return (String) c.uniqueResult();
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
    @SuppressWarnings("unchecked")
    private boolean existAccessDefinition(String pool, String objid) {
        Session session = MCRHIBConnection.instance().getSession();
        MCRACCESSPK key = new MCRACCESSPK(pool, objid);
        List<MCRACCESS> l = session.createCriteria(MCRACCESS.class).add(Restrictions.eq("key", key)).list();
        return l.size() == 1;
    }

    @Override
    public boolean existsRule(String objid, String pool) {
        Session session = MCRHIBConnection.instance().getSession();

        if (objid == null || objid.equals("")) {
            LOGGER.warn("empty parameter objid in existsRule");
            return false;
        }

        Criteria criteria = session.createCriteria(MCRACCESS.class);
        criteria.setProjection(Projections.rowCount());
        criteria.add(Restrictions.eq("key.objid", objid));
        if (pool != null && !pool.equals("")) {
            criteria.add(Restrictions.eq("key.acpool", pool));
        }
        int count = ((Number) criteria.uniqueResult()).intValue();
        return count > 0;
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
                "delete MCRACCESS " + "where ACPOOL = '" + rulemapping.getPool() + "'" + " AND OBJID = '" + rulemapping.getObjId() + "'")
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
        MCRACCESS accdef = (MCRACCESS) session.get(MCRACCESS.class, new MCRACCESSPK(rulemapping.getPool(), rulemapping.getObjId()));
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

        Session session = MCRHIBConnection.instance().getSession();
        MCRRuleMapping rulemapping = new MCRRuleMapping();
        MCRACCESS data = (MCRACCESS) session.createCriteria(MCRACCESS.class).add(Restrictions.eq("key", new MCRACCESSPK(pool, objid)))
                .list().get(0);
        if (data != null) {
            rulemapping.setCreationdate(data.getCreationdate());
            rulemapping.setCreator(data.getCreator());
            rulemapping.setObjId(data.getKey().getObjid());
            rulemapping.setPool(data.getKey().getAcpool());
            rulemapping.setRuleId(data.getRule().getRid());
        }
        session.evict(data);
        return rulemapping;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> getMappedObjectId(String pool) {

        Session session = MCRHIBConnection.instance().getSession();
        ArrayList<String> ret = new ArrayList<String>();

        List<MCRACCESS> l = session.createQuery("from MCRACCESS where ACPOOL = '" + pool + "'").list();
        for (MCRACCESS aL : l) {
            ret.add(aL.getKey().getObjid());
        }

        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> getPoolsForObject(String objid) {

        Session session = MCRHIBConnection.instance().getSession();
        ArrayList<String> ret = new ArrayList<String>();
        List<MCRACCESS> l = session.createQuery("from MCRACCESS where OBJID = '" + objid + "'").list();
        for (MCRACCESS access : l) {
            ret.add(access.getKey().getAcpool());
        }

        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> getDatabasePools() {

        ArrayList<String> ret = new ArrayList<String>();
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRACCESS> l = session.createCriteria(MCRACCESS.class).list();
        for (MCRACCESS aL : l) {
            if (!ret.contains(aL.getKey().getAcpool())) {
                ret.add(aL.getKey().getAcpool());
            }
        }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getDistinctStringIDs() {
        List<String> ret;
        Session session = MCRHIBConnection.instance().getSession();
        String query = "select distinct(key.objid) from MCRACCESS order by OBJID";
        ret = session.createQuery(query).list();
        return ret;
    }

    private static MCRACCESSRULE getAccessRule(String rid) {
        Session session = MCRHIBConnection.instance().getSession();
        return (MCRACCESSRULE) session.get(MCRACCESSRULE.class, rid);
    }
}
