package org.mycore.services.acl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.MCRHIBAccessStore;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

public class MCRACLHIBAccess {

	private static Logger LOGGER = Logger.getLogger(MCRACLHIBAccess.class);

	public List getAccess(){
		return MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class).list();
	}
	
	public List getAccessPermission(String objidFilter, String acpoolFilter){
		Criteria query = MCRHIBConnection.instance().getSession().createCriteria(MCRACCESS.class);
		
		if (objidFilter != null){
			LOGGER.info("#### OBJID Filter: " + objidFilter + "\t" + objidFilter.replaceAll("\\*", "%"));
			query = query.add(Restrictions.like("key.objid", objidFilter.replaceAll("\\*", "%")));
		}
		
		if (acpoolFilter != null){
			LOGGER.info("#### OBJID Filter: " + acpoolFilter + "\t" + acpoolFilter.replaceAll("\\*", "%"));
			query = query.add(Restrictions.like("key.acpool", acpoolFilter.replaceAll("\\*", "%")));
		}
		
		return query.list();
	}
	
	public List getAccessRule(){
		return MCRHIBConnection.instance().getSession().createCriteria(MCRACCESSRULE.class).list();
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
        
        for (Iterator it = ((List)diffMap.get("update")).iterator(); it.hasNext();){
            ruleStore.updateRule((MCRAccessRule)it.next());
        }
        
        for (Iterator it = ((List)diffMap.get("save")).iterator(); it.hasNext();){
            ruleStore.createRule((MCRAccessRule)it.next());
        }
        
        for (Iterator it = ((List)diffMap.get("delete")).iterator(); it.hasNext();){
            ruleStore.deleteRule((String)it.next());
        }
    }
}
