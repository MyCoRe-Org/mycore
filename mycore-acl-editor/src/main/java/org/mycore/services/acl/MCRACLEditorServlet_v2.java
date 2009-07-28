package org.mycore.services.acl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRUserMgr;

public class MCRACLEditorServlet_v2 extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRACLEditorServlet_v2.class);

    protected static final String LOGINSERVLET_URL = "MCRLoginServlet";

    public void init() throws MCRConfigurationException, ServletException {
        super.init();
    }

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String mode = request.getParameter("mode");

        verifyAccess(job);

        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(request, response, getBaseURL()))
            return;

        LOGGER.debug("Mode: " + mode);

        String layout = "html";

        boolean mcrWebPage = false;
        Element answer = null;

        String errorMsg = "The request did not contain a valid mode for this servlet!";

        MCRAclEditor aclEditor = MCRAclEditor.instance();
        if (mode.equals("getACLEditor")) {
            answer = aclEditor.getACLEditor(request);

        }

        else if (mode.equals("dataRequest")) {
            answer = aclEditor.dataRequest(request);

        } else {
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, errorMsg);
        }

        if (answer.getName().equals("redirect")) {
            LOGGER.debug("Redirect: " + answer.getText());
            redirect(response, answer.getText());
        } else {
            LOGGER.debug("Normal doLayout!");
            doLayout(request, response, answer, layout, mcrWebPage);
        }
    }

    public void verifyAccess(MCRServletJob job) throws IOException {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-aclEditor")) {
            LOGGER.info("Access denied for userID=" + MCRUserMgr.instance().getCurrentUser().getID());
            final String queryString = (job.getRequest().getQueryString() != null) ? "?" + job.getRequest().getQueryString() : ":";
            job.getResponse().sendRedirect(
                    job.getResponse()
                            .encodeRedirectURL(getServletBaseURL() + LOGINSERVLET_URL + "?url=" + job.getRequest().getRequestURL().append(queryString)));
        }
    }

    private void redirect(HttpServletResponse response, String url) {
        if (url == null)
            url = "";

        if (!url.startsWith("http"))
            url = getBaseURL() + url;

        try {
            response.sendRedirect(response.encodeRedirectURL(url));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void doLayout(HttpServletRequest request, HttpServletResponse response, Element elem, String format, boolean mcrWebPage) throws IOException {
        Document doc = new Document();

        if (mcrWebPage) {
            Element webPage = new Element("MyCoReWebPage");
            webPage.addContent(elem);
            doc.setRootElement(webPage);
            doc.setDocType(new DocType("MyCoReWebPage"));
        } else {
            doc.setRootElement(elem);
        }

        doLayout(request, response, doc, format);
    }

    private void doLayout(HttpServletRequest request, HttpServletResponse response, Document doc, String format) throws IOException {

        if (format.equals("xml"))
            getLayoutService().sendXML(request, response, doc);
        else
            getLayoutService().doLayout(request, response, doc);
    }
}
