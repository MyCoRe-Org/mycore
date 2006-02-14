/*
 * $RCSfile$
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jdom.Element;

import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFileImportExport;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.editor.MCREditorServletHelper;
import org.mycore.frontend.fileupload.MCRUploadHandlerMyCoRe;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * The servlet start the MyCoRe editor session or other workflow actions with
 * some parameters from a HTML form. The parameters are: <br />
 * <li>type - the MCRObjectID type like schrift, text ...</li>
 * <br />
 * <li>step - the name of the step like author, editor ...</li>
 * <br />
 * <li>layout - the name of the layout like firststep, secondstep ...</li>
 * <br />
 * <li>todo - the mode of the editor start like new or edit or change or delete
 * </li>
 * <br />
 * <li>tf_mcrid - the MCRObjectID of the data they came from a input field
 * </li>
 * <br />
 * <li>se_mcrid - the MCRObjectID of the data they came from a select field
 * </li>
 * <br />
 * <li>re_mcrid - the MCRObjectID of the data they is in relation to
 * tf_mcrid/se_mcrid</li>
 * <br />
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRStartEditorServlet extends MCRServlet {
    // The configuration
    private static Logger LOGGER = Logger.getLogger(MCRStartEditorServlet.class);

    // The workflow manager
    private static MCRSimpleWorkflowManager WFM = null;

    // The file slash
    private static String SLASH = System.getProperty("file.separator");;

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();

        // Workflow Manager
        WFM = MCRSimpleWorkflowManager.instance();

    }

    /**
     * This method overrides doGetPost of MCRServlet. <br />
     * The <b>todo </b> value corresponds with <b>tf_mcrid </b> or <b>se_mcrid
     * </b> and with the type of the data model for the privileges that the user
     * need. for some actions you need a third value of re_mcrid for relations
     * (object - derivate). <br />
     * 
     * The table shows the possible todo's in the server: <br />
     * <table>
     * <tr>
     * <th>TODO</th>
     * <th>MCRObjectID from</th>
     * <th>used privileg</th>
     * <th>description</th>
     * </tr>
     * <tr>
     * <td>seditobj</td>
     * <td>tf_mcrid</td>
     * <td>modify-type</td>
     * <td>edit an object in the server</td>
     * </tr>
     * <tr>
     * <td>sdelobj</td>
     * <td>tf_mcrid</td>
     * <td>delete-type</td>
     * <td>delete an object from the server</tr>
     * <tr>
     * <td>snewder</td>
     * <td>tf_mcrid</td>
     * <td>create-type</td>
     * <td>create a new derivate in the server</tr>
     * <tr>
     * <td>sdelder</td>
     * <td>tf_mcrid</td>
     * <td>delete-type</td>
     * <td>delete a derivate from the server</tr>
     * <tr>
     * <td>seditder</td>
     * <td>tf_mcrid</td>
     * <td>modify-type</td>
     * <td>change a derivate in the server</tr>
     * <tr>
     * <td>saddfile</td>
     * <td>tf_mcrid <br />
     * re_mcrid</td>
     * <td>modify-type</td>
     * <td>add a new file to a derivate in the server</td>
     * </tr>
     * <tr>
     * <td>sdelfile</td>
     * <td>tf_mcrid <br />
     * re_mcrid</td>
     * <td>modify-type</td>
     * <td>remove a file from a derivate in the server</td>
     * </tr>
     * </table> <br />
     * 
     * The table shows the possible todo's in the workflow: <br />
     * <table>
     * <tr>
     * <th>TODO</th>
     * <th>MCRObjectID from</th>
     * <th>used privileg</th>
     * <th>description</th>
     * </tr>
     * <tr>
     * <td>wnewobj</td>
     * <td></td>
     * <td>create-type</td>
     * <td>add a new object to the workflow</td>
     * </tr>
     * <tr>
     * <td>wnewder</td>
     * <td>se_mcrid</td>
     * <td>create-type</td>
     * <td>add a new derivate to the workflow</td>
     * </tr>
     * <tr>
     * <td>waddfile</td>
     * <td>se_mcrid <br />
     * re_mcrid</td>
     * <td>create-type</td>
     * <td>add a new file to a derivate in the workflow</td>
     * </tr>
     * <tr>
     * <td>weditobj</td>
     * <td>se_mcrid</td>
     * <td>modify-type</td>
     * <td>edit an object in the workflow</td>
     * </tr>
     * <tr>
     * <td>weditder</td>
     * <td>se_mcrid (Der) <br />
     * re_mcrid (Obj)</td>
     * <td>modify-type</td>
     * <td>edit an derivate in the workflow</td>
     * </tr>
     * <tr>
     * <td>wcommit</td>
     * <td>se_mcrid</td>
     * <td>commit-type</td>
     * <td>commit a document to the server</td>
     * </tr>
     * <tr>
     * <td>wdelobj</td>
     * <td>se_mcrid</td>
     * <td>delete-type</td>
     * <td>delete an object from the workflow</td>
     * </tr>
     * <tr>
     * <td>wdelder</td>
     * <td>se_mcrid <br />
     * re_mcrid</td>
     * <td>delete-type</td>
     * <td>delete a derivate from the workflow</td>
     * </tr>
     * </table> <br />
     * <li>If the privileg is not correct it calls
     * <em>editor_error_user.xml</em>.</li>
     * <br />
     * <li>If the MCRObjectID is not correct it calls
     * <em>editor_error_mcrid.xml</em>.</li>
     * <br />
     * <li>If a store error is occured it calls <em>editor_error_store.xml</em>.
     * </li>
     * <br />
     * <li>If <b>CANCEL </b> was pressed it calls <em>editor_cancel.xml</em>.
     * </li>
     * <br />
     * <li>If the privileg is correct it starts the file editor_form_
     * <em>step-type</em> .xml.</li>
     * <br />
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        // get the current language
        String mylang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + mylang);

        // read the parameter
        // get the step
        String mystep = getProperty(job.getRequest(), "step");

        if (mystep == null) {
            mystep = "";
        }

        LOGGER.info("STEP = " + mystep);

        // get the type
        String mytype = getProperty(job.getRequest(), "type");

        if (mytype == null) {
            mytype = CONFIG.getString("MCR.default_project_type", "document");
        }

        if (mytype.length() == 0) {
            mytype = CONFIG.getString("MCR.default_project_type", "document");
        }

        LOGGER.info("TYPE = " + mytype);

        // get the layout
        String mylayout = getProperty(job.getRequest(), "layout");

        if (mylayout == null) {
            mylayout = "";
        }

        LOGGER.info("LAYOUT = " + mylayout);

        // get what is to do
        String mytodo = getProperty(job.getRequest(), "todo");

        if ((mytodo == null) || ((mytodo = mytodo.trim()).length() == 0)) {
            mytodo = "wrongtodo";
        }

        if (!mytodo.equals("wnewobj") && !mytodo.equals("wnewder") && !mytodo.equals("waddfile") && !mytodo.equals("wdelfile") && !mytodo.equals("weditobj") && !mytodo.equals("weditder") && !mytodo.equals("wdelobj") && !mytodo.equals("wdelder") && !mytodo.equals("wsetfile") && !mytodo.equals("wsetlabel") && !mytodo.equals("wcommit") && !mytodo.equals("seditobj") && !mytodo.equals("seditder") && !mytodo.equals("sdelobj") && !mytodo.equals("sdelder") && !mytodo.equals("snewder") && !mytodo.equals("scommitder") && !mytodo.equals("saddfile") && !mytodo.equals("snewfile") && !mytodo.equals("sdelfile") && !mytodo.equals("ssetlabel") && !mytodo.equals("ssetfile")) {
            mytodo = "wrongtodo";
        }

        LOGGER.info("TODO = " + mytodo);

        // get the MCRObjectID from the text filed (TF)
        String mytfmcrid = getProperty(job.getRequest(), "tf_mcrid");

        try {
            new MCRObjectID(mytfmcrid);
        } catch (Exception e) {
            mytfmcrid = "";
        }

        if ((mytfmcrid == null) || ((mytfmcrid = mytfmcrid.trim()).length() == 0)) {
            String defaproject = CONFIG.getString("MCR.default_project_id", "MCR");
            String myproject = CONFIG.getString("MCR." + mytype + "_project_id", "MCR");

            if (myproject.equals("MCR")) {
                myproject = defaproject;
            }

            myproject = myproject + "_" + mytype;

            MCRObjectID mcridnext = new MCRObjectID();
            mcridnext.setNextFreeId(myproject);

            String workdir = CONFIG.getString("MCR.editor_" + mytype + "_directory", "/");
            File workf = new File(workdir);

            if (workf.isDirectory()) {
                String[] list = workf.list();

                for (int i = 0; i < list.length; i++) {
                    if (!list[i].startsWith(myproject)) {
                        continue;
                    }

                    try {
                        MCRObjectID mcriddir = new MCRObjectID(list[i].substring(0, list[i].length() - 4));

                        if (mcridnext.getNumberAsInteger() <= mcriddir.getNumberAsInteger()) {
                            mcriddir.setNumber(mcriddir.getNumberAsInteger() + 1);
                            mcridnext = mcriddir;
                        }
                    } catch (Exception e) {
                    }
                }
            }

            mytfmcrid = mcridnext.getId();
        }

        LOGGER.info("MCRID (TF) = " + mytfmcrid);

        // get the MCRObjectID from the selcet field (SE)
        String mysemcrid = getProperty(job.getRequest(), "se_mcrid");

        if (mysemcrid == null) {
            mysemcrid = "";
        } else {
            try {
                new MCRObjectID(mysemcrid);
            } catch (Exception e) {
                mysemcrid = "";
            }
        }

        LOGGER.info("MCRID (SE) = " + mysemcrid);

        // get the MCRObjectID from the relation field (RE)
        String myremcrid = getProperty(job.getRequest(), "re_mcrid");

        if (myremcrid == null) {
            myremcrid = "";
        } else {
            try {
                new MCRObjectID(myremcrid);
            } catch (Exception e) {
                myremcrid = "";
            }
        }

        LOGGER.info("MCRID (RE) = " + myremcrid);

        // appending parameter
        String extparm = getProperty(job.getRequest(), "extparm");
        LOGGER.info("EXTPARM = " + extparm);

        LOGGER.debug("Base URL : " + getBaseURL());

        // set the pages and language
        String pagedir = CONFIG.getString("MCR.editor_page_dir", "");
        StringBuffer sb = new StringBuffer();
        sb.append(pagedir).append("editor_form_").append(mystep).append('-').append(mytype);

        if (mylayout.length() != 0) {
            sb.append('-').append(mylayout);
        }

        sb.append(".xml");

        String myfile = sb.toString();
        String cancelpage = pagedir + CONFIG.getString("MCR.editor_page_cancel", "editor_cancel.xml");
        String deletepage = pagedir + CONFIG.getString("MCR.editor_page_delete", "editor_delete.xml");
        String usererrorpage = pagedir + CONFIG.getString("MCR.editor_page_error_user", "editor_error_user.xml");
        String mcriderrorpage = pagedir + CONFIG.getString("MCR.editor_page_error_mcrid", "editor_error_mcrid.xml");
        String storeerrorpage = pagedir + CONFIG.getString("MCR.editor_page_error_store", "editor_error_store.xml");
        String deleteerrorpage = pagedir + CONFIG.getString("MCR.editor_page_error_delete", "editor_error_delete.xml");

        // action WNEWOBJ - create a new object
        if (mytodo.equals("wnewobj")) {
            if (!AI.checkPermission("create-" + mytype)) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            // start changes for submitting xml templates
            Element root = new Element("mycoreobject");
            LOGGER.debug("calling buildXMLTemplate...");
            if (MCREditorServletHelper.buildXMLTemplate(job.getRequest(), root)) {
                MCRSessionMgr.getCurrentSession().put("wnewobj", root);
                params.put("XSL.editor.source.url", "session:wnewobj");
            } else {
                LOGGER.debug("XMLTemplate is empty");
                params.put("XSL.editor.source.new", "true");// old code
            }
            // end changes
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("mcrid", mytfmcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

            return;
        }

        // action WNEWDER - create a new derivate
        if (mytodo.equals("wnewder")) {
            if (!AI.checkPermission("create-" + mytype)) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            myremcrid = mysemcrid;
            mysemcrid = WFM.createDerivate(myremcrid, false);
            mytodo = "waddfile";
        }

        // action WADDFILE - create a new file in the derivate
        if (mytodo.equals("waddfile")) {
            if (!AI.checkPermission(myremcrid, "writewf")) {
                job.getResponse().sendRedirect(getBaseURL() + usererrorpage);

                return;
            }

            sb = new StringBuffer(pagedir);
            sb.append("editor_").append(mytype).append("_editor.xml");

            String fuhid = new MCRUploadHandlerMyCoRe(myremcrid, mysemcrid, "new", getBaseURL() + sb.toString()).getID();
            myfile = pagedir + "fileupload_new.xml";

            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("XSL.UploadID", fuhid);
            params.put("XSL.editor.source.new", "true");
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("mcrid", mysemcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            params.put("remcrid", myremcrid);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

            return;
        }

        // action WEDITOBJ - change the object in the workflow
        if (mytodo.equals("weditobj")) {
            org.jdom.Element rule = WFM.getRuleFromFile(mysemcrid, "writewf");
            if (!AI.checkPermission(rule)) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer();
            sb.append("file://").append(CONFIG.getString("MCR.editor_" + mytype + "_directory")).append('/').append(mysemcrid).append(".xml");

            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("XSL.editor.source.url", sb.toString());
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("mcrid", mysemcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

            return;
        }

        // action WDELOBJ - delete an object from the workflow
        if (mytodo.equals("wdelobj")) {
            org.jdom.Element rule = WFM.getRuleFromFile(mysemcrid, "deletewf");
            if (!AI.checkPermission(rule)) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer(pagedir);
            sb.append("editor_").append(mytype).append("_editor.xml");

            new MCRObjectID(mysemcrid);
            WFM.deleteMetadataObject(mytype, mysemcrid);

            List addr = WFM.getMailAddress(mytype, "wdelobj");

            if (addr.size() != 0) {
                String sender = WFM.getMailSender();
                String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
                String subject = "Automaticaly message from " + appl;
                StringBuffer text = new StringBuffer();
                text.append("Es wurde ein Objekt vom Typ ").append(mytype).append(" mit der ID ").append(mysemcrid).append(" aus dem Workflow gel�scht.");
                LOGGER.info(text.toString());

                try {
                    MCRMailer.send(sender, addr, subject, text.toString(), false);
                } catch (Exception ex) {
                    LOGGER.error("Can't send a mail to " + addr);
                }
            }

            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));

            return;
        }

        // action WDELDER - delete a derivate from the workflow
        if (mytodo.equals("wdelder")) {
            if (!AI.checkPermission(mysemcrid, "deletewf")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer(pagedir);
            sb.append("editor_").append(mytype).append("_editor.xml");

            new MCRObjectID(mysemcrid);
            WFM.deleteDerivateObject(mytype, mysemcrid);

            List addr = WFM.getMailAddress(mytype, "wdelder");

            if (addr.size() != 0) {
                String sender = WFM.getMailSender();
                String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
                String subject = "Automaticaly message from " + appl;
                StringBuffer text = new StringBuffer();
                text.append("Es wurde ein Derivate mit der ID ").append(mysemcrid).append(" aus dem Workflow gel�scht.");
                LOGGER.info(text.toString());

                try {
                    MCRMailer.send(sender, addr, subject, text.toString(), false);
                } catch (Exception ex) {
                    LOGGER.error("Can't send a mail to " + addr);
                }
            }

            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));

            return;
        }

        // action WCOMMIT - commit a object from the workflow to the server
        if (mytodo.equals("wcommit")) {
            org.jdom.Element rule = WFM.getRuleFromFile(mysemcrid, "commitdb");
            if (!AI.checkPermission(rule)) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            try {
                if (WFM.commitMetadataObject(mytype, mysemcrid)) {
                    WFM.deleteMetadataObject(mytype, mysemcrid);

                    List addr = WFM.getMailAddress(mytype, "wcommit");

                    if (addr.size() != 0) {
                        String sender = WFM.getMailSender();
                        String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
                        String subject = "Automaticaly message from " + appl;
                        StringBuffer text = new StringBuffer();
                        text.append("Es wurde ein Objekt vom Typ ").append(mytype).append(" mit der ID ").append(mysemcrid).append(" aus dem Workflow in das System geladen.");
                        LOGGER.info(text.toString());

                        try {
                            MCRMailer.send(sender, addr, subject, text.toString(), false);
                        } catch (Exception ex) {
                            LOGGER.error("Can't send a mail to " + addr);
                        }
                    }

                    sb = (new StringBuffer("MCR.type_")).append(mytype).append("_in");

                    String searchtype = CONFIG.getString(sb.toString(), mytype);
                    sb = new StringBuffer("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
                    sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(mysemcrid).append("\']");
                    myfile = sb.toString();
                } else {
                    myfile = storeerrorpage;
                }
            } catch (MCRActiveLinkException e) {
                generateActiveLinkErrorpage(job.getRequest(), job.getResponse(), "Error while commiting work to the server.", e);
                return;
            } catch (MCRException e) {
                myfile = storeerrorpage;
            }
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + myfile));

            return;
        }

        // action WEDITDER in the database
        if (mytodo.equals("weditder")) {
            if (!AI.checkPermission(myremcrid, "writewf")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer();
            sb.append(WFM.getDirectoryPath(mytype)).append(SLASH).append(mysemcrid).append(".xml");

            MCRDerivate der = new MCRDerivate();
            der.setFromURI(sb.toString());

            org.jdom.Element textfield = new org.jdom.Element("textfield");
            org.jdom.Element defa = new org.jdom.Element("default");
            defa.setText(der.getLabel());
            textfield.addContent(defa);
            MCRSessionMgr.getCurrentSession().put("weditder", textfield);
            sb = new StringBuffer();
            sb.append(getBaseURL()).append(pagedir).append("editor_form_editor-derivate.xml");

            Properties params = new Properties();
            params.put("XSL.editor.source.new", "true");
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("se_mcrid", mysemcrid);
            params.put("re_mcrid", myremcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));

            return;
        }

        // action WSETLABEL in the database
        if (mytodo.equals("wsetlabel")) {
            extparm = "####label####" + extparm;
            mytodo = "wsetfile";
        }

        // action WSETFILE in the database
        if (mytodo.equals("wsetfile")) {
            if (!AI.checkPermission(myremcrid, "writewf")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer();
            sb.append(WFM.getDirectoryPath(mytype)).append(SLASH).append(mysemcrid).append(".xml");

            MCRDerivate der = new MCRDerivate();
            der.setFromURI(sb.toString());

            if (extparm.startsWith("####main####")) {
                der.getDerivate().getInternals().setMainDoc(extparm.substring(mysemcrid.length() + 1 + 12, extparm.length()));
            }

            if (extparm.startsWith("####label####")) {
                der.setLabel(extparm.substring(13, extparm.length()));
            }

            byte[] outxml = MCRUtils.getByteArray(der.createXML());

            try {
                FileOutputStream out = new FileOutputStream(sb.toString());
                out.write(outxml);
                out.flush();
            } catch (IOException ex) {
                LOGGER.error("Exception while store to file " + sb.toString());
            }

            sb = new StringBuffer();
            sb.append(pagedir).append("editor_").append(mytype).append("_editor.xml");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));

            return;
        }

        // action WDELFILE in the database
        if (mytodo.equals("wdelfile")) {
            if (!AI.checkPermission(myremcrid, "deletewf")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            int all = 0;

            int i = extparm.indexOf("####nrall####");
            int j = 0;

            if (i != -1) {
                j = extparm.indexOf("####", i + 13);
                all = Integer.parseInt(extparm.substring(i + 13, j));
            }

            i = extparm.indexOf("####nrthe####");

            if (i != -1) {
                j = extparm.indexOf("####", i + 13);
                Integer.parseInt(extparm.substring(i + 13, j));
            }

            if (all > 1) {
                String derpath = WFM.getDirectoryPath(mytype);
                i = extparm.indexOf("####filename####");

                if (i != -1) {
                    String filename = extparm.substring(i + 16, extparm.length());

                    try {
                        File fi = new File(derpath, filename);
                        fi.delete();
                    } catch (Exception ex) {
                        LOGGER.warn("Can't remove file " + filename);
                    }
                }
            }

            sb = new StringBuffer();
            sb.append(getBaseURL()).append(pagedir).append("editor_").append(mytype).append("_editor.xml");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

            return;
        }

        // action SEDITOBJ in the database
        if (mytodo.equals("seditobj")) {
            if (!AI.checkPermission(mytfmcrid, "writedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mytfmcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            sb = new StringBuffer();
            sb.append(getBaseURL()).append("servlets/MCRQueryServlet").append("?XSL.Style=editor").append("&mode=ObjectMetadata").append("&type=").append(mytype).append("&hosts=").append(CONFIG.getString("MCR.editor_baseurl", "local")).append("&query=/mycoreobject%5b@ID=%22").append(mytfmcrid).append("%22%5d");

            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("XSL.editor.source.url", sb.toString());
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("mcrid", mytfmcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

            return;
        }

        // action SDELOBJ from the database
        if (mytodo.equals("sdelobj")) {
            if (!AI.checkPermission(mytfmcrid, "deletedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mytfmcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            MCRObject obj = new MCRObject();

            try {
                obj.deleteFromDatastore(mytfmcrid);
                myfile = deletepage;
            } catch (Exception e) {
                myfile = deleteerrorpage;
            }

            List addr = WFM.getMailAddress(mytype, "sdelobj");

            if (addr.size() != 0) {
                String sender = WFM.getMailSender();
                String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
                String subject = "Automaticaly message from " + appl;
                StringBuffer text = new StringBuffer();
                text.append("Es wurde ein Objekt vom Typ ").append(mytype).append(" mit der ID ").append(mytfmcrid).append(" aus dem Server gel�scht.");
                LOGGER.info(text.toString());

                try {
                    MCRMailer.send(sender, addr, subject, text.toString(), false);
                } catch (Exception ex) {
                    LOGGER.error("Can't send a mail to " + addr);
                }
            }

            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + myfile));

            return;
        }

        // action SDELDER from the database
        if (mytodo.equals("sdelder")) {
            if (!AI.checkPermission(myremcrid, "deletedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            MCRDerivate der = new MCRDerivate();

            try {
                der.deleteFromDatastore(mysemcrid);

                MCRObjectID ID = new MCRObjectID(myremcrid);
                sb = (new StringBuffer("MCR.type_")).append(ID.getTypeId()).append("_in");

                String searchtype = CONFIG.getString(sb.toString(), ID.getTypeId());
                sb = new StringBuffer();
                sb.append("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
                sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(myremcrid).append("\']");
                myfile = sb.toString();
            } catch (Exception e) {
                myfile = deleteerrorpage;
            }

            List addr = WFM.getMailAddress(mytype, "sdelder");

            if (addr.size() != 0) {
                String sender = WFM.getMailSender();
                String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
                String subject = "Automaticaly message from " + appl;
                StringBuffer text = new StringBuffer();
                text.append("Es wurde ein Derivate mit der ID ").append(mysemcrid).append(" des Objektes mit der ID ").append(mysemcrid).append(" aus dem Server gel�scht.");
                LOGGER.info(text.toString());

                try {
                    MCRMailer.send(sender, addr, subject, text.toString(), false);
                } catch (Exception ex) {
                    LOGGER.error("Can't send a mail to " + addr);
                }
            }

            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + myfile));

            return;
        }

        // action SNEWDER - create a new derivate
        if (mytodo.equals("snewder")) {
            if (!AI.checkPermission(myremcrid, "commitdb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            mysemcrid = WFM.createDerivate(myremcrid, true);
            mystep = "addder";
            mytodo = "saddfile";
        }

        // action SNEWFILE - create a new derivate
        if (mytodo.equals("snewfile")) {
            if (!AI.checkPermission(myremcrid, "commitdb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            String workdir = CONFIG.getString("MCR.editor_" + mytype + "_directory");
            File ndir = new File(workdir, mysemcrid);
            ndir.mkdir();
            mystep = "addfile";
            mytodo = "saddfile";
        }

        // action SADDFILE - create a new file in the derivate
        if (mytodo.equals("saddfile")) {
            if (!AI.checkPermission(myremcrid, "commitdb")) {
                job.getResponse().sendRedirect(getBaseURL() + usererrorpage);
                return;
            }

            sb = new StringBuffer(getBaseURL());
            sb.append("servlets/MCRStartEditorServlet?").append("se_mcrid=").append(mysemcrid).append("&re_mcrid=").append(myremcrid).append("&type=").append(mytype).append("&step=").append(mystep).append("&todo=scommitder");

            MCRUploadHandlerMyCoRe fuh = new MCRUploadHandlerMyCoRe(myremcrid, mysemcrid, "new", sb.toString());
            String fuhid = fuh.getID();
            myfile = pagedir + "fileupload_commit.xml";

            String base = getBaseURL() + myfile;
            Properties params = new Properties();
            params.put("XSL.UploadID", fuhid);
            params.put("XSL.editor.source.new", "true");
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("mcrid", mysemcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            params.put("remcrid", myremcrid);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));

            return;
        }

        // action SCOMMITDER in the database
        if (mytodo.equals("scommitder")) {
            if (!AI.checkPermission(myremcrid, "commitdb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }

            if (mytfmcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            // commit to the server
            boolean b = false;
            MCRObjectID ID = new MCRObjectID(myremcrid);

            if (mystep.indexOf("addfile") != -1) {
                String workdir = CONFIG.getString("MCR.editor_" + mytype + "_directory");
                File ndir = new File(workdir, mysemcrid);
                MCRFileImportExport.addFiles(ndir, mysemcrid);
                b = true;
            } else {
                b = WFM.commitDerivateObject(ID.getTypeId(), mysemcrid);
            }

            if (b) {
                WFM.deleteDerivateObject(ID.getTypeId(), mysemcrid);
                sb = (new StringBuffer("MCR.type_")).append(ID.getTypeId()).append("_in");

                String searchtype = CONFIG.getString(sb.toString(), ID.getTypeId());
                sb = new StringBuffer(getBaseURL());
                sb.append("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
                sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(myremcrid).append("\']");
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

                return;
            }

            WFM.deleteDerivateObject(ID.getTypeId(), mysemcrid);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + storeerrorpage));

            return;
        }

        // action SEDITDER in the database
        if (mytodo.equals("seditder")) {
            if (!AI.checkPermission(mysemcrid, "writedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            MCRDerivate der = new MCRDerivate();
            der.receiveFromDatastore(mysemcrid);

            org.jdom.Element textfield = new org.jdom.Element("textfield");
            org.jdom.Element defa = new org.jdom.Element("default");
            defa.setText(der.getLabel());
            textfield.addContent(defa);
            MCRSessionMgr.getCurrentSession().put("seditder", textfield);
            sb = new StringBuffer();
            sb.append(getBaseURL()).append(pagedir).append("editor_form_commit-derivate.xml");

            Properties params = new Properties();
            params.put("XSL.editor.source.new", "true");
            params.put("XSL.editor.cancel.url", getBaseURL() + cancelpage);
            params.put("se_mcrid", mysemcrid);
            params.put("re_mcrid", myremcrid);
            params.put("type", mytype);
            params.put("step", mystep);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));

            return;
        }

        // action SSETLABEL in the database
        if (mytodo.equals("ssetlabel")) {
            if (!AI.checkPermission(myremcrid, "writedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            MCRDerivate der = new MCRDerivate();
            der.receiveFromDatastore(mysemcrid);
            der.setLabel(extparm);

            try {
                der.updateXMLInDatastore();
            } catch (MCRException ex) {
                LOGGER.error("Exception while store to derivate " + mysemcrid);
            }

            MCRObjectID ID = new MCRObjectID(myremcrid);
            sb = new StringBuffer();
            sb = (new StringBuffer("MCR.type_")).append(ID.getTypeId()).append("_in");

            String searchtype = CONFIG.getString(sb.toString(), ID.getTypeId());
            sb = new StringBuffer(getBaseURL());
            sb.append("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
            sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(myremcrid).append("\']");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

            return;
        }

        // action SSETFILE in the database
        if (mytodo.equals("ssetfile")) {
            if (!AI.checkPermission(myremcrid, "writedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            MCRDerivate der = new MCRDerivate();
            der.receiveFromDatastore(mysemcrid);
            der.getDerivate().getInternals().setMainDoc(extparm);

            try {
                der.updateXMLInDatastore();
            } catch (MCRException ex) {
                LOGGER.error("Exception while store to derivate " + mysemcrid);
            }

            sb = new StringBuffer();
            sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(mysemcrid).append("/?hosts=local");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

            return;
        }

        // action SDELFILE in the database
        if (mytodo.equals("sdelfile")) {
            if (!AI.checkPermission(myremcrid, "deletedb")) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
                return;
            }
            if (mysemcrid.length() == 0) {
                job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
                return;
            }

            int all = 0;

            int i = extparm.indexOf("####nrall####");
            int j = 0;

            if (i != -1) {
                j = extparm.indexOf("####", i + 13);
                all = Integer.parseInt(extparm.substring(i + 13, j));
            }

            i = extparm.indexOf("####nrthe####");

            if (i != -1) {
                j = extparm.indexOf("####", i + 13);
                Integer.parseInt(extparm.substring(i + 13, j));
            }

            if (all > 1) {
                i = extparm.indexOf("####filename####");

                if (i != -1) {
                    String filename = extparm.substring(i + 16, extparm.length());

                    try {
                        MCRDirectory rootdir = MCRDirectory.getRootDirectory(mysemcrid);
                        rootdir.getChildByPath(filename).delete();
                    } catch (Exception ex) {
                        LOGGER.warn("Can't remove file " + filename);
                    }
                }
            }

            sb = new StringBuffer();
            sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(mysemcrid).append("/?hosts=local");
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));

            return;
        }

        sb = new StringBuffer();
        sb.append(getBaseURL()).append("index.html");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * Builds an url that can be used to redirect the client browser to another
     * page, including http request parameters. The request parameters will be
     * encoded as http get request.
     * 
     * @param baseURL
     *            the base url of the target webpage
     * @param parameters
     *            the http request parameters
     */
    private String buildRedirectURL(String baseURL, Properties parameters) {
        StringBuffer redirectURL = new StringBuffer(baseURL);
        boolean first = true;

        for (Enumeration e = parameters.keys(); e.hasMoreElements();) {
            if (first) {
                redirectURL.append("?");
                first = false;
            } else {
                redirectURL.append("&");
            }

            String name = (String) (e.nextElement());
            String value = null;

            try {
                value = URLEncoder.encode(parameters.getProperty(name), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                value = parameters.getProperty(name);
            }

            redirectURL.append(name).append("=").append(value);
        }

        LOGGER.debug("Sending redirect to " + redirectURL.toString());

        return redirectURL.toString();
    }
}
