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
            if (ex instanceof SAXParseException) {
                ArrayList<String> errorLog = new ArrayList<String>();
                errorLog.add(MCRXMLParserErrorHandler.getSAXErrorMessage((SAXParseException) ex));
                errorHandlerValid(job, errorLog);
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
        throws MCRAccessException, ServletException, MCRActiveLinkException, SAXParseException, JDOMException, IOException;

    abstract void displayResult(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException;

    protected void redirectToUploadForm(ServletContext context, HttpServletRequest request, HttpServletResponse response, String objectId, String derivateId)
        throws ServletException, IOException {
            MCRUploadHandlerIFS fuh = new MCRUploadHandlerIFS(objectId, derivateId, MCRPersistenceHelper.getCancelUrl(request));
            String fuhid = fuh.getID();
            String page = MCRPersistenceHelper.getWebPage(getServletContext(), "fileupload.xml", "fileupload_commit.xml");
            String base = MCRFrontendUtil.getBaseURL() + page;
            Properties params = new Properties();
            params.put("XSL.UploadID", fuhid);
            params.put("cancelUrl", MCRPersistenceHelper.getCancelUrl(request));
            params.put("XSL.target.param.1", "method=formBasedUpload");
            params.put("XSL.target.param.2", "uploadId=" + fuhid);
            params.put("XSL.ObjectID", objectId);
            params.put("mcrid", objectId);
            params.put("XSL.parentObjectID", objectId);
            response.sendRedirect(response.encodeRedirectURL(buildRedirectURL(base, params)));
        }

    /**
     * handles validation errors (XML Schema) and present nice pages instead of stack traces.
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    private void errorHandlerValid(MCRServletJob job, List<String> logtext) throws IOException, TransformerException,
        SAXException {
        // write to the log file
        for (String aLogtext : logtext) {
            LOGGER.error(aLogtext);
        }
    
        // prepare editor with error messages
        String myfile = "editor_error_formular.xml";
        //TODO: Access File directly
        Element root = MCRURIResolver.instance().resolve("webapp:" + myfile);
        List<Element> sectionlist = root.getChildren("section");
    
        for (Element section : sectionlist) {
            final String sectLang = section.getAttributeValue("lang", Namespace.XML_NAMESPACE);
            if (!sectLang.equals(MCRSessionMgr.getCurrentSession().getCurrentLanguage()) && !sectLang.equals("all")) {
                continue;
            }
    
            Element p = new Element("p");
            section.addContent(0, p);
    
            Element center = new Element("center");
    
            // the error message
            Element table = new Element("table");
            table.setAttribute("width", "80%");
    
            for (String logMsg : logtext) {
                Element tr = new Element("tr");
                Element td = new Element("td");
                Element el = new Element("pre");
                el.setAttribute("style", "color:red;");
                el.addContent(logMsg);
                td.addContent(el);
                tr.addContent(td);
                table.addContent(tr);
            }
    
            center.addContent(table);
            section.addContent(1, center);
            p = new Element("p");
            section.addContent(2, p);
            break;
        }
    
        // restart editor
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(root));
    }

}
