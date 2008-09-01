package org.mycore.services.acl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;

public class MCRACLHIBAccess {
    String uid = MCRSessionMgr.getCurrentSession().getCurrentUserID();

    private static Logger LOGGER = Logger.getLogger(MCRACLHIBAccess.class);

    public List getAccess() {
        return MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class).list();
    }

    public List getAccessPermission(String objidFilter, String acpoolFilter) {
        return getAccessPermission(objidFilter, acpoolFilter, null);
    }

    public List getAccessPermission(String objidFilter, String acpoolFilter, String ridFilter) {
        Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class);

        if (objidFilter != null && !objidFilter.equals("")) {
            LOGGER.info("OBJID Filter: " + objidFilter + "\t" + objidFilter.replaceAll("\\*", "%"));
            query = query.add(Restrictions.like("key.objid", objidFilter.replaceAll("\\*", "%")));
        }

        if (acpoolFilter != null && !acpoolFilter.equals("")) {
            LOGGER.info("ACPOOL Filter: " + acpoolFilter + "\t" + acpoolFilter.replaceAll("\\*", "%"));
            query = query.add(Restrictions.like("key.acpool", acpoolFilter.replaceAll("\\*", "%")));
        }

        if (ridFilter != null && !ridFilter.equals("")) {
            LOGGER.info("RID Filter: " + ridFilter);
            query = query.add(Restrictions.like("rule.rid", ridFilter.replaceAll("\\*", "%")));
        }

        query.addOrder(Order.asc("key.objid"));
        query.addOrder(Order.asc("key.acpool"));

        return query.list();
    }

    public List getAccessRule() {
        Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESSRULE.class);
        List list = query.list();

        return list;
    }

    public void savePermChanges(Map diffMap) {
        MCRAccessStore accessStore = MCRAccessStore.getInstance();

        List updateList = (List) diffMap.get("update");
        List saveList = (List) diffMap.get("save");
        List deleteList = (List) diffMap.get("delete");

        if (updateList != null) {
            for (Iterator it = updateList.iterator(); it.hasNext();) {
                MCRRuleMapping accDef = (MCRRuleMapping) it.next();
                
                String rid = accDef.getRuleId();
                if (rid == null || rid.trim().length() <= 0){
                    throw new MCRException("The rule ID should not be null, empty or just spaces");
                }
                
                String acpool = accDef.getPool();
                if (acpool == null || acpool.trim().length() <= 0){
                    throw new MCRException("The AcPool ID should not be null, empty or just spaces");
                }
                
                String objid = accDef.getObjId();
                if (objid == null || objid.trim().length() <= 0){
                    throw new MCRException("The object ID should not be null, empty or just spaces");
                }
                
                accessStore.updateAccessDefinition(accDef);
            }
        }

        if (saveList != null) {
            for (Iterator it = saveList.iterator(); it.hasNext();) {
                MCRRuleMapping next = (MCRRuleMapping) it.next();
                accessStore.createAccessDefinition(next);
            }
        }

        if (deleteList != null) {
            for (Iterator it = deleteList.iterator(); it.hasNext();) {
                accessStore.deleteAccessDefinition((MCRRuleMapping) it.next());
            }
        }
    }

    public void saveRuleChanges(Map diffMap) {
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        MCRCache cache = MCRAccessControlSystem.getCache();

        List updateList = (List) diffMap.get("update");
        List saveList = (List) diffMap.get("save");
        List deleteList = (List) diffMap.get("delete");

        if (updateList != null)
            for (Iterator it = updateList.iterator(); it.hasNext();) {
                MCRACCESSRULE rule = (MCRACCESSRULE) it.next();
                String rid = rule.getRid();
                String ruleString = rule.getRule();
                String desc = rule.getDescription();
                StringBuffer debugMSG = new StringBuffer("Update: ");
                debugMSG.append(rid).append(" - ");
                debugMSG.append(ruleString).append(" - ");
                debugMSG.append(desc);

                LOGGER.debug(debugMSG.toString());
                MCRAccessRule accessRule = new MCRAccessRule(rid, uid, new Date(), ruleString, desc);
                ruleStore.updateRule(accessRule);
                cache.put(rid, accessRule);
            }

        if (saveList != null)
            for (Iterator it = saveList.iterator(); it.hasNext();) {
                MCRACCESSRULE rule = (MCRACCESSRULE) it.next();
                String rid = rule.getRid();
                String ruleString = rule.getRule();
                String desc = rule.getDescription();
                MCRAccessRule accessRule = new MCRAccessRule(rid, uid, new Date(), ruleString, desc);

                ruleStore.createRule(accessRule);
                cache.put(rid, accessRule); // upadte cache
            }

        if (deleteList != null)
            for (Iterator it = deleteList.iterator(); it.hasNext();) {
                String rid = (String) it.next();

                if (ruleIsInUse(rid).isEmpty()) {
                    ruleStore.deleteRule(rid);
                    cache.remove(rid);
                    LOGGER.debug("Rule " + rid + " deleted!");
                } else {
                    LOGGER.debug("Rule " + rid + " is in use, don't deleted!");
                }
            }

    }

    public List ruleIsInUse(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("from MCRACCESS as accdef where accdef.rule.rid = '" + ruleid + "'");
        return query.list();
    }
}
