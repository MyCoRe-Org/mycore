/**
 * 
 */
package org.mycore.frontend.servlets.persistence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserErrorHandler;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.fileupload.MCRUploadHandlerIFS;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Acts as a base class for all other servlets in this package
 * @author Thomas Scheffler (yagee)
 *
 */
abstract class MCRPersistenceServlet extends MCRServlet {
    /**
     * 
     */
    private static final long serialVersionUID = -6776941436009193613L;

    protected static final String OBJECT_ID_KEY = MCRPersistenceServlet.class.getCanonicalName() + ".MCRObjectID";

    private Logger LOGGER = LogManager.getLogger();

    private String uploadPage;

    @Override
    public void init() throws ServletException {
        super.init();
        String configuredPage = MCRConfiguration.instance().getString("MCR.FileUpload.WebPage");
        uploadPage = MCRPersistenceHelper.getWebPage(getServletContext(), configuredPage, "fileupload_commit.xml");
    }

    @Override
    protected void think(MCRServletJob job) throws Exception {
        //If admin mode, do not change any data
        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(),
            MCRFrontendUtil.getBaseURL()))
            return;
        handlePersistenceOperation(job.getRequest(), job.getResponse());
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (job.getResponse().isCommitted()) {
            LOGGER.info("Response allready committed");
            return;
        }
        if (ex != null) {
            if (ex instanceof MCRAccessException) {
                job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
                return;
            }
            if (ex instanceof MCRActiveLinkException) {
                String msg = ((MCRActiveLinkException) ex)
                    .getActiveLinks()
                    .values()
                    .iterator()
                    .next()
                    .stream()
                    .collect(Collectors.joining(String.format(Locale.ROOT, "%n  - "),
                        String.format(Locale.ROOT, "%s%n  - ", ex.getMessage()),
                        String.format(Locale.ROOT, "%nPlease remove links before trying again.")));
                throw new ServletException(msg, ex);
            }
            throw ex;
        }
        displayResult(job.getRequest(), job.getResponse());
    }

    abstract void handlePersistenceOperation(HttpServletRequest request, HttpServletResponse response)
        throws MCRAccessException, ServletException, MCRActiveLinkException, SAXParseException, JDOMException,
        IOException;

    abstract void displayResult(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException;

    protected void redirectToUploadForm(ServletContext context, HttpServletRequest request,
        HttpServletResponse response, String objectId, String derivateId)
        throws ServletException, IOException {
        MCRUploadHandlerIFS fuh = new MCRUploadHandlerIFS(objectId, derivateId,
            MCRPersistenceHelper.getCancelUrl(request));
        String fuhid = fuh.getID();
        String base = MCRFrontendUtil.getBaseURL() + uploadPage;
        Properties params = new Properties();
        params.put("uploadId", fuhid);
        params.put("parentObjectID", objectId);
        if (derivateId != null) {
            params.put("derivateID", derivateId);
        }
        params.put("cancelUrl", MCRPersistenceHelper.getCancelUrl(request));
        response.sendRedirect(response.encodeRedirectURL(buildRedirectURL(base, params)));
    }

}
