package org.mycore.services.acl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

public class MCRACLHIBAccess {

	private static Logger LOGGER = Logger.getLogger(MCRACLHIBAccess.class);

	public List getAccess(){
		return MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class).list();
	}
	
	public List getAccessPermission(String objidFilter, String acpoolFilter){
        return getAccessPermission(objidFilter, acpoolFilter, null);
    }
    
	public List getAccessPermission(String objidFilter, String acpoolFilter, String ridFilter){
		Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class);
		
		if (objidFilter != null){
			LOGGER.info("OBJID Filter: " + objidFilter + "\t" + objidFilter.replaceAll("\\*", "%"));
			query = query.add(Restrictions.like("key.objid", objidFilter.replaceAll("\\*", "%")));
		}
		
		if (acpoolFilter != null){
			LOGGER.info("OBJID Filter: " + acpoolFilter + "\t" + acpoolFilter.replaceAll("\\*", "%"));
			query = query.add(Restrictions.like("key.acpool", acpoolFilter.replaceAll("\\*", "%")));
		}
        
        if (ridFilter != null){
            LOGGER.info("RID Filter: " + ridFilter);
            query = query.add(Restrictions.like("rule.rid", ridFilter.replaceAll("\\*", "%")));
        }
		
        query.addOrder(Order.asc("key.objid"));
        query.addOrder(Order.asc("key.acpool"));
        
		return query.list();
	}
	
	public List getAccessRule(){
        Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESSRULE.class);
		return query.list();
	}
	
	public void savePermChanges(Map diffMap){
		MCRAccessStore accessStore = MCRAccessStore.getInstance();
		
		for (Iterator it = ((List)diffMap.get("update")).iterator(); it.hasNext();){
			accessStore.updateAccessDefinition((MCRRuleMapping)it.next());
		}
		
		for (Iterator it = ((List)diffMap.get("save")).iterator(); it.hasNext();){
			accessStore.createAccessDefinition((MCRRuleMapping)it.next());
		}
		
		for (Iterator it = ((List)diffMap.get("delete")).iterator(); it.hasNext();){
			accessStore.deleteAccessDefinition((MCRRuleMapping)it.next());
		}
	}
	
	public void saveRuleChanges(Map diffMap){
        MCRRuleStore ruleStore = MCRRuleStore.getInstance();
        MCRAccessInterface AI = MCRAccessControlSystem.instance();
        
        for (Iterator it = ((List)diffMap.get("update")).iterator(); it.hasNext();){
            MCRACCESSRULE rule = (MCRACCESSRULE) it.next();
            ruleStore.updateRule(new MCRAccessRule(rule.getRid(), "ACL-Editor", new Date(), rule.getRule(), rule.getDescription()));
        }
        
        for (Iterator it = ((List)diffMap.get("save")).iterator(); it.hasNext();){
            MCRACCESSRULE accessRule = (MCRACCESSRULE)it.next();
            AI.createRule(accessRule.getRule(), "ACL-Editor", accessRule.getDescription());
        }
        
        for (Iterator it = ((List)diffMap.get("delete")).iterator(); it.hasNext();){
            ruleStore.deleteRule((String)it.next());
        }
    }
}
