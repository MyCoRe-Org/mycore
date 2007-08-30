package org.mycore.services.acl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRACLEditorServlet extends MCRServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRACLEditorServlet.class);

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String mode = request.getParameter("mode");

        MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
        MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();

        String objidFilter = null;
        String acpoolFilter = null;

        LOGGER.info("******* Current Filter: obj: " + objidFilter + "\t ac: " + acpoolFilter);

        System.out.println("#################################################");
        System.out.println("# version 0.041                                 #");
        System.out.println("#################################################");

        boolean doLayout = false;
        Element answer = null;

        String errorMsg = "The request did not contain a valid mode for this servlet!";

        if (mode == null) {
            LOGGER.debug("processSubmission");
            processSubmission(job, response, objidFilter, acpoolFilter);
        } else if (mode.equals("getACLPermissions")) {
            LOGGER.debug("getACLPermissions");
            String addNew = request.getParameter("addNew");
            objidFilter = request.getParameter("objid");
            acpoolFilter = request.getParameter("acpool");
            answer = XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter));
           
            doLayout = true;
        } else if (mode.equals("ACLPermissionsEditor")) {
            LOGGER.debug("ACLPermissionsEditor");
            answer = new Element("ACLPermissionsEditor");
            doLayout = true;
            // redirect(response,
            // "modules/module-ACL-editor/web/editor/editor-ACL_Permissions.xml?source_mode=getACLPermissions");
        } else if (mode.equals("access")) {
            answer = XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter));
            answer.addContent(XMLProcessing.ruleSet2Items(HIBA.getAccessRule()));
            doLayout = true;
        } else if (mode.equals("getRuleAsItems")) {
            answer = XMLProcessing.ruleSet2Items(HIBA.getAccessRule());
            doLayout = true;
        } else if (mode.equals("getRuleTable")) {
            List ruleList = null;

            if (request.getParameter("newRule") == null) {
                ruleList = HIBA.getAccessRule();
            }

            answer = XMLProcessing.ruleSet2XML(ruleList);
            doLayout = true;
        } else if (mode.equals("getFilter")) {
            LOGGER.info("******* get Filter!");
            objidFilter = request.getParameter("objid");
            acpoolFilter = request.getParameter("acpool");
            answer = XMLProcessing.accessFilter2XML(objidFilter, acpoolFilter);
            doLayout = true;
        } else if (mode.equals("removeFilter")) {
            String redirectURL = "modules/module-ACL-editor/web/editor/editor-ACL_start.xml";
            LOGGER.debug("Redirect to URL " + redirectURL);
            redirect(response, redirectURL);
        } else {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
        }

        if (doLayout)
            doLayout(request, response, answer, "html");

    }

    private void processSubmission(MCRServletJob job, HttpServletResponse response, String objidFilter, String acpoolFilter) throws Exception {
        LOGGER.debug("Process submission!");
        MCRACLHIBAccess HIBA = new MCRACLHIBAccess();
        MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();
        Document indoc = getSubmissionDoc(job); // sub.getXML();

        Element indocRoot = indoc.getRootElement();
        String indocRootName = indocRoot.getName();

        if (indocRootName.equals("mcr_access_filter")) {
            /*
             * Here we are a little bit inconsistent. Normally
             * MCRACLXMLProcessing should take care of all the xml stuff, but
             * here we have to move to much data around for nothing and blow up
             * the code
             */
            objidFilter = indocRoot.getChildText(ACLConstants.objidFilter);
            acpoolFilter = indocRoot.getChildText(ACLConstants.acpoolFilter);
            setFilter(objidFilter, acpoolFilter);
            String redirectURL = "modules/module-ACL-editor/web/editor/editor-ACL_start.xml";
            redirectURL = appendFilter(redirectURL, objidFilter, acpoolFilter);
            LOGGER.debug("Redirect to URL " + redirectURL);
            redirect(response, redirectURL);
        } else if (indocRootName.equals("mcr_access_set")) {
            Document origDoc = new Document(XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter)));
            Map diffMap = XMLProcessing.findAccessDiff(indoc, origDoc);

            HIBA.savePermChanges(diffMap);
            redirect(response, "modules/module-ACL-editor/web/editor/editor-ACL_start.xml");

        } else if (indocRootName.equals("mcr_access_rule_set")) {
            Document origDoc = new Document(XMLProcessing.ruleSet2XML(HIBA.getAccessRule()));
            Map diffMap = XMLProcessing.findRulesDiff(indoc, origDoc);

            HIBA.saveRuleChanges(diffMap);
            redirect(response, "modules/module-ACL-editor/web/editor/editor-access_rule.xml");

        }
    }

    private String appendFilter(String URL, String objid, String acpool) {
        String sign = "";

        if (URL.contains("?"))
            sign = "&";
        else
            sign = "?";

        if (objid != null) {
            URL = URL + sign + "objid=" + objid;
            sign = "&";
        }

        if (acpool != null)
            URL = URL + sign + "acpool=" + acpool;

        return URL;
    }

    private Document getSubmissionDoc(MCRServletJob job) {
        MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
        return sub.getXML();
    }

    private String getFiler(MCRSession session, String filter) {
        Map filterMap = (Map) session.get(ACLConstants.filterFromSession);

        if (filterMap != null) {
            filter = (String) filterMap.get(filter);
        } else {
            filter = null;
            LOGGER.debug("******** Filter NULL *******");
        }

        return filter;
    }

    private void setFilter(Document indoc) {
        Element indocRoot = indoc.getRootElement();
        String indocRootName = indocRoot.getName();
        if (indocRootName.equals("mcr_access_filter")) {
            String objidFilter = indocRoot.getChildText(ACLConstants.objidFilter);
            String acpoolFilter = indocRoot.getChildText(ACLConstants.acpoolFilter);
            setFilter(objidFilter, acpoolFilter);
        } else
            LOGGER.debug("Wrong filter document, filter not set!");
    }

    private void setFilter(HttpServletRequest request, HttpServletResponse response) {
        String newObjIDFilter = request.getParameter(ACLConstants.objidFilter);
        String newAcPoolFilter = request.getParameter(ACLConstants.acpoolFilter);

        setFilter(newObjIDFilter, newAcPoolFilter);
    }

    private void setFilter(String newObjIDFilter, String newAcPoolFilter) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Map filterMap = (Map) session.get(ACLConstants.filterFromSession);
        boolean oldFilterExist = false;

        if (filterMap == null)
            filterMap = new HashMap();
        else
            oldFilterExist = true;

        String oldObjIDFilter = (String) filterMap.get(ACLConstants.objidFilter);
        String oldAcPoolFilter = (String) filterMap.get(ACLConstants.acpoolFilter);

        /*
         * Here we check what happen to the objid filter depending on its old
         * and new value.
         * 
         * There are two cases where the filter have to be removed: 1)
         * newObjIDFilter != null && newObjIDFilter.equals("") 2) newObjIDFilter ==
         * null && oldObjIDFilter != null
         * 
         * so the order of the if test is important to keep the code small. When
         * oldObjIDFilter == null, the rest is skipped regardless of the value
         * of the or test. The newObjIDFilter.equals("") ist just evualate if
         * newObjIDFilter != null so no NullPointer exception is expected.
         * 
         * When the if test is true so we have to remove the filter value in the
         * session as well the value in filterMap.
         * 
         * When we reach the second if test we have to check the following
         * cases: 1) newObjIDFilter != null && oldObjIDFilter == null 2)
         * newObjIDFilter != null && oldObjIDFilter != null
         * 
         * In both cases we have to set the new filter regardless of the value
         * of oldObjIDFilter.
         * 
         * When newObjIDFilter == null && oldObjIDFilter == null, nothing to do
         */
        if (oldObjIDFilter != null && (newObjIDFilter == null || newObjIDFilter.equals(""))) {
            session.deleteObject(ACLConstants.objidFilter);
            filterMap.remove(ACLConstants.objidFilter);

            LOGGER.info("******* ObjID Filter removed!");
        } else if (newObjIDFilter != null) {
            filterMap.put(ACLConstants.objidFilter, newObjIDFilter);
            LOGGER.info("******* new ObjID Filter: " + newObjIDFilter);
        }

        /*
         * see objid filter
         */
        if (oldAcPoolFilter != null && (newAcPoolFilter == null || newAcPoolFilter.equals(""))) {
            session.deleteObject(ACLConstants.acpoolFilter);
            filterMap.remove(ACLConstants.acpoolFilter);

            LOGGER.info("******* AcPool Filter removed!");
        } else if (newAcPoolFilter != null) {
            filterMap.put(ACLConstants.acpoolFilter, newAcPoolFilter);
            LOGGER.info("******* new AcPool Filter: " + newAcPoolFilter);
        }

        if (filterMap.size() == 0 && oldFilterExist) {
            session.deleteObject(ACLConstants.filterFromSession);
            LOGGER.info("******* Filter completely removed!");
        } else {
            session.put(ACLConstants.filterFromSession, filterMap);
            LOGGER.info("******* Filter updated!");
        }
    }

    private void removeFilter(HttpServletRequest request, HttpServletResponse response) {
        MCRSession session = MCRSessionMgr.getCurrentSession();

        if (session.get(ACLConstants.filterFromSession) != null)
            session.deleteObject(ACLConstants.filterFromSession);
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

    private class ACLConstants {
        static final String filterFromSession = "filter";
        static final String objidFilter = "objid";
        static final String acpoolFilter = "acpool";
    }

}
