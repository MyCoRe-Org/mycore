package org.mycore.services.acl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRStringContent;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.acl.filter.MCRAclObjIdFilter;
import org.mycore.services.acl.filter.MCRAclPermissionFilter;
import org.xml.sax.SAXException;

import com.ibm.icu.util.StringTokenizer;

public class MCRAclEditorStdImpl extends MCRAclEditor {

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

        Properties requestProperties = getRequestProperties(request);

        if (action.equals("setFilter"))
            elem = setFilter(request);
        else if (action.equals("getPermEditor"))
            elem = getPermEditor(requestProperties);
        else if (action.equals("getRuleEditor"))
            elem = getRuleEditor(requestProperties);
        else if (action.equals("deleteFilter"))
            elem = getACLEditor(request);
        else if (action.equals("createNewPerm"))
            elem = createNewPerm(request);
        else if (action.equals("createNewRule")) {
            try {
                elem = createNewRule(request);
            } catch (JDOMException | IOException | SAXException e) {
                throw new MCRException(e);
            }
        } else if (action.equals("getRuleAsItems"))
            elem = getRuleAsItems(requestProperties);
        else if (action.equals("submitPerm"))
            elem = processPermSubmission(request);
        else if (action.equals("submitRule"))
            elem = processRuleSubmission(request);
        else if (action.equals("delAllRules"))
            elem = deleteAllRules(request, requestProperties);
        else if (action.equals("delAllPerms"))
            elem = deleteAllRuleMappings(request, requestProperties);

        return elem;
    }

    protected Properties getRequestProperties(HttpServletRequest request) {
        Properties p = new Properties();
        @SuppressWarnings("unchecked")
        Enumeration<String> en = request.getAttributeNames();
        while (en.hasMoreElements()) {
            String attrKey = en.nextElement();
            p.put(attrKey, request.getAttribute(attrKey));
        }
        @SuppressWarnings("unchecked")
        Enumeration<String> en2 = request.getParameterNames();
        while (en2.hasMoreElements()) {
            String attrKey = en2.nextElement();
            p.put(attrKey, request.getParameter(attrKey));
        }
        return p;
    }

    // End implementing abstract methods

    /***************************************************************************
     * Mapping stuff
     **************************************************************************/

    public static Element getPermEditor(Properties properties) {
        String objidFilter = properties.getProperty(MCRAclObjIdFilter.PROPERTY_NAME);
        String acpoolFilter = properties.getProperty(MCRAclPermissionFilter.PROPERTY_NAME);
        String embedded = properties.getProperty("emb");
        String cmd = properties.getProperty("cmd");

        String redirectURL = properties.getProperty("redir");

        LOGGER.debug("Redirect: " + redirectURL);
        LOGGER.debug("ObjId: " + objidFilter);
        LOGGER.debug("AcPool: " + acpoolFilter);

        Element permEditor = getPermission(objidFilter, acpoolFilter, properties);

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

    private static Element getPermission(String objIdFilter, String acPoolFilter, Properties filterProperties) {
        Element elem = MCRACLXMLProcessing.access2XML(MCRACLHIBAccess.getRuleMappingList(filterProperties), true);
        elem.addContent(getFilterElem(objIdFilter, acPoolFilter));
        return elem;
    }

    private static Element getFilterElem(String objIdFilter, String acPoolFilter) {
        Element elem = MCRACLXMLProcessing.accessFilter2XML(objIdFilter, acPoolFilter);
        return elem;
    }

    private Element createNewPerm(HttpServletRequest request) {
        String objId = "";
        try {
            objId = URLDecoder.decode(request.getParameter("newPermOBJID"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }
        String acPool = request.getParameter("newPermACPOOL");
        String ruleId = request.getParameter("newPermRID");
        String creator = MCRServlet.getProperty(request, "creator");

        LOGGER.debug("ObjId: " + objId);
        LOGGER.debug("AcPool: " + acPool);
        LOGGER.debug("RuleId: " + ruleId);

        MCRRuleMapping perm = createRuleMapping(acPool, objId, ruleId, creator);
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

        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterMap = request.getParameterMap();
        Iterator<String> iter = parameterMap.keySet().iterator();

        LinkedList<MCRRuleMapping> updateAccessList = new LinkedList<MCRRuleMapping>();
        LinkedList<MCRRuleMapping> deleteAccessList = new LinkedList<MCRRuleMapping>();

        final String change = "changed$";
        final String delete = "deleted$";

        while (iter.hasNext()) {
            // key should be in the form changed$_RID$ObjId$AcPool%creator
            String key = iter.next().trim();
            LOGGER.debug("Param key: " + key);

            if (key.startsWith(change)) {
                LOGGER.debug("RID changed: " + key);
                MCRRuleMapping ruleMapping = extractRuleMapping(parameterMap, change, key);
                if (ruleMapping == null)
                    LOGGER.debug("ruleMapping NULL!");
                updateAccessList.add(ruleMapping);
            }

            if (key.startsWith(delete)) {
                LOGGER.debug("RID deleted: " + key);
                MCRRuleMapping ruleMapping = extractRuleMapping(parameterMap, delete, key);
                deleteAccessList.add(ruleMapping);
            }
        }

        HashMap<MCRAclAction, List<MCRRuleMapping>> diffMap = new HashMap<MCRAclAction, List<MCRRuleMapping>>();
        diffMap.put(MCRAclAction.update, updateAccessList);
        diffMap.put(MCRAclAction.delete, deleteAccessList);
        MCRACLHIBAccess.saveRuleMappingChanges(diffMap);

        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("permEditor"));

        return editor;
    }

    private Element deleteAllRuleMappings(HttpServletRequest request, Properties filterProperties) {
        String objidFilter = request.getParameter(MCRAclObjIdFilter.PROPERTY_NAME);
        String acpoolFilter = request.getParameter(MCRAclPermissionFilter.PROPERTY_NAME);
        List<MCRACCESS> accessList = MCRACLHIBAccess.getRuleMappingList(filterProperties);

        HashMap<MCRAclAction, List<MCRRuleMapping>> diffMap = new HashMap<MCRAclAction, List<MCRRuleMapping>>();
        LinkedList<MCRRuleMapping> deleteAccessList = new LinkedList<MCRRuleMapping>();
        for (MCRACCESS currentAcc : accessList) {
            String acpool = currentAcc.getKey().getAcpool();
            String objid = currentAcc.getKey().getObjid();
            String rId = currentAcc.getRule().getRid();
            String creator = currentAcc.getRule().getCreator();
            MCRRuleMapping ruleMapping = createRuleMapping(acpool, objid, rId, creator);
            deleteAccessList.add(ruleMapping);
        }
        diffMap.put(MCRAclAction.delete, deleteAccessList);
        MCRACLHIBAccess.saveRuleMappingChanges(diffMap);

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
        String rId = parameterMap.get(key)[0];
        String objId = token.nextToken();
        String acPool = token.nextToken();
        String creator = token.nextToken();

        MCRRuleMapping ruleMapping = createRuleMapping(acPool, objId, rId, creator);
        LOGGER.debug("ObjId: " + ruleMapping.getObjId());
        LOGGER.debug("AcPool: " + ruleMapping.getPool());
        return ruleMapping;
    }

    public MCRRuleMapping createRuleMapping(String acpool, String objid, String rid, String creator) {
        MCRRuleMapping ruleMapping = new MCRRuleMapping();
        if (creator == null || creator.equals(""))
            creator = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        ruleMapping.setCreator(creator);
        ruleMapping.setCreationdate(new Date());
        ruleMapping.setPool(acpool);
        ruleMapping.setRuleId(rid);
        ruleMapping.setObjId(objid);
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

    public static Element getRuleEditor(Properties properties) {
        String notEditableCreators = properties.getProperty("notEditableCreators");
        List<String> notEditableCreatorList = new ArrayList<String>();
        if (notEditableCreators != null) {
            Collections.addAll(notEditableCreatorList, notEditableCreators.split(":"));
        }
        Element elem = MCRACLXMLProcessing.ruleSet2XML(MCRACLHIBAccess.getRuleList(properties), notEditableCreatorList);
        return elem;
    }

    private Element createNewRule(HttpServletRequest request) throws JDOMException, IOException, SAXException {
        MCRACCESSRULE accessRule = new MCRACCESSRULE();
        MCRAccessInterface AI = MCRAccessControlSystem.instance();
        String rule = MCRServlet.getProperty(request, "newRule").trim();
        String desc = MCRServlet.getProperty(request, "newRuleDesc");
        String creator = MCRServlet.getProperty(request, "creator");
        if (creator == null)
            creator = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();

        if (rule.startsWith("<"))
            rule = ruleFromXML(rule);

        accessRule.setRule(rule);
        accessRule.setDescription(desc);

        LOGGER.debug("Creator ID: " + creator);

        AI.createRule(accessRule.getRule(), creator, accessRule.getDescription());

        LOGGER.debug("Rule: " + rule);
        LOGGER.debug("Desc: " + desc);

        String redirectURL = MCRServlet.getProperty(request, "redir");
        LOGGER.debug("Redirect URL: " + redirectURL);

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));

        return editor;
    }

    private String ruleFromXML(String rule) throws JDOMException, IOException, SAXException {
        MCRStringContent content = new MCRStringContent(rule);
        Document jdomDocument = content.asXML();
        MCRAccessInterface AI = MCRAccessControlSystem.instance();
        return AI.getNormalizedRuleString(jdomDocument.getRootElement());
    }

    public static Element getRuleAsItems(Properties filterProperties) {
        Element elem = MCRACLXMLProcessing.ruleSet2Items(MCRACLHIBAccess.getRuleList(filterProperties));
        return elem;
    }

    private Element processRuleSubmission(HttpServletRequest request) {
        LOGGER.debug("Processing Rule submission.");

        @SuppressWarnings("unchecked")
        Map<String, String[]> parameterMap = request.getParameterMap();
        Set<String> keySet = parameterMap.keySet();
        Iterator<String> iter = keySet.iterator();

        LinkedList<MCRACCESSRULE> updateRule = new LinkedList<MCRACCESSRULE>();
        LinkedList<MCRACCESSRULE> deleteRule = new LinkedList<MCRACCESSRULE>();

        final String change = "changed$";
        final String delete = "deleted$";

        String ridOld = "";
        String currentRid = "";
        String creator = "";

        while (iter.hasNext()) {
            String key = iter.next().trim();
            MCRACCESSRULE ruleMapping = new MCRACCESSRULE();

            if (key.contains(change) || key.contains(delete)) {
                LOGGER.debug("Param key: " + key);

                currentRid = key.substring(key.lastIndexOf("$") + 1, key.length());
                creator = parameterMap.get(currentRid + "$CREATOR")[0];

                if (!currentRid.equals(ridOld)) {
                    ruleMapping.setRid(currentRid);
                    if (key.startsWith(change)) {
                        ruleMapping = extractAccessRule(parameterMap, change, key, currentRid, creator);
                        LOGGER.debug("Rule changed: " + key);
                        updateRule.add(ruleMapping);
                    } else if (key.startsWith(delete)) {
                        LOGGER.debug("Delete Rule: " + key);
                        deleteRule.add(ruleMapping);
                    }
                }
                ridOld = new String(currentRid);
            }
        }

        HashMap<MCRAclAction, List<MCRACCESSRULE>> diffMap = new HashMap<MCRAclAction, List<MCRACCESSRULE>>();
        diffMap.put(MCRAclAction.update, updateRule);
        diffMap.put(MCRAclAction.delete, deleteRule);

        MCRACLHIBAccess.saveRuleChanges(diffMap);
        String redirectURL = request.getParameter("redir");

        Element editor;

        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));

        return editor;
    }

    private Element deleteAllRules(HttpServletRequest request, Properties filterProperties) {
        LOGGER.debug("Delete all rules.");
        HashMap<MCRAclAction, List<MCRACCESSRULE>> diffMap = new HashMap<MCRAclAction, List<MCRACCESSRULE>>();

        List<MCRACCESSRULE> ruleList = MCRACLHIBAccess.getRuleList(filterProperties);
        diffMap.put(MCRAclAction.delete, ruleList);
        MCRACLHIBAccess.saveRuleChanges(diffMap);

        String redirectURL = request.getParameter("redir");
        Element editor;
        if (redirectURL != null && !redirectURL.equals(""))
            editor = redirect(redirectURL);
        else
            editor = ACLEditor().addContent(editorType("ruleEditor"));
        return editor;
    }

    private MCRACCESSRULE extractAccessRule(Map<String, String[]> parameterMap, String action, String key, String rid, String creator) {
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
        accessrule.setCreator(creator);

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

    private static Element redirect(String url) {
        Element redirect = new Element("redirect");
        redirect.addContent(url);
        return redirect;
    }

}
