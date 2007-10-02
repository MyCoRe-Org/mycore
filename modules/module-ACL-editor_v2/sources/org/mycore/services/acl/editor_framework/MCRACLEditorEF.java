package org.mycore.services.acl.editor_framework;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.services.acl.MCRACLEditor;

public class MCRACLEditorEF extends MCRACLEditor {
    private static Logger LOGGER = Logger.getLogger(MCRACLEditorEF.class);

    @Override
    public void getEmbPermEditor(MCRServletJob job) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getEmbRuleEditor(MCRServletJob job) {
        // TODO Auto-generated method stub

    }

    @Override
    public void getPermEditor(MCRServletJob job) {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        String permMode = request.getParameter("permMode");

        if (permMode.equals("xmlData"))
            getPermXMLData(request, response);
        else if (permMode.equals("layout"))
            getPermLayout(request, response);
        else
            errorMSG(job);
    }

    private void errorMSG(MCRServletJob job) {
        String errorMsg = "The request did not contain a valid mode for this ACL Editor!";

        try {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    private void getPermLayout(HttpServletRequest request, HttpServletResponse response) {
        String uri = "webapp:modules/module-ACL-editor/web/editor/editor-ACL_start.xml";
        String redirectURL = "servlets/MCRACLEditorServlet_v2?mode=openPermissionEditor";

        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");

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
//            redirectURL = buildRedirectURL(redirectURL, parameters);
            LOGGER.debug("Redirect to: " + redirectURL);
        }
//        redirect(response, redirectURL);

    }
    

    private void getPermXMLData(HttpServletRequest request, HttpServletResponse response) {
        String objidFilter = request.getParameter("objid");
        String acpoolFilter = request.getParameter("acpool");

        MCRACLXMLProcessing XMLProcessing = new MCRACLXMLProcessing();
        MCRACLHIBAccess HIBA = new MCRACLHIBAccess();

        Element permData = XMLProcessing.access2XML(HIBA.getAccessPermission(objidFilter, acpoolFilter));

        dolayout(request, response, permData, "xml");
    }

    private void dolayout(HttpServletRequest request, HttpServletResponse response, Element elem, String outType) {
        doLayout(request, response, new Document(elem), outType);
    }

    private void doLayout(HttpServletRequest request, HttpServletResponse response, Document document, String outType) {
        try {
            if (outType.equals("xml"))
                layoutService.sendXML(request, response, document);
            else
                layoutService.doLayout(request, response, document);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void getRuleEditor(MCRServletJob job) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processingInput(MCRServletJob job) {
        // TODO Auto-generated method stub

    }

}
