/*
 * 
 * $Revision$ $Date$
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.validator.MCREditorOutValidator;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;
import org.xml.sax.SAXException;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML and store the XML in a file or if an error was occured start the
 * editor again.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
abstract public class MCRCheckDataBase extends MCRCheckBase {
    private static final long serialVersionUID = 8744330302159842747L;

    private static Logger LOGGER = Logger.getLogger(MCRCheckDataBase.class);

    /**
      * This method overrides doGetPost of MCRServlet. <br />
      */
    public void doGetPost(MCRServletJob job) throws Exception {
        // read the XML data
        Document indoc = readEditorOutput(job);

        // read the parameter
        MCRRequestParameters parms;

        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        if (sub == null) {
            parms = new MCRRequestParameters(job.getRequest());
        } else {
            parms = sub.getParameters();
        }

        String oldmcrid = parms.getParameter("mcrid");
        String oldtype = parms.getParameter("type");
        String oldstep = parms.getParameter("step");
        LOGGER.debug("XSL.target.param.0 = " + oldmcrid);
        LOGGER.debug("XSL.target.param.1 = " + oldtype);
        LOGGER.debug("XSL.target.param.2 = " + oldstep);

        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String lang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + lang);

        // prepare the MCRObjectID's for the Metadata
        String mmcrid = "";
        boolean hasid = false;

        try {
            mmcrid = indoc.getRootElement().getAttributeValue("ID");

            if (mmcrid == null) {
                mmcrid = oldmcrid;
            } else {
                hasid = true;
            }
        } catch (Exception e) {
            mmcrid = oldmcrid;
        }

        MCRObjectID ID = MCRObjectID.getInstance(mmcrid);

        if (!ID.getTypeId().equals(oldtype)) {
            ID = MCRObjectID.getInstance(oldmcrid);
            hasid = false;
        }

        if (!hasid) {
            indoc.getRootElement().setAttribute("ID", ID.toString());
        }

        // check access
        if (!checkAccess(ID)) {
            job.getResponse().sendRedirect(MCRFrontendUtil.getBaseURL() + usererrorpage);
            return;
        }

        // Save the incoming to a file
        byte[] outxml = MCRUtils.getByteArray(indoc);
        File savedir = MCRSimpleWorkflowManager.instance().getDirectoryPath(ID.getBase());
        File fullname = new File(savedir, ID.toString() + ".xml");
        storeMetadata(outxml, job, ID, fullname.getAbsolutePath());

        // create a metadata object and prepare it
        org.jdom2.Document outdoc = prepareMetadata((org.jdom2.Document) indoc.clone(), ID, job, lang);
        if (outdoc == null)
            return;
        outxml = MCRUtils.getByteArray(outdoc);

        // Save the prepared metadata object
        boolean okay = storeMetadata(outxml, job, ID, fullname.getAbsolutePath());

        // call the getNextURL and sendMail methods
        String url = getNextURL(ID, okay);
        sendMail(ID);
        if (!job.getResponse().isCommitted())
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + url));
    }

    /**
     * The method stores the data in a working directory dependenced of the
     * type.
     * 
     * @param outxml
     *            the prepared JDOM object
     * @param job
     *            the MCRServletJob
     * @param ID
     *            MCRObjectID of the MCRObject/MCRDerivate
     * @param fullname
     *            the file name where the JDOM was stored.
     */
    public final boolean storeMetadata(byte[] outxml, MCRServletJob job, MCRObjectID ID, String fullname) throws Exception {
        if (outxml == null) {
            return false;
        }

        // Save the prepared MCRObject/MCRDerivate to a file
        FileOutputStream out = new FileOutputStream(fullname);
        try {
            out.write(outxml);
            out.flush();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to file " + fullname);
            errorHandlerIO(job);

            return false;
        } finally {
            out.close();
        }

        LOGGER.info("Object " + ID.toString() + " stored under " + fullname + ".");
        return true;
    }

    /**
     * The method read the incoming JDOM tree in a MCRObject and prepare this by
     * the following rules. After them it return a JDOM as result of
     * MCRObject.createXML(). <br/>
     * <li>remove all target of MCRMetaClassification they have not a categid
     * attribute.</li>
     * <br/>
     * <li>remove all target of MCRMetaLangText they have an empty text</li>
     * <br/>
     * 
     * @param jdom_in
     *            the JDOM tree from the editor
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param job
     *            the MCRServletJob data
     * @param lang
     *            the current language
     */
    protected org.jdom2.Document prepareMetadata(org.jdom2.Document jdom_in, MCRObjectID ID, MCRServletJob job, String lang)
            throws IOException, TransformerException, SAXException {
        MCREditorOutValidator ev = null;
        try {
            ev = new MCREditorOutValidator(jdom_in, ID);
            Document jdom_out = ev.generateValidMyCoReObject();
            if (LOGGER.getEffectiveLevel().isGreaterOrEqual(Level.INFO))
                for (String logMsg : ev.getErrorLog()) {
                    LOGGER.info(logMsg);
                }
            return jdom_out;
        } catch (Exception e) {
            List<String> errorLog = ev != null ? ev.getErrorLog() : new ArrayList<String>();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorLog.add(sw.toString());
            pw.close();
            errorHandlerValid(job, errorLog, ID, lang);
            return null;
        }
    }

    /**
     * A method to handle valid errors.
     * @throws IOException 
     * @throws SAXException 
     * @throws TransformerException 
     */
    private void errorHandlerValid(MCRServletJob job, List<String> logtext, MCRObjectID ID, String lang) throws IOException, TransformerException, SAXException {
        // handle HttpSession
        String sessionID = "";
        HttpSession session = job.getRequest().getSession(false);
        if (session != null) {
            String jSessionID = MCRConfiguration.instance().getString("MCR.Session.Param", ";jsessionid=");
            sessionID = jSessionID + session.getId();
        }

        // write to the log file
        for (String aLogtext : logtext) {
            LOGGER.error(aLogtext);
        }

        // prepare editor with error messages
        String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");
        String myfile = pagedir + MCRConfiguration.instance().getString("MCR.SWF.PageErrorFormular", "editor_error_formular.xml");
        org.jdom2.Document jdom = null;

        try {
            //TODO: Access File directly
            InputStream in = new URL(MCRFrontendUtil.getBaseURL() + myfile + sessionID + "?XSL.Style=xml").openStream();

            if (in == null) {
                throw new MCRConfigurationException("Can't read editor file " + myfile);
            }

            jdom = new org.jdom2.input.SAXBuilder().build(in);

            Element root = jdom.getRootElement();
            List<Element> sectionlist = root.getChildren("section");

            for (Element section : sectionlist) {
                final String sectLang = section.getAttributeValue("lang", Namespace.XML_NAMESPACE);
                if (!sectLang.equals(lang) && !sectLang.equals("all")) {
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
        } catch (org.jdom2.JDOMException e) {
            throw new MCRException("Can't read editor file " + myfile + " or it has a parse error.", e);
        }

        // restart editor
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
    }

}
