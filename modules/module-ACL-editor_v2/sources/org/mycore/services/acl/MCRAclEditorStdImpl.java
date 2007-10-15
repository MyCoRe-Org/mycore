package org.mycore.services.acl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

import com.ibm.icu.util.StringTokenizer;

public class MCRAclEditorStdImpl extends MCRAclEditor {
    MCRACLHIBAccess HIBA = new MCRACLHIBAccess();

    MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

    /***************************************************************************
     * Implementing abstract methods
     **************************************************************************/

    @Override
    public Element dataRequest(HttpServletRequest request) {
        LOGGER.debug("Handling data request.");

        String action = request.getParameter("action");
        Element elem = null;

        if (action.equals("setFilter"))
            elem = setFilter(request);
        else if (action.equals("deleteFilter"))
            elem = getPermission(null, null);
        else if (action.equals("createNewPerm"))
            elem = createNewPerm(request);
        else if (action.equals("createNewRule"))
            elem = createNewRule(request);
        else if (action.equals("getRuleAsItems"))
            elem = getRuleAsItems(request);
        else if (action.equals("submitPerm"))
            elem = processSubmissionPerm(request);
        else if (action.equals("submitRule"))
            elem = processSubmissionRule(request);

        return elem;
    }

    @Override
    public Element getPermEditor(HttpServletRequest request) {
        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");

        return getPermission(objidFilter, acpoolFilter);
    }

    @Override
    public Element getRuleEditor(HttpServletRequest request) {
        Element elem = XMLProcessing.ruleSet2XML(HIBA.getAccessRule());
        return elem;
    }

    // End implementing abstract methods

    /***************************************************************************
     * Mapping stuff
     **************************************************************************/

    private Element getPermission(String objIdFilter, String acPoolFilter) {
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

    private Element processSubmissionPerm(HttpServletRequest request) {
        LOGGER.debug("Processing Mapping submission.");

        Map<String, String[]> parameterMap = request.getParameterMap();
        Iterator<String> iter = parameterMap.keySet().iterator();

        LinkedList<MCRRuleMapping> updateAccess = new LinkedList<MCRRuleMapping>();
        LinkedList<MCRRuleMapping> deleteAccess = new LinkedList<MCRRuleMapping>();

        final String change = "changed$";
        final String delete = "deleted$";

        while (iter.hasNext()) {
            // key should be in the form changed$_RID$ObjId$AcPool
            String key = iter.next().trim();

            LOGGER.debug("Param key: " + key);

            if (key.startsWith(change)) {
                LOGGER.debug("RID changed: " + key);

                MCRRuleMapping ruleMapping = extractRuleMapping(parameterMap, change, key);

                if (ruleMapping == null) {
                    LOGGER.debug("ruleMapping NULL!");
                }

                updateAccess.add(ruleMapping);
            }

            if (key.startsWith(delete)) {
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

    private MCRRuleMapping extractRuleMapping(Map<String, String[]> parameterMap, String action, String key) {
        StringTokenizer token = new StringTokenizer(key.substring(action.length() - 1), "$");
        String objId = token.nextToken();
        String acPool = token.nextToken();
        String ruleId = parameterMap.get(key)[0];

        MCRRuleMapping ruleMapping = XMLProcessing.createRuleMapping(ruleId, acPool, objId);
        LOGGER.debug("ObjId: " + ruleMapping.getObjId());
        LOGGER.debug("AcPool: " + ruleMapping.getPool());
        LOGGER.debug("RuleId: " + ruleMapping.getRuleId());
        return ruleMapping;
    }

    private Element setFilter(HttpServletRequest request) {
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

    // End Mapping stuff

    /***************************************************************************
     * Rule stuff
     **************************************************************************/

    private Element createNewRule(HttpServletRequest request) {
        MCRACCESSRULE accessRule = new MCRACCESSRULE();
        MCRAccessInterface AI = MCRAccessControlSystem.instance();
        String rule = request.getParameter("newRule").trim();
        String desc = request.getParameter("newRuleDesc");

        if (rule.startsWith("<"))
            rule = ruleFromXML(rule);

        accessRule.setRule(rule);
        accessRule.setDescription(desc);

        AI.createRule(accessRule.getRule(), "ACL-Editor", accessRule.getDescription());

        LOGGER.debug("Rule: " + rule);
        LOGGER.debug("Desc: " + desc);
        return getRuleEditor(request);
    }

    private String ruleFromXML(String rule) {
        SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Reader stringReader = new StringReader(rule);
        Document jdomDocument = null;

        try {
            jdomDocument = saxBuilder.build(stringReader);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MCRAccessInterface AI = MCRAccessControlSystem.instance();

        return AI.getNormalizedRuleString(jdomDocument.getRootElement());
    }

    private Element getRuleAsItems(HttpServletRequest request) {
        Element elem = XMLProcessing.ruleSet2Items(HIBA.getAccessRule());
        return elem;
    }

    private Element processSubmissionRule(HttpServletRequest request) {
        LOGGER.debug("Processing Rule submission.");

        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keySet = parameterMap.keySet();
        Iterator<String> iter = keySet.iterator();

        LinkedList<MCRACCESSRULE> updateRule = new LinkedList<MCRACCESSRULE>();
        LinkedList<MCRACCESSRULE> deleteRule = new LinkedList<MCRACCESSRULE>();

        final String change = "changed$";
        final String delete = "deleted$";

        String ridOld = "";
        String ridNew = "";

        while (iter.hasNext()) {
            String key = iter.next().trim();
            MCRACCESSRULE ruleMapping = new MCRACCESSRULE();

            if (key.contains(change) || key.contains(delete)) {
                LOGGER.debug("Param key: " + key);
                
                ridNew = new String(key.substring(key.lastIndexOf("$") + 1, key.length()));
                LOGGER.debug("new: " + ridNew + " - old: " + ridOld);

                if (!ridNew.equals(ridOld)) {
                    ruleMapping = extractAccessRule(parameterMap, change, key, ridNew);

                    if (key.startsWith(change)) {
                        LOGGER.debug("Rule changed: " + key);
                        updateRule.add(ruleMapping);
                    } else if (key.startsWith(delete)) {
                        LOGGER.debug("RID deleted: " + key);
                        deleteRule.add(ruleMapping);
                    }
                }

                ridOld = new String(ridNew);
            }
        }

        HashMap<String, LinkedList<MCRACCESSRULE>> diffMap = new HashMap<String, LinkedList<MCRACCESSRULE>>();
        diffMap.put("update", updateRule);
        diffMap.put("delete", deleteRule);

        HIBA.saveRuleChanges(diffMap);

        return getRuleEditor(request);
    }

    private MCRACCESSRULE extractAccessRule(Map<String, String[]> parameterMap, String action, String key, String rid) {
        final String RULE = "Rule$";
        final String DESC = "RuleDesc$";

        MCRACCESSRULE accessrule = new MCRACCESSRULE();

        String ruleString = null;
        String ruleDesc = null;

        // Checking which case occur
        // | rule | description
        // --------+----------+-----------------
        // changed | yes | no
        // | no | yes
        // | yes | yes
        // 
        if (key.contains(RULE)) {
            ruleString = parameterMap.get(key)[0];

            if (parameterMap.get(DESC + rid) != null)
                ruleDesc = parameterMap.get(DESC + rid)[0];
            else if (parameterMap.get(action + DESC + rid) != null)
                ruleDesc = parameterMap.get(action + DESC + rid)[0];
            else
                ruleDesc = "";

        } else if (key.contains(DESC)) {
            ruleDesc = parameterMap.get(key)[0];

            if (parameterMap.get(RULE + rid) != null)
                ruleString = parameterMap.get(RULE + rid)[0];
            else if (parameterMap.get(action + RULE + rid) != null)
                ruleString = parameterMap.get(action + RULE + rid)[0];
            else
                ruleString = "";

        } else
            LOGGER.debug("Wrong key: " + key);

        accessrule.setRid(rid);
        accessrule.setRule(ruleString);
        accessrule.setDescription(ruleDesc);

        return accessrule;
    }
    // End Rule stuff

}
