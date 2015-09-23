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

import static org.mycore.access.MCRAccessManager.PERMISSION_DELETE;
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
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
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserErrorHandler;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCREditorOutValidator;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.cli.MCRDerivateCommands;
import org.mycore.frontend.cli.MCRObjectCommands;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.fileupload.MCRUploadHandlerIFS;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 *
 */
public class MCRPersistentServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRPersistentServlet.class);

    protected static String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");

    public static enum Operation {
        create, update, delete
    }

    public static enum Type {
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
        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse(), MCRFrontendUtil.getBaseURL()))
            return;
        switch (operation) {
            case create:
                //derivate is handled in render phase
                if (type == Type.object) {
                    Document editorSubmission = getEditorSubmission(job, false);
                    //editorSubmission is null, when editor input is absent (redirect to editor form in render phase)
                    if (editorSubmission != null) {
                        MCRObjectID objectID = createObject(job, editorSubmission);
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
        Document inDoc = (Document) (job.getRequest().getAttribute("MCRXEditorSubmission"));
        if (inDoc == null) {
            MCREditorSubmission sub = (MCREditorSubmission) job.getRequest().getAttribute("MCREditorSubmission");
            if (sub != null)
                inDoc = sub.getXML();
            else if (failOnMissing)
                throw new ServletException("No MCREditorSubmission");
            else
                return null;
        }
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
    private MCRObjectID createObject(MCRServletJob job, Document doc) throws MCRActiveLinkException, JDOMException,
        IOException, MCRException, SAXParseException {
        MCRObject mcrObject = getMCRObject(doc);
        MCRObjectID objectId = mcrObject.getId();
        if (!MCRAccessManager.checkPermission("create-" + objectId.getBase())
            && !MCRAccessManager.checkPermission("create-" + objectId.getTypeId())) {
            job.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "You do not have \"create\" permission on " + objectId.getTypeId() + ".");
            return null;
        }
        //noinspection SynchronizeOnNonFinalField
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
    private MCRObjectID updateObject(Document doc) throws MCRActiveLinkException, JDOMException, IOException,
        MCRException, SAXParseException {
        MCRObject mcrObject = getMCRObject(doc);
        LOGGER.info("ID: " + mcrObject.getId());
        try {
            if (MCRAccessManager.checkPermission(mcrObject.getId(), PERMISSION_WRITE)) {
                MCRMetadataManager.update(mcrObject);
                return mcrObject.getId();
            } else {
                throw new MCRPersistenceException("You do not have \"" + PERMISSION_WRITE + "\" permission on "
                    + mcrObject.getId() + ".");
            }
        } finally {
            MCRObjectIDLockTable.unlock(mcrObject.getId());
        }
    }

    /**
     * Updates derivate xml in the persistence backend and store the label
     * in the corresponding object derivate entry
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
        if (!MCRAccessManager.checkPermission(derivateID, PERMISSION_WRITE)) {
            throw new MCRPersistenceException("You do not have \"" + PERMISSION_WRITE + "\" permission on "
                + derivateID + ".");
        }
        MCRMetadataManager.updateMCRDerivateXML(der);
        objectID = der.getDerivate().getMetaLink().getXLinkHrefID();
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(objectID);
        List<MCRMetaLinkID> linkIDs = obj.getStructure().getDerivates();
        for (MCRMetaLinkID linkID : linkIDs) {
        	if (linkID.getXLinkHrefID().equals(derivateID)) {
        		linkID.setXLinkTitle(der.getLabel());
        		try {
					MCRMetadataManager.update(obj);
				} catch (MCRPersistenceException | MCRActiveLinkException e) {
		            throw new MCRPersistenceException("Can't store label of derivate " + derivateID
		                 + " in derivate list of object " + objectID + ".", e);
				}
        	}
        }
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
        if (MCRAccessManager.checkPermission(id, PERMISSION_DELETE))
            MCRObjectCommands.delete(id);
        else
            throw new MCRPersistenceException("You do not have \"" + PERMISSION_DELETE + "\" permission on " + id + ".");
    }

    /**
     * Deletes a mycore derivate from the persistence backend.
     * @param id
     *  MyCoRe derivate ID
     * @throws MCRActiveLinkException 
     * @throws MCRPersistenceException 
     */
    private void deleteDerivate(String id) throws MCRPersistenceException, MCRActiveLinkException {
        if (MCRAccessManager.checkPermission(id, PERMISSION_DELETE)) {
            MCRDerivateCommands.delete(id);
        } else
            throw new MCRPersistenceException("You do not have \"" + PERMISSION_DELETE + "\" permission on " + id + ".");
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
                errorLog.add(MCRXMLParserErrorHandler.getSAXErrorMessage((SAXParseException) ex));
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
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString()));
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
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString()));
                        break;
                    case derivate: {
                        returnID = (MCRObjectID) job.getRequest().getAttribute(OBJECT_ID_KEY);
                        if (returnID == null) {
                            //calculate redirect to title change form or add files form
                            redirectToUpdateDerivate(job);
                            return;
                        } else
                            job.getResponse().sendRedirect(
                                job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "receive/" + returnID.toString()));
                        break;
                    }
                    default:
                        throw new MCRException("Operation " + operation + " is not implemented for type " + type);
                }
                break;
            case delete:
                switch (type) {
                    case object:
                        job.getResponse().sendRedirect(
                            job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "editor_deleted.xml"));
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

    /** Check for existing file type *.xed or *.xml
     * 
     * @param file name base
     * @return the complete file name, *.xed is default
     */
    private String checkFileName(String base_name) {
        String file_name = base_name + ".xed";
        MCRURIResolver URI_RESOLVER = MCRURIResolver.instance();
        try {
            URI_RESOLVER.resolve("webapp:" + file_name);
            return file_name;
        } catch (MCRException e) {
            LOGGER.warn("Can't find " + file_name + ", now we try it with " + base_name + ".xml");
            return base_name + ".xml";
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
            // TODO: don't use swf code here...
            String msg = MCRTranslation.translate("component.swf.page.error.user.text");
            job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN, msg);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(pagedir).append("editor_form_author").append('-').append(type);
        if (layout != null && layout.length() != 0) {
            sb.append('-').append(layout);
        }
        String form = checkFileName(sb.toString());
        Properties params = new Properties();
        params.put("cancelUrl", getCancelUrl(job));
        params.put("mcrid", MCRObjectID.formatID(base, 0));
        Enumeration<String> e = job.getRequest().getParameterNames();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = job.getRequest().getParameter(name);
            params.put(name, value);
        }
        job.getResponse().sendRedirect(
            job.getResponse().encodeRedirectURL(buildRedirectURL(MCRFrontendUtil.getBaseURL() + form, params)));
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
        if (!MCRAccessManager.checkPermission(parentObjectID, PERMISSION_WRITE)) {
            throw new MCRPersistenceException("You do not have \"" + PERMISSION_WRITE + "\" permission on "
                + parentObjectID + ".");
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
            if (!MCRAccessManager.checkPermission(parentObjectID, PERMISSION_WRITE)) {
                throw new MCRPersistenceException("You do not have \"" + PERMISSION_WRITE + "\" permission on "
                    + parentObjectID + ".");
            }
            if (!MCRAccessManager.checkPermission(derivateID, PERMISSION_WRITE)) {
                throw new MCRPersistenceException("You do not have \"" + PERMISSION_WRITE + "\" permission on "
                    + derivateID + ".");
            }
            redirectToUploadPage(job, parentObjectID, derivateID);
        } else {
            //set derivate title
            StringBuilder sb = new StringBuilder();
            Properties params = new Properties();
            sb.append("xslStyle:mycorederivate-editor:mcrobject:").append(derivateID);
            params.put("sourceUri", sb.toString());
            params.put("cancelUrl", getCancelUrl(job));
            sb = new StringBuilder();
            sb.append(MCRFrontendUtil.getBaseURL()).append(pagedir);
            try {
                MCRURIResolver.instance().resolve("webapp:/editor_form_derivate.xed");
                sb.append("editor_form_derivate.xed");
            } catch (MCRException e) {
                LOGGER.warn("Can't find editor_form_derivate.xed, now we try it with editor_form_derivate.xml");
                sb.append("editor_form_derivate.xml");
            }
            job.getResponse()
                .sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));
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
        MCRUploadHandlerIFS fuh = new MCRUploadHandlerIFS(parentObjectID, derivateID, getCancelUrl(job));
        String fuhid = fuh.getID();
        String page = pagedir + "fileupload_commit.xml";

        String base = MCRFrontendUtil.getBaseURL() + page;
        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("cancelUrl", getCancelUrl(job));
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("mcrid", parentObjectID);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
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
        String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");
        String myfile = pagedir
            + MCRConfiguration.instance().getString("MCR.SWF.PageErrorFormular", "editor_error_formular.xml");
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

    private String getCancelUrl(MCRServletJob job) {
        return getReferer(job);
    }

    private String getReferer(MCRServletJob job) {
        String referer = job.getRequest().getHeader("Referer");
        if (referer == null || referer.equals("")) {
            referer = MCRFrontendUtil.getBaseURL();
        }
        LOGGER.debug("Referer: " + referer);
        return referer;
    }

}
