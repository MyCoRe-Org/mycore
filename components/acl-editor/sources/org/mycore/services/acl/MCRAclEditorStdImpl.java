package org.mycore.services.acl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;

import com.ibm.icu.util.StringTokenizer;

public class MCRAclEditorStdImpl extends MCRAclEditor {
    MCRACLHIBAccess HIBA = new MCRACLHIBAccess();

    MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

    /***************************************************************************
     * Implementing abstract methods
     **************************************************************************/
    @Override
    public Element getACLEditor(HttpServletRequest request) {
        LOGGER.debug("Request String: " + request.getQueryString());
        Element aclEditor = ACLEditor();
        String type = request.getParameter("editor");
        String cmd = request.getParameter("cmd");

        String objIdFilter = request.getParameter("objid");
        String acPoolFilter = request.getParameter("acpool");

        String redirectURL = request.getParameter("redir");

        LOGGER.debug("Redirect: " + redirectURL);

        if (type == null)
            type = "permEditor";

        if (cmd != null)
            aclEditor.addContent(editorCmd(cmd));

        if (redirectURL != null && !redirectURL.equals(""))
            aclEditor.addContent(redirect(redirectURL));

        aclEditor.addContent(editorType(type));
        aclEditor.addContent(getFilterElem(objIdFilter, acPoolFilter));

        return aclEditor;
    }

    @Override
    public Element dataRequest(HttpServletRequest request) {
        LOGGER.debug("Handling data request.");
        LOGGER.debug("Query String: " + request.getQueryString());

        String action = request.getParameter("action");
        Element elem = null;

        if (action.equals("setFilter"))
            elem = setFilter(request);
        else if (action.equals("getPermEditor"))
            elem = getPermEditor(request);
        else if (action.equals("getRuleEditor"))
            elem = getRuleEditor(request);
        else if (action.equals("deleteFilter"))
            elem = getACLEditor(request);
        else if (action.equals("createNewPerm"))
            elem = createNewPerm(request);
        else if (action.equals("createNewRule"))
            elem = createNewRule(request);
        else if (action.equals("getRuleAsItems"))
            elem = getRuleAsItems(request);
        else if (action.equals("submitPerm"))
            elem = processPermSubmission(request);
        else if (action.equals("submitRule"))
            elem = processRuleSubmission(request);
        else if (action.equals("delAllRules"))
            elem = deleteAllRules(request);
        else if (action.equals("delAllPerms"))
            elem = deleteAllPerms(request);

        return elem;
    }

    // End implementing abstract methods

    /***************************************************************************
     * Mapping stuff
     **************************************************************************/

    private Element getPermEditor(HttpServletRequest request) {
        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");
        String embedded = request.getParameter("emb");
        String cmd = request.getParameter("cmd");

        String redirectURL = request.getParameter("redir");

        LOGGER.debug("Redirect: " + redirectURL);
        LOGGER.debug("ObjId: " + objidFilter);
        LOGGER.debug("AcPool: " + acpoolFilter);

        Element permEditor = getPermission(objidFilter, acpoolFilter);

        if (redirectURL != null && !redirectURL.equals(""))
            permEditor.addContent(redirect(redirectURL));

        if (embedded != null) {
            permEditor.setAttribute("emb", "true");
        }

        if (cmd != null) {
            permEditor.setAttribute("cmd", cmd);
        }

        return permEditor;
    }

    private Element getPermission(String objIdFilter, String acPoolFilter) {
        Element elem = XMLProcessing.access2XML(HIBA.getAccessPermission(objIdFilter, acPoolFilter), true);
        elem.addContent(getFilterElem(objIdFilter, acPoolFilter));

        return elem;
    }

    private Element getFilterElem(String objIdFilter, String acPoolFilter) {
        Element elem = XMLProcessing.accessFilter2XML(objIdFilter, acPoolFilter);
        return elem;
    }

    private Element createNewPerm(HttpServletRequest request) {
        String objId = request.getParameter("newPermOBJID");
        String acPool = request.getParameter("newPermACPOOL");
        String ruleId = request.getParameter("newPermRID");
        String uid = MCRServlet.getProperty(request, "uid");

        LOGGER.debug("ObjId: " + objId);
        LOGGER.debug("AcPool: " + acPool);
        LOGGER.debug("RuleId: " + ruleId);

        MCRRuleMapping perm = XMLProcessing.createRuleMapping(ruleId, acPool, objId);
        MCRAccessStore.getInstance().createAccessDefinition(perm);

        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("permEditor"));

        return editor;
    }

    private Element processPermSubmission(HttpServletRequest request) {
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

        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("permEditor"));

        return editor;
    }

    private Element deleteAllPerms(HttpServletRequest request) {
        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");
        List<MCRACCESS> accessList = HIBA.getAccessPermission(objidFilter, acpoolFilter);
        HashMap<String, LinkedList<MCRRuleMapping>> diffMap = new HashMap<String, LinkedList<MCRRuleMapping>>();
        LinkedList<MCRRuleMapping> deleteAccess = new LinkedList<MCRRuleMapping>();
        
        for (Iterator iter = accessList.iterator(); iter.hasNext();) {
            MCRACCESS currentAcc = (MCRACCESS) iter.next();
            String rid = currentAcc.getRule().getRid();
            String acpool = currentAcc.getKey().getAcpool();
            String objid = currentAcc.getKey().getObjid();
            
            

            MCRRuleMapping ruleMapping = XMLProcessing.createRuleMapping(rid, acpool, objid);
            deleteAccess.add(ruleMapping);
        }
        diffMap.put("delete", deleteAccess);

        HIBA.savePermChanges(diffMap);

        String redirectURL = request.getParameter("redir");
        LOGGER.debug("Redirect URL: " + redirectURL);

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else {
            editor = ACLEditor().addContent(editorType("permEditor"));
            editor.addContent(getFilterElem(objidFilter, acpoolFilter));
        }

        return editor;
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

        Element editor = ACLEditor();
        editor.addContent(editorType("permEditor"));
        editor.addContent(getFilterElem(objIdFilter, acPoolFilter));
        return editor;
    }

    // End Mapping stuff

    /***************************************************************************
     * Rule stuff
     **************************************************************************/

    private Element getRuleEditor(HttpServletRequest request) {
        Element elem = XMLProcessing.ruleSet2XML(HIBA.getAccessRule());
        return elem;
    }

    private Element createNewRule(HttpServletRequest request) {
        MCRACCESSRULE accessRule = new MCRACCESSRULE();
        MCRAccessInterface AI = MCRAccessControlSystem.instance();
        String rule = request.getParameter("newRule").trim();
        String desc = request.getParameter("newRuleDesc");
        String uid = MCRSessionMgr.getCurrentSession().getCurrentUserID();

        if (rule.startsWith("<"))
            rule = ruleFromXML(rule);

        accessRule.setRule(rule);
        accessRule.setDescription(desc);
        
        LOGGER.debug("User ID: " + uid);

        AI.createRule(accessRule.getRule(), uid, accessRule.getDescription());

        LOGGER.debug("Rule: " + rule);
        LOGGER.debug("Desc: " + desc);

        String redirectURL = request.getParameter("redir");
        LOGGER.debug("Redirect URL: " + redirectURL);

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));
        
        return editor;
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

    private Element processRuleSubmission(HttpServletRequest request) {
        LOGGER.debug("Processing Rule submission.");
        
        String uid = MCRServlet.getProperty(request, "uid");
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keySet = parameterMap.keySet();
        Iterator<String> iter = keySet.iterator();

        LinkedList<MCRACCESSRULE> updateRule = new LinkedList<MCRACCESSRULE>();
        LinkedList<String> deleteRule = new LinkedList<String>();

        final String change = "changed$";
        final String delete = "deleted$";

        String ridOld = "";
        String currentRid = "";

        while (iter.hasNext()) {
            String key = iter.next().trim();
            MCRACCESSRULE ruleMapping = new MCRACCESSRULE();

            if (key.contains(change) || key.contains(delete)) {
                LOGGER.debug("Param key: " + key);

                currentRid = new String(key.substring(key.lastIndexOf("$") + 1, key.length()));

                if (!currentRid.equals(ridOld)) {
                    if (key.startsWith(change)) {
                        ruleMapping = extractAccessRule(parameterMap, change, key, currentRid);
                        LOGGER.debug("Rule changed: " + key);
                        updateRule.add(ruleMapping);
                    } else if (key.startsWith(delete)) {
                        LOGGER.debug("Delete Rule: " + key);
                        deleteRule.add(currentRid);
                    }
                }

                ridOld = new String(currentRid);
            }
        }

        HashMap diffMap = new HashMap();
        diffMap.put("update", updateRule);
        diffMap.put("delete", deleteRule);

        HIBA.saveRuleChanges(diffMap);
        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));

        return editor;
    }

    private Element deleteAllRules(HttpServletRequest request) {
        LOGGER.debug("Delete all rules.");

        String uid = MCRServlet.getProperty(request, "uid");
        HashMap diffMap = new HashMap();

        List<MCRACCESSRULE> ruleList = HIBA.getAccessRule();
        LinkedList<String> deleteRule = new LinkedList<String>();

        for (Iterator iter = ruleList.iterator(); iter.hasNext();) {
            MCRACCESSRULE rule = (MCRACCESSRULE) iter.next();
            String currentRid = rule.getRid();
            LOGGER.debug("Delete: " + currentRid);
            deleteRule.add(currentRid);
        }
        diffMap.put("delete", deleteRule);

        HIBA.saveRuleChanges(diffMap);
        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));

        return editor;
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

    public Element ACLEditor() {
        Element element = new Element("mcr_acl_editor");
        return element;
    }

    private Content editorType(String type) {
        Element editorType = new Element("editor");
        editorType.addContent(type);
        return editorType;
    }

    private Content editorCmd(String cmd) {
        Element editorType = new Element("cmd");
        editorType.addContent(cmd);
        return editorType;
    }

    private Element redirect(String url) {
        Element redirect = new Element("redirect");
        redirect.addContent(url);
        return redirect;
    }

}
