package org.mycore.services.acl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRACLEditorServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRACLEditorServlet.class);

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String mode = request.getParameter("mode");

        MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
        MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

        String objidFilter = null;
        String acpoolFilter = null;
        String layout = "html";

        boolean doLayout = false;
        Element answer = null;

        String errorMsg = "The request did not contain a valid mode for this servlet!";
        objidFilter = request.getParameter("objid");
        acpoolFilter = request.getParameter("acpool");
        
        if (mode == null) {
            LOGGER.debug("processSubmission");
            processSubmission(job, response, objidFilter, acpoolFilter);
        } else if (mode.equals("getACLPermissions")) {
            // retrieve the permission from DB, maybe with some filter
            // create the xml
            LOGGER.debug("getACLPermissions");
            answer = XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter));

            doLayout = true;
        } else if (mode.equals("ACLPermissionsEditor")) {
            callPermissionEditor(request, response);
        } else if (mode.equals("access")) {
            // still in use should be removed in future
            answer = XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter));
            answer.addContent(XMLProcessing.ruleSet2Items(HIBA.getAccessRule()));
            doLayout = true;
        } else if (mode.equals("getRuleAsItems")) {
            // rules in item form for dropdown list in permission editor
            answer = XMLProcessing.ruleSet2Items(HIBA.getAccessRule());
            doLayout = true;
        } else if (mode.equals("getRuleTable")) {
            // rule as xml for the rule editor
            List ruleList = null;

            if (request.getParameter("newRule") == null) {
                ruleList = HIBA.getAccessRule();
            }

            answer = XMLProcessing.ruleSet2XML(ruleList);
            doLayout = true;
        } else if (mode.equals("getFilter")) {
            // the filter as xml for filter editor
            LOGGER.info("get Filter!");
            objidFilter = request.getParameter("objid");
            acpoolFilter = request.getParameter("acpool");
            answer = XMLProcessing.accessFilter2XML(objidFilter, acpoolFilter);
            doLayout = true;
        } else if (mode.equals("removeFilter")) {
            // "remove" the filter
            String redirectURL = "modules/module-ACL-editor/web/editor/editor-ACL_start.xml";
            LOGGER.debug("Redirect to URL " + redirectURL);
            redirect(response, redirectURL);
        } else {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
        }

        if (doLayout)
            doLayout(request, response, answer, layout);

    }

    private void callPermissionEditor(HttpServletRequest request, HttpServletResponse response) {
        LOGGER.debug("ACLPermissionsEditor");
        String redirectURL = "modules/module-ACL-editor/web/editor/editor-ACL_start.xml";

        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");

        redirect(response, redirectURL, objidFilter, acpoolFilter);
    }

    private void redirect(HttpServletResponse response, String redirectURL, String objidFilter, String acpoolFilter) {
        Properties parameters = new Properties();
        boolean addParam = false;

        if (objidFilter != null) {
            parameters.put("objid", objidFilter);
            addParam = true;
        }

        if (acpoolFilter != null) {
            parameters.put("acpool", acpoolFilter);
            addParam = true;
        }

        if (addParam) {
            redirectURL = buildRedirectURL(redirectURL, parameters);
            LOGGER.debug("Redirect to: " + redirectURL);
        }
        redirect(response, redirectURL);
    }

    /**
     * processing incomming data from editor
     * 
     * @param job
     * @param response
     * @param objidFilter
     * @param acpoolFilter
     * @throws Exception
     */
    private void processSubmission(MCRServletJob job, HttpServletResponse response, String objidFilter, String acpoolFilter) throws Exception {
        LOGGER.debug("Process submission!");
        MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
        MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();
        Document indoc = getSubmissionDoc(job);

        Element indocRoot = indoc.getRootElement();
        String indocRootName = indocRoot.getName();

        if (indocRootName.equals("mcr_access_filter")) {
            objidFilter = indocRoot.getChildText("objid");
            acpoolFilter = indocRoot.getChildText("acpool");
            String redirectURL = "modules/module-ACL-editor/web/editor/editor-ACL_start.xml";
            
            redirect(response, redirectURL, objidFilter, acpoolFilter);
        } else if (indocRootName.equals("mcr_access_set")) {
            LOGGER.debug("Process Submission - Filter:");
            LOGGER.debug("objid Filter: " + objidFilter);
            LOGGER.debug("acpool Filter: " + acpoolFilter);
            Document origDoc = new Document(XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter)));
            Map diffMap = XMLProcessing.findAccessDiff(indoc, origDoc);

            HIBA.savePermChanges(diffMap);
            redirect(response, "modules/module-ACL-editor/web/editor/editor-ACL_start.xml");

        } else if (indocRootName.equals("mcr_access_rule_set")) {
            Document origDoc = new Document(XMLProcessing.ruleSet2XML(HIBA.getAccessRule()));
            Map diffMap = XMLProcessing.findRulesDiff(indoc, origDoc);

            HIBA.saveRuleChanges(diffMap);
            redirect(response, "modules/module-ACL-editor/web/editor/editor-ACL_Rules.xml");

        }
    }

    private Document getSubmissionDoc(MCRServletJob job) {
        MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
        return sub.getXML();
    }

    private void redirect(HttpServletResponse response, String url) {
        if (url == null)
            url = "";

        try {
            response.sendRedirect(response.encodeRedirectURL(getBaseURL() + url));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void doLayout(HttpServletRequest request, HttpServletResponse response, Element elem, String format) throws IOException {
        doLayout(request, response, new Document(elem), format);
    }

    private void doLayout(HttpServletRequest request, HttpServletResponse response, Document doc, String format) throws IOException {

        if (format.equals("xml"))
            getLayoutService().sendXML(request, response, doc);
        else
            getLayoutService().doLayout(request, response, doc);
    }
}
