package org.mycore.services.acl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.mycore.common.config.MCRConfiguration;
import org.mycore.services.acl.filter.MCRAclCriterionFilter;

/**
 * 
 * 
 * @author Huu Chi Vu
 * @author Matthias Eichner
 */
public abstract class MCRACLHIBAccess {

    private static Logger LOGGER = Logger.getLogger(MCRACLHIBAccess.class);


    /**
     * Returns a list of <code>MCRACCESS</code> rule mappings. The list is filterd by all
     * <code>MCRAclCriterionFilter</code> classes which are defined in the
     * <i>MCR.ACL.Editor.ruleMappingFilter</i> properties.
     * 
     * @param request the incoming http request - needed for filters
     * @return a list of <code>MCRACCESS</code> instances
     */
    @SuppressWarnings("unchecked")
    public static List<MCRACCESS> getRuleMappingList(Properties filterProperties) {
        Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class);
        filterQuery(query, filterProperties, "MCR.ACL.Editor.ruleMappingFilter");
        return query.list();
    }

    /**
     * Returns a list of <code>MCRACCESSRULE</code> rules. The list is filterd by all
     * <code>MCRAclCriterionFilter</code> classes which are defined in the
     * <i>MCR.ACL.Editor.ruleFilter</i> properties.
     * 
     * @param request the incoming http request - needed for filters
     * @return a list of <code>MCRACCESSRULE</code> instances
     */
    @SuppressWarnings("unchecked")
    public static List<MCRACCESSRULE> getRuleList(Properties filterProperties) {
        Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESSRULE.class);
        filterQuery(query, filterProperties, "MCR.ACL.Editor.ruleFilter");
        return query.list();
    }

    /**
     * This method filters a hibernate criteria. The filter classes (instances of
     * <code>MCRAclCriterionFilter</code>) are loaded by reflection.
     * They are selected from the properties with the help of the filterPrefix.
     * 
     * @param query the query to filter
     * @param request the incoming http request - needed for filters
     * @param filterPrefix selects the code>MCRAclCriterionFilter</code> classes
     */
    public static void filterQuery(Criteria query, Properties filterProperties, String filterPrefix) {
        Map<String, String> filters = MCRConfiguration.instance().getPropertiesMap(filterPrefix);
        for(String filterClass : filters.values()) {
            try {
                Class<?> c = Class.forName(filterClass);
                Object o = c.newInstance();
                if(o instanceof MCRAclCriterionFilter) {
                    Criterion criterion = ((MCRAclCriterionFilter)o).filter(filterProperties);
                    if(criterion != null)
                        query.add(criterion);
                } else
                    LOGGER.warn("'" + c.getCanonicalName() + "' doesnt implements MCRAclCriterionFilter. Check the " +
                                filterPrefix + ".xxx filters.");
            } catch(Exception exc) {
                LOGGER.error("while loading acl filter '" + filterClass + "'!", exc);
            }
        }
    }

    /**
     * Saves all rule mapping changes to the <code>MCRAccesStore</code>. That includes
     * update-, save- and delete operations on <code>MCRRuleMapping</code> objects.
     * These are set in the diffMap. The key is the operation and the value a list of
     * the rule mappings.
     * 
     * @param diffMap map with changes
     */
    public static void saveRuleMappingChanges(Map<MCRAclAction, List<MCRRuleMapping>> diffMap) {
        MCRAccessStore accessStore = MCRAccessStore.getInstance();

        List<MCRRuleMapping> updateList = diffMap.get(MCRAclAction.update);
        List<MCRRuleMapping> saveList = diffMap.get(MCRAclAction.save);
        List<MCRRuleMapping> deleteList = diffMap.get(MCRAclAction.delete);

        if (updateList != null) {
            for (MCRRuleMapping accDef : updateList) {
                String rid = accDef.getRuleId();
                if (rid == null || rid.trim().length() <= 0)
                    throw new MCRException("The rule ID should not be null, empty or just spaces");

                String acpool = accDef.getPool();
                if (acpool == null || acpool.trim().length() <= 0)
                    throw new MCRException("The AcPool ID should not be null, empty or just spaces");

                String objid = accDef.getObjId();
                if (objid == null || objid.trim().length() <= 0)
                    throw new MCRException("The object ID should not be null, empty or just spaces");

                accessStore.updateAccessDefinition(accDef);
            }
        }

        if (saveList != null)
            for (MCRRuleMapping accDef : saveList)
                accessStore.createAccessDefinition(accDef);

        if (deleteList != null)
            for (MCRRuleMapping accDef : deleteList)
                accessStore.deleteAccessDefinition(accDef);
    }

    /**
     * Saves all rule changes to the <code>MCRRuleStore</code>. That includes
     * update-, save- and delete operations on <code>MCRACCESSRULE</code> objects.
     * These are set in the diffMap. The key is the operation and the value a list of
     * rules.
     * 
     * @param diffMap map with changes
     */
    public static void saveRuleChanges(Map<MCRAclAction, List<MCRACCESSRULE>> diffMap) {
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        MCRCache<String, MCRAccessRule> cache = MCRAccessControlSystem.getCache();

        List<MCRACCESSRULE> updateList = diffMap.get(MCRAclAction.update);
        List<MCRACCESSRULE> saveList = diffMap.get(MCRAclAction.save);
        List<MCRACCESSRULE> deleteList = diffMap.get(MCRAclAction.delete);

        if (updateList != null)
            for (MCRACCESSRULE rule : updateList) {
                String rid = rule.getRid();
                String ruleString = rule.getRule();
                String desc = rule.getDescription();
                String creator = rule.getCreator();
                StringBuilder debugMSG = new StringBuilder("Update: ");
                debugMSG.append(rid).append(" - ");
                debugMSG.append(ruleString).append(" - ");
                debugMSG.append(desc);

                LOGGER.debug(debugMSG.toString());
                MCRAccessRule accessRule = new MCRAccessRule(rid, creator, new Date(), ruleString, desc);
                ruleStore.updateRule(accessRule);
                cache.put(rid, accessRule);
            }

        if (saveList != null)
            for (MCRACCESSRULE rule : saveList) {
                String rid = rule.getRid();
                String ruleString = rule.getRule();
                String desc = rule.getDescription();
                String creator = rule.getCreator();
                MCRAccessRule accessRule = new MCRAccessRule(rid, creator, new Date(), ruleString, desc);

                ruleStore.createRule(accessRule);
                cache.put(rid, accessRule); // upadte cache
            }

        if (deleteList != null)
            for (MCRACCESSRULE rule : deleteList) {
                String rid = rule.getRid();
                if (!isRuleInUse(rid)) {
                    ruleStore.deleteRule(rid);
                    cache.remove(rid);
                    LOGGER.debug("Rule " + rid + " deleted!");
                } else
                    LOGGER.debug("Rule " + rid + " is in use, don't deleted!");
            }
    }

    /**
     * Checks if a rule mappings uses the rule.
     * 
     * @param ruleid the rule id to check
     * @return true if the rule is used, otherwise false
     */
    public static boolean isRuleInUse(String ruleid) {
        Session session = MCRHIBConnection.instance().getSession();
        Query query = session.createQuery("from MCRACCESS as accdef where accdef.rule.rid = '" + ruleid + "'");
        return !query.list().isEmpty();
    }
}