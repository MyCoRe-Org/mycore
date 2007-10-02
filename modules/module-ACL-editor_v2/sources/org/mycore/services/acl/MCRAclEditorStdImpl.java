package org.mycore.services.acl;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Element;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

import com.ibm.icu.util.StringTokenizer;

public class MCRAclEditorStdImpl extends MCRAclEditor{
    MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
    MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

    @Override
    public Element dataRequest(HttpServletRequest request) {
        LOGGER.debug("Handling data request.");
        
        String action = request.getParameter("action");
        Element elem = null;
        
        if (action.equals("setFilter"))
            elem = setFilter(request);
        if (action.equals("deleteFilter"))
            elem = getPermission(null, null);
        if (action.equals("createNewPerm"))
            elem = createNewPerm(request);
        if (action.equals("submit"))
            elem = processSubmission(request);
        
        return elem;
    }

    @Override
    public Element getPermEditor(HttpServletRequest request) {
        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");
        
        return getPermission(objidFilter, acpoolFilter);
    }

    private Element setFilter(HttpServletRequest request){
        String objIdFilter = request.getParameter("ObjIdFilter");
        String acPoolFilter = request.getParameter("AcPoolFilter");
        
        if (objIdFilter.equals(""))
            objIdFilter = null;
        if (acPoolFilter.equals(""))
            acPoolFilter = null;
        
        LOGGER.debug("ObjIdFilter: " + objIdFilter);
        LOGGER.debug("AcPoolFilter: " + acPoolFilter);
        
        return getPermission(objIdFilter, acPoolFilter);
    }
    
    private Element getPermission(String objIdFilter, String acPoolFilter){
        Element elem = XMLProcessing.access2XML(HIBA.getAccessPermission(objIdFilter, acPoolFilter), true);
        elem.addContent(getFilterElem(objIdFilter, acPoolFilter));
        
        return elem;
    }
    
    private Element getFilterElem(String objidFilter, String acpoolFilter) {
        Element elem = XMLProcessing.accessFilter2XML(objidFilter, acpoolFilter);
        return elem;
    }
    
    private Element createNewPerm(HttpServletRequest request) {
        
        String objId = request.getParameter("newPermOBJID");
        String acPool = request.getParameter("newPermACPOOL");
        String ruleId = request.getParameter("newPermRID");
        
        LOGGER.debug("ObjId: " + objId);
        LOGGER.debug("AcPool: " + acPool);
        LOGGER.debug("RuleId: " + ruleId);
        
        MCRRuleMapping perm = XMLProcessing.createRuleMapping(ruleId, acPool, objId);
        MCRAccessStore.getInstance().createAccessDefinition(perm);
        
        return getPermission(null, null);
    }
    
    private Element processSubmission(HttpServletRequest request) {
        LOGGER.debug("Processing submission.");
        
        Map<String, String[]> parameterMap = request.getParameterMap();
        Iterator<String> iter = parameterMap.keySet().iterator();
        
        LinkedList<MCRRuleMapping> updateAccess = new LinkedList<MCRRuleMapping>();
        LinkedList<MCRRuleMapping> deleteAccess = new LinkedList<MCRRuleMapping>();
        
        final String change = "changed$_RID$";
        final String delete = "deleted$_RID$";
        
        while (iter.hasNext()){
            // key should be in the form changed$_RID$ObjId$AcPool
            String key = iter.next();
            
            LOGGER.debug("Param key: " + key);
            
            if (key.startsWith(change)){
                LOGGER.debug("RID changed: " + key);
                
                MCRRuleMapping ruleMapping = extractRuleMapping(parameterMap, change, key);
                
                updateAccess.add(ruleMapping);
            }
            
            if (key.startsWith(delete)){
                LOGGER.debug("RID deleted: " + key);
                
                MCRRuleMapping ruleMapping = extractRuleMapping(parameterMap, delete, key);
                
                deleteAccess.add(ruleMapping);
            }
        }
        
        HashMap<String, LinkedList<MCRRuleMapping>> diffMap = new HashMap<String, LinkedList<MCRRuleMapping>>();
        diffMap.put("update", updateAccess);
        diffMap.put("delete", deleteAccess);
        
        HIBA.savePermChanges(diffMap);
        
        return getPermission(null, null);
    }

    private MCRRuleMapping extractRuleMapping(Map<String, String[]> parameterMap, String remove, String key) {
        StringTokenizer token = new StringTokenizer(key.substring(remove.length() - 1), "$");
        String objId = token.nextToken();
        String acPool = token.nextToken();
        String ruleId = parameterMap.get(key)[0];
        
        LOGGER.debug("ObjId: " + objId);
        LOGGER.debug("AcPool: " + acPool);
        LOGGER.debug("RuleId: " + ruleId);
        
        MCRRuleMapping ruleMapping = XMLProcessing.createRuleMapping(ruleId, acPool, objId);
        return ruleMapping;
    }


}
