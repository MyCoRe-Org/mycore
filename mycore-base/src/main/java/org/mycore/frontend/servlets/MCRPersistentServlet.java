/*
 * $Id$
 * $Revision: 5697 $ $Date: 21.08.2009 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.servlets;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCREditorOutValidator;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.fileupload.MCRUploadHandlerIFS;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRPersistentServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRPersistentServlet.class);

    protected static String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");

    private static enum Operation {
        create, update, delete
    }

    private static enum Type {
        object, derivate
    }

    private Operation operation;

    private Type type;

    private static final String OBJECT_ID_KEY = MCRPersistentServlet.class.getCanonicalName() + ".MCRObjectID";

    @Override
    public void init() throws ServletException {
        super.init();
        String operation = getInitParameter("operation");
        if (operation == null) {
            throw new ServletException("Parameter \"operation\" is missing.");
        }
        String type = getInitParameter("type");
        if (type == null) {
            throw new ServletException("Parameter \"type\" is missing.");
        }
        this.operation = Operation.valueOf(operation);
        this.type = Type.valueOf(type);
    }

    @Override
    protected void think(MCRServletJob job) throws Exception {
        //If admin mode, do not change any data
        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(), getBaseURL()))
            return;
        switch (operation) {
        case create:
            //derivate is handled in render phase
            if (type == Type.object) {
                Document editorSubmission = getEditorSubmission(job, false);
                //editorSubmission is null, when editor input is absent (redirect to editor form in render phase)
                if (editorSubmission != null) {
                    MCRObjectID objectID = createObject(editorSubmission);
                    job.getRequest().setAttribute(OBJECT_ID_KEY, objectID);
                }
            }
            break;
        case update:
            switch (type) {
            case object:
                MCRObjectID objectID = updateObject(getEditorSubmission(job, true));
                job.getRequest().setAttribute(OBJECT_ID_KEY, objectID);
                break;
            case derivate:
                Document editorSubmission = getEditorSubmission(job, false);
                if (editorSubmission != null) {
                    objectID = updateDerivateXML(editorSubmission);
                    job.getRequest().setAttribute(OBJECT_ID_KEY, objectID);
                }
                break;
            default:
                throw new MCRException("Operation " + operation + " is not implemented for type " + type);
            }
            break;
        case delete:
            switch (type) {
            case object:
                deleteObject(getProperty(job.getRequest(), "id"));
                break;
            case derivate:
                deleteDerivate(getProperty(job.getRequest(), "id"));
                break;
            default:
                throw new MCRException("Operation " + operation + " is not implemented for type " + type);
            }
            break;
        default:
            throw new MCRException("Operation " + operation + " is not implemented");
        }
    }

    /**
     * returns a jdom document representing the output of a mycore editor form.
     * @param job
     * @param failOnMissing
     *  if not editor submission is present, should we throw an exception (true) or just return null (false)
     * @return
     *  editor submission or null if editor submission is not present and failOnMissing==false
     * @throws ServletException
     *  if failOnMissing==true and no editor submission is present
     */
    private Document getEditorSubmission(MCRServletJob job, boolean failOnMissing) throws ServletException {
        MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
        if (sub == null) {
            if (failOnMissing)
                throw new ServletException("No MCREditorSubmission");
            return null;
        }
        Document inDoc = sub.getXML();
        if (inDoc.getRootElement().getAttribute("ID") == null) {
            String mcrID = getProperty(job.getRequest(), "mcrid");
            LOGGER.info("Adding MCRObjectID from request: " + mcrID);
            inDoc.getRootElement().setAttribute("ID", mcrID);
        }
        return inDoc;
    }

    /**
     * creates a MCRObject instance on base of JDOM document
     * @param doc
     *  MyCoRe object as XML.
     * @return
     * @throws JDOMException
     *  exception from underlying {@link MCREditorOutValidator}
     * @throws IOException
     *  exception from underlying {@link MCREditorOutValidator} or {@link XMLOutputter}
     * @throws SAXParseException 
     * @throws MCRException 
     */
    private MCRObject getMCRObject(Document doc) throws JDOMException, IOException, MCRException, SAXParseException {
        MCREditorOutValidator ev = null;
        String id = doc.getRootElement().getAttributeValue("ID");
        MCRObjectID objectID = MCRObjectID.getInstance(id);
        ev = new MCREditorOutValidator(doc, objectID);
        Document jdom_out = ev.generateValidMyCoReObject();
        if (ev.getErrorLog().size() > 0 && LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter swOrig = new StringWriter();
            xout.output(doc, swOrig);
            LOGGER.debug("Input document \n" + swOrig.toString());
            for (String logMsg : ev.getErrorLog()) {
                LOGGER.debug(logMsg);
            }
            StringWriter swClean = new StringWriter();
            xout.output(jdom_out, swClean);
            LOGGER.debug("Results in \n" + swClean.toString());
        }
        MCRObject mcrObject = new MCRObject(jdom_out);
        return mcrObject;
    }

    /**
     * Adds a new mycore object to the persistence backend.
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws MCRActiveLinkException
     *  If links from or to other objects will fail.
     * @throws JDOMException
     *  from {@link #getMCRObject(Document)}
     * @throws IOException
     *  from {@link #getMCRObject(Document)}
     * @throws SAXParseException 
     * @throws MCRException 
     */
    private MCRObjectID createObject(Document doc) throws MCRActiveLinkException, JDOMException, IOException, MCRException, SAXParseException {
        MCRObject mcrObject = getMCRObject(doc);
        MCRObjectID objectId = mcrObject.getId();
        if (MCRAccessManager.checkPermission("create-" + objectId.getBase()) || MCRAccessManager.checkPermission("create-" + objectId.getTypeId())) {
            synchronized (operation) {
                if (objectId.getNumberAsInteger() == 0) {
                    String objId = mcrObject.getId().toString();
                    objectId = MCRObjectID.getNextFreeId(objectId.getBase());
                    if (mcrObject.getLabel().equals(objId))
                        mcrObject.setLabel(objectId.toString());
                    mcrObject.setId(objectId);
                }
                MCRMetadataManager.create(mcrObject);
            }
            return objectId;
        } else
            throw new MCRPersistenceException("You do not have \"create\" permission on " + objectId.getTypeId() + ".");
    }

    /**
     * Updates a mycore object in the persistence backend. 
     * @param doc
     *  MyCoRe object as XML
     * @return
     *  MCRObjectID of the newly created object.
     * @throws MCRActiveLinkException
     *  If links from or to other objects will fail.
     * @throws JDOMException
     *  from {@link #getMCRObject(Document)}
     * @throws IOException
     *  from {@link #getMCRObject(Document)}
     * @throws SAXParseException 
     * @throws MCRException 
     */
    private MCRObjectID updateObject(Document doc) throws MCRActiveLinkException, JDOMException, IOException, MCRException, SAXParseException {
        MCRObject mcrObject = getMCRObject(doc);
        LOGGER.info("ID: " + mcrObject.getId());
        if (MCRAccessManager.checkPermission(mcrObject.getId(), "writedb")) {
            MCRMetadataManager.update(mcrObject);
            return mcrObject.getId();
        } else
            throw new MCRPersistenceException("You do not have \"write\" permission on " + mcrObject.getId() + ".");
    }

    /**
     * Updates derivate xml in the persistence backend
     * @param editorSubmission
     *  MyCoRe derivate as XML
     * @return
     *  MCRObjectID of the MyCoRe object
     * @throws SAXParseException 
     */
    private MCRObjectID updateDerivateXML(Document editorSubmission) throws SAXParseException, IOException {
        MCRObjectID objectID;
        Element root = editorSubmission.getRootElement();
        root.setAttribute("noNamespaceSchemaLocation", "datamodel-derivate.xsd", XSI_NAMESPACE);
        root.addNamespaceDeclaration(XLINK_NAMESPACE);
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        byte[] xml = MCRUtils.getByteArray(editorSubmission);
        MCRDerivate der = new MCRDerivate(xml, true);
        String derivateID = der.getId().toString();
        if (!MCRAccessManager.checkPermission(derivateID, "writedb")) {
            throw new MCRPersistenceException("You do not have \"write\" permission on " + derivateID + ".");
        }
        MCRMetadataManager.updateMCRDerivateXML(der);
        objectID = der.getDerivate().getMetaLink().getXLinkHrefID();
        return objectID;
    }

    /**
     * Deletes a mycore object from the persistence backend.
     * @param id
     *  MyCoRe object ID
     * @throws MCRActiveLinkException
     *  If links from other objects will fail.
     */
    private void deleteObject(String id) throws MCRActiveLinkException {
        if (MCRAccessManager.checkPermission(id, "deletedb"))
            MCRObjectCommands.delete(id);
        else
            throw new MCRPersistenceException("You do not have \"delete\" permission on " + id + ".");
    }

    /**
     * Deletes a mycore derivate from the persistence backend.
     * @param id
     *  MyCoRe derivate ID
     * @throws MCRActiveLinkException 
     * @throws MCRPersistenceException 
     */
    private void deleteDerivate(String id) throws MCRPersistenceException, MCRActiveLinkException {
        if (MCRAccessManager.checkPermission(id, "deletedb")) {
            MCRDerivateCommands.delete(id);
        } else
            throw new MCRPersistenceException("You do not have \"delete\" permission on " + id + ".");
    }

    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        if (job.getResponse().isCommitted()) {
            LOGGER.info("Response allready committed");
            return;
        }
        if (ex != null) {
            if (ex instanceof SAXParseException) {
                ArrayList<String> errorLog = new ArrayList<String>();
                errorLog.add(getSAXErrorMessage((SAXParseException) ex));
                errorHandlerValid(job, errorLog);
                return;
            }
            throw ex;
        }
        switch (operation) {
        case create:
            switch (type) {
            case object:
                //return to object itself if created, else call editor form
                MCRObjectID returnID = (MCRObjectID) job.getRequest().getAttribute(OBJECT_ID_KEY);
                if (returnID == null)
                    redirectToCreateObject(job);
                else
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + "receive/" + returnID.toString()));
                return;
            case derivate:
                redirectToCreateDerivate(job);
                return;
            default:
                throw new MCRException("Operation " + operation + " is not implemented for type " + type);
            }
        case update:
            switch (type) {
            case object:
                MCRObjectID returnID = (MCRObjectID) job.getRequest().getAttribute(OBJECT_ID_KEY);
                if (returnID == null) {
                    throw new MCRException("No MCRObjectID given.");
                } else
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + "receive/" + returnID.toString()));
                break;
            case derivate: {
                returnID = (MCRObjectID) job.getRequest().getAttribute(OBJECT_ID_KEY);
                if (returnID == null) {
                    //calculate redirect to title change form or add files form
                    redirectToUpdateDerivate(job);
                    return;
                } else
                    job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + "receive/" + returnID.toString()));
                break;
            }
            default:
                throw new MCRException("Operation " + operation + " is not implemented for type " + type);
            }
            break;
        case delete:
            switch (type) {
            case object:
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + "editor_deleted.xml"));
                break;
            case derivate:
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getReferer(job)));
                break;
            default:
                throw new MCRException("Operation " + operation + " is not implemented for type " + type);
            }
            break;
        default:
            throw new MCRException("Operation " + operation + " is not implemented");
        }
    }

    /**
     * redirects to new mcrobject form.
     * 
     * At least "type" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>type</dt>
     *   <dd>object type of the new object (required)</dd>
     *   <dt>project</dt>
     *   <dd>project ID part of object ID</dd>
     *   <dt>layout</dt>
     *   <dd>special editor form layout</dd>
     * </dl>
     * @param job
     * @throws IOException
     */
    private void redirectToCreateObject(MCRServletJob job) throws IOException {
        String projectID = getProperty(job.getRequest(), "project");
        String type = getProperty(job.getRequest(), "type");
        String layout = getProperty(job.getRequest(), "layout");
        if (projectID == null) {
            String defaultProjectID = MCRConfiguration.instance().getString("MCR.SWF.Project.ID", "MCR");
            projectID = MCRConfiguration.instance().getString("MCR.SWF.Project.ID." + type, defaultProjectID);
        }
        String base = projectID + "_" + type;
        if (!(MCRAccessManager.checkPermission("create-" + base) || MCRAccessManager.checkPermission("create-" + type))) {
            String msg = MCRTranslation.translate("component.swf.page.error.user.text");
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(pagedir).append("editor_form_author").append('-').append(type);
        if (layout != null && layout.length() != 0) {
            sb.append('-').append(layout);
        }
        String form = sb.append(".xml").toString();
        Properties params = new Properties();
        params.put("cancelUrl", getReferer(job));
        params.put("mcrid", MCRObjectID.formatID(base, 0));
        @SuppressWarnings("unchecked")
        Enumeration<String> e = job.getRequest().getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = job.getRequest().getParameter(name);
            params.put(name, value);
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(getBaseURL() + form, params)));
    }

    /**
     * redirects to new derivate upload form.
     * 
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>object ID of the parent mycore object (required)</dd>
     * </dl>
     * @param job
     * @throws IOException
     */
    private void redirectToCreateDerivate(MCRServletJob job) throws IOException {
        String parentObjectID = getProperty(job.getRequest(), "id");
        if (!MCRAccessManager.checkPermission(parentObjectID, "writedb")) {
            throw new MCRPersistenceException("You do not have \"write\" permission on " + parentObjectID + ".");
        }
        redirectToUploadPage(job, parentObjectID, null);
    }

    /**
     * redirects to either add files to derivate upload form or change derivate title form.
     * 
     * At least "id" HTTP parameter is required to succeed.
     * <dl>
     *   <dt>id</dt>
     *   <dd>derivate ID(required)</dd>
     *   <dt>objectid</dt>
     *   <dd>object ID of the parent mycore object</dd>
     * </dl>
     * If the "objectid" parameter is given, upload form is presented.
     * If not than the user is redirected to the title change form.
     * @param job
     * @throws IOException
     */
    private void redirectToUpdateDerivate(MCRServletJob job) throws IOException {
        String parentObjectID = getProperty(job.getRequest(), "objectid");
        String derivateID = getProperty(job.getRequest(), "id");
        if (parentObjectID != null) {
            //Load additional files
            if (!MCRAccessManager.checkPermission(parentObjectID, "writedb")) {
                throw new MCRPersistenceException("You do not have \"write\" permission on " + parentObjectID + ".");
            }
            if (!MCRAccessManager.checkPermission(derivateID, "writedb")) {
                throw new MCRPersistenceException("You do not have \"write\" permission on " + derivateID + ".");
            }
            redirectToUploadPage(job, parentObjectID, derivateID);
        } else {
            //set derivate title
            StringBuilder sb = new StringBuilder();
            Properties params = new Properties();
            sb.append("xslStyle:mycorederivate-editor:mcrobject:").append(derivateID);
            params.put("sourceUri", sb.toString());
            params.put("cancelUrl", getReferer(job));
            sb = new StringBuilder();
            sb.append(getBaseURL()).append(pagedir).append("editor_form_derivate.xml");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));
        }
    }

    /**
     * redirects to upload form page.
     * 
     * used by {@link #redirectToCreateDerivate(MCRServletJob)} {@link #redirectToUpdateDerivate(MCRServletJob)}
     * @param job
     * @param parentObjectID
     * @param derivateID
     * @throws IOException
     */
    private void redirectToUploadPage(MCRServletJob job, String parentObjectID, String derivateID) throws IOException {
        MCRUploadHandlerIFS fuh = new MCRUploadHandlerIFS(parentObjectID, derivateID, getReferer(job));
        String fuhid = fuh.getID();
        String page = pagedir + "fileupload_commit.xml";

        String base = getBaseURL() + page;
        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("cancelUrl", getReferer(job));
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("mcrid", parentObjectID);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * handles validation errors (XML Schema) and present nice pages instead of stack traces.
     * @throws IOException 
     */
    private final void errorHandlerValid(MCRServletJob job, List<String> logtext) throws IOException {
        // write to the log file
        for (int i = 0; i < logtext.size(); i++) {
            LOGGER.error(logtext.get(i));
        }

        // prepare editor with error messages
        String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");
        String myfile = pagedir + MCRConfiguration.instance().getString("MCR.SWF.PageErrorFormular", "editor_error_formular.xml");
        //TODO: Access File directly
        Element root = MCRURIResolver.instance().resolve("webapp:" + myfile);
        @SuppressWarnings("unchecked")
        List<Element> sectionlist = root.getChildren("section");

        for (int i = 0; i < sectionlist.size(); i++) {
            Element section = sectionlist.get(i);

            final String sectLang = section.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
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
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), root.getDocument());
    }

    /**
     * Returns a text indicating at which line and column the error occurred.
     * 
     * @param ex
     *            the SAXParseException exception
     * @return the location string
     */
    private String getSAXErrorMessage(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');

            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }

            str.append(systemId).append(": ");
        }

        str.append("line ").append(ex.getLineNumber());
        str.append(", column ").append(ex.getColumnNumber());
        str.append(", ");
        str.append(ex.getLocalizedMessage());

        return str.toString();
    }

    private String getReferer(MCRServletJob job) {
        String referer = job.getRequest().getHeader("Referer");
        if (referer == null || referer.equals("")) {
            referer = getBaseURL();
        }
        LOGGER.debug("Referer: " + referer);
        return referer;
    }

}
