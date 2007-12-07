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
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRMetaNBN;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.fileupload.MCRSWFUploadHandlerIFS;
import org.mycore.frontend.fileupload.MCRUploadHandlerMyCoRe;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;
import org.mycore.services.urn.MCRURNManager;

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

    private static final long serialVersionUID = 1L;

    // The configuration
    protected static Logger LOGGER = Logger.getLogger(MCRStartEditorServlet.class);

    // The workflow manager
    protected static MCRSimpleWorkflowManager WFM = null;

    // The file slash
    protected static String SLASH = System.getProperty("file.separator");;

    // static pages
    protected static String pagedir = CONFIG.getString("MCR.SWF.PageDir", "");

    protected static String cancelpage = pagedir + CONFIG.getString("MCR.SWF.PageCancel", "editor_cancel.xml");

    protected static String deletepage = pagedir + CONFIG.getString("MCR.SWF.PageDelete", "editor_delete.xml");

    protected static String usererrorpage = pagedir + CONFIG.getString("MCR.SWF.PageErrorUser", "editor_error_user.xml");

    protected static String mcriderrorpage = pagedir + CONFIG.getString("MCR.SWF.PageErrorMcrid", "editor_error_mcrid.xml");

    protected static String storeerrorpage = pagedir + CONFIG.getString("MCR.SWF.PageErrorStore", "editor_error_store.xml");

    protected static String deleteerrorpage = pagedir + CONFIG.getString("MCR.SWF.PageErrorDelete", "editor_error_delete.xml");

    // common data
    protected static class CommonData {
        protected String mystep = null; // the special step for todo

        protected String mytype = null; // the metadata type

        protected String myfile = null; // the formular file to be called

        protected String mytfmcrid = null; // the metadata ID

        protected String mysemcrid = null; // the metadata ID

        protected MCRObjectID mcrmysemcrid = null;

        protected String myremcrid = null; // the metadata ID

        protected String extparm = null; // the extra parameter
    }

    protected static int number_distance = 1;

    /** Initialisation of the servlet */
    public void init() throws ServletException {
        super.init();

        // Workflow Manager
        WFM = MCRSimpleWorkflowManager.instance();
        number_distance = CONFIG.getInt("MCR.Metadata.ObjectID.NumberDistance", 1);

    }

    /**
     * This method overrides doGetPost of MCRServlet. <br />
     * <br />
     * The <b>todo </b> value corresponds with <b>tf_mcrid</b> or <b>se_mcridor</b>
     * value and with the type of the data model for the permissions that the
     * user need. For some actions you need a third value of <b>re_mcrid</b>
     * for relations (object - derivate). <br />
     * 
     * <li>If the permission is not correct it calls
     * <em>editor_error_user.xml</em>.</li>
     * <br />
     * <li>If the MCRObjectID is not correct it calls
     * <em>editor_error_mcrid.xml</em>.</li>
     * <br />
     * <li>If a store error is occurred it calls <em>editor_error_store.xml</em>.
     * </li>
     * <br />
     * <li>If <b>CANCEL </b> was pressed it calls <em>editor_cancel.xml</em>.
     * </li>
     * <br />
     * <li>If the permission is correct it starts the file editor_form_
     * <em>step-type</em> .xml.</li>
     * <br />
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        
        if (MCRWebsiteWriteProtection.printInfoPageIfNoAccess(job.getRequest(), job.getResponse()))
            return;
        
        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        CommonData cd = new CommonData();

        // get the current language
        String mylang = mcrSession.getCurrentLanguage();
        LOGGER.debug("LANG = " + mylang);

        // read the parameter
        // get the step
        cd.mystep = getProperty(job.getRequest(), "step");
        if (cd.mystep == null) {
            cd.mystep = "";
        }
        LOGGER.debug("STEP = " + cd.mystep);

        // get the type
        cd.mytype = getProperty(job.getRequest(), "type");
        if (cd.mytype == null) {
            cd.mytype = CONFIG.getString("MCR.SWF.Project.Type", "document");
        }
        if (cd.mytype.length() == 0) {
            cd.mytype = CONFIG.getString("MCR.SWF.Project.Type", "document");
        }
        LOGGER.debug("TYPE = " + cd.mytype);

        // get the layout
        String mylayout = getProperty(job.getRequest(), "layout");
        if (mylayout == null) {
            mylayout = "";
        }
        LOGGER.debug("LAYOUT = " + mylayout);

        // get what is to do
        String mytodo = getProperty(job.getRequest(), "todo");
        if ((mytodo == null) || ((mytodo = mytodo.trim()).length() == 0)) {
            mytodo = "wrongtodo";
        }
        LOGGER.debug("TODO = " + mytodo);

        // get the MCRObjectID from the text filed (TF)
        cd.mytfmcrid = getProperty(job.getRequest(), "tf_mcrid");
        try {
            new MCRObjectID(cd.mytfmcrid);
        } catch (Exception e) {
            cd.mytfmcrid = "";
        }

        if ((cd.mytfmcrid == null) || ((cd.mytfmcrid = cd.mytfmcrid.trim()).length() == 0)) {
            String myproject = CONFIG.getString("MCR.SWF.Project.ID." + cd.mytype, "MCR");
            cd.mytfmcrid = getNextMCRTFID(myproject, cd.mytype);
        }
        LOGGER.debug("MCRID (TF) = " + cd.mytfmcrid);

        // get the MCRObjectID from the select field (SE)
        cd.mysemcrid = getProperty(job.getRequest(), "se_mcrid");
        if (cd.mysemcrid == null) {
            cd.mysemcrid = "";
        } else {
            try {
                cd.mcrmysemcrid = new MCRObjectID(cd.mysemcrid);
            } catch (Exception e) {
                cd.mysemcrid = "";
            }
        }
        LOGGER.debug("MCRID (SE) = " + cd.mysemcrid);

        // get the MCRObjectID from the relation field (RE)
        cd.myremcrid = getProperty(job.getRequest(), "re_mcrid");
        if (cd.myremcrid == null) {
            cd.myremcrid = "";
        } else {
            try {
                new MCRObjectID(cd.myremcrid);
            } catch (Exception e) {
                cd.myremcrid = "";
            }
        }
        LOGGER.debug("MCRID (RE) = " + cd.myremcrid);

        // appending parameter
        cd.extparm = getProperty(job.getRequest(), "extparm");
        LOGGER.info("EXTPARM = " + cd.extparm);

        LOGGER.debug("Base URL : " + getBaseURL());

        // set the pages
        StringBuffer sb = new StringBuffer();
        sb.append(pagedir).append("editor_form_").append(cd.mystep).append('-').append(cd.mytype);
        if (mylayout.length() != 0) {
            sb.append('-').append(mylayout);
        }
        cd.myfile = sb.append(".xml").toString();

        // call method named like todo
        Method meth[] = this.getClass().getMethods();
        for (int i = 0; i < meth.length; i++) {
            LOGGER.debug("Methods for SWF " + meth[i].getName());
        }
        try {
            Method method = this.getClass().getMethod(mytodo, new Class[] { job.getClass(), cd.getClass() });
            method.invoke(this, new Object[] { job, cd });
            return;
        } catch (Exception e) {
            LOGGER.error("Error while execution of method " + mytodo);
            e.printStackTrace();
        }

        sb = new StringBuffer();
        sb.append(getBaseURL()).append("index.html");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * This method return a next new MCRObjectID for the given type and project
     * ID.
     * 
     * @param projectid
     *            The MCRObjectID project ID
     * @type type The MCRObjectID type
     * @return the next free MCRObject for the given parameter
     */
    protected final String getNextMCRTFID(String myproject, String mytype) {
        if ((myproject == null) || (myproject.trim().length() == 0) || (myproject.equals("MCR"))) {
            myproject = CONFIG.getString("MCR.SWF.Project.ID", "MCR");
        }
        if ((mytype == null) || (mytype.trim().length() == 0) || (mytype.equals("MCR"))) {
            mytype = "document";
        }

        myproject = myproject + "_" + mytype;

        MCRObjectID mcridnext = new MCRObjectID();
        mcridnext.setNextFreeId(myproject);

        String workdir = CONFIG.getString("MCR.SWF.Directory." + mytype, "/");
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
                        int mylastnumber = mcriddir.getNumberAsInteger() + 1;
                        while ((mylastnumber % number_distance) != 0) {
                            mylastnumber += 1;
                        }
                        mcriddir.setNumber(mylastnumber);
                        mcridnext = mcriddir;
                    }
                } catch (Exception e) {
                }
            }
        }

        return mcridnext.getId();
    }

    /**
     * The method start the editor add a file to a derivate object that is
     * stored in the server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void saddfile(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid, "writedb")) {
            job.getResponse().sendRedirect(getBaseURL() + usererrorpage);
            return;
        }

        StringBuffer sb = new StringBuffer(getBaseURL()).append("receive/").append(cd.myremcrid);
        MCRSWFUploadHandlerIFS fuh = new MCRSWFUploadHandlerIFS(cd.myremcrid, cd.mysemcrid, sb.toString());
        String fuhid = fuh.getID();
        cd.myfile = pagedir + "fileupload_commit.xml";

        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("mcrid", cd.mysemcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        params.put("remcrid", cd.myremcrid);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method add a new NBN to the dataset with type <b>document</b> or
     * <b>disshab</b>. The access right is writedb.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void saddnbn(MCRServletJob job, CommonData cd) throws Exception {
        // access right
        if (!MCRAccessManager.checkPermission(cd.mysemcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }
        // check type
        if (cd.mytype.equals("document") || cd.mytype.equals("disshab")) {
            MCRObject obj = new MCRObject();
            obj.receiveFromDatastore(cd.mysemcrid);
            MCRMetaElement elm = obj.getMetadataElement("nbns");
            if (elm == null) {
                String urn = MCRURNManager.buildURN("UBL");
                MCRMetaNBN nbn = new MCRMetaNBN("metadata", "nbn", 0, urn);
                ArrayList<MCRMetaInterface> list = new ArrayList<MCRMetaInterface>();
                elm = new MCRMetaElement("de", "MCRMetaNBN", "nbns", true, false, list);
                elm.addMetaObject(nbn);
                obj.getMetadata().setMetadataElement(elm, "nbns");
                try {
                    obj.updateInDatastore();
                    MCRURNManager.assignURN(urn, obj.getId().toString());
                } catch (MCRActiveLinkException e) {
                    LOGGER.warn("Can't store NBN for " + cd.mysemcrid);
                    e.printStackTrace();
                }
                LOGGER.info("Add the NBN " + urn);
            } else {
                LOGGER.warn("The NBN already exists for " + cd.mysemcrid);
            }

        }
        // back to the metadata view
        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append("receive/").append(cd.mysemcrid);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * The method delete a derivate object that is stored in the server. The
     * method use the input parameter: <b>type</b>,<b>step</b> and
     * <b>tf_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void sdelder(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid, "deletedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        MCRDerivate der = new MCRDerivate();

        try {
            der.deleteFromDatastore(cd.mysemcrid);

            new MCRObjectID(cd.myremcrid);
            StringBuffer sb = new StringBuffer();
            sb.append("receive/").append(cd.myremcrid);
            cd.myfile = sb.toString();
        } catch (Exception e) {
            cd.myfile = deleteerrorpage;
        }

        List <String>addr = WFM.getMailAddress(cd.mytype, "sdelder");

        if (addr.size() != 0) {
            String sender = WFM.getMailSender();
            String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
            String subject = "Automatically generated message from " + appl;
            StringBuffer text = new StringBuffer();
            text.append("The derivate with ID ").append(cd.mysemcrid).append(" from the object with ID ").append(cd.mysemcrid).append(
                    " was removed from server.");
            LOGGER.info(text.toString());

            try {
                MCRMailer.send(sender, addr, subject, text.toString(), false);
            } catch (Exception ex) {
                LOGGER.error("Can't send a mail to " + addr);
            }
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + cd.myfile));
    }

    /**
     * The method delete a file from a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void sdelfile(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid, "deletedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        int all = 0;

        int i = cd.extparm.indexOf("####nrall####");
        int j = 0;

        if (i != -1) {
            j = cd.extparm.indexOf("####", i + 13);
            all = Integer.parseInt(cd.extparm.substring(i + 13, j));
        }

        i = cd.extparm.indexOf("####nrthe####");

        if (i != -1) {
            j = cd.extparm.indexOf("####", i + 13);
            Integer.parseInt(cd.extparm.substring(i + 13, j));
        }

        if (all > 1) {
            i = cd.extparm.indexOf("####filename####");

            if (i != -1) {
                String filename = cd.extparm.substring(i + 16, cd.extparm.length());

                try {
                    MCRDirectory rootdir = MCRDirectory.getRootDirectory(cd.mysemcrid);
                    rootdir.getChildByPath(filename).delete();
                } catch (Exception ex) {
                    LOGGER.warn("Can't remove file " + filename, ex);
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(cd.mysemcrid).append("/?hosts=local");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * The method delete a metadata object that is stored in the server. The
     * method use the input parameter: <b>type</b>,<b>step</b> and
     * <b>tf_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void sdelobj(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.mytfmcrid, "deletedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mytfmcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        MCRObject obj = new MCRObject();

        try {
            obj.deleteFromDatastore(cd.mytfmcrid);
            cd.myfile = deletepage;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            } else {
                LOGGER.error(e.getMessage());
            }
            cd.myfile = deleteerrorpage;
        }

        List <String>addr = WFM.getMailAddress(cd.mytype, "sdelobj");

        if (addr.size() != 0) {
            String sender = WFM.getMailSender();
            String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
            String subject = "Automaticaly message from " + appl;
            StringBuffer text = new StringBuffer();
            text.append("The object with type ").append(cd.mytype).append(" with ID ").append(cd.mytfmcrid).append(" was removed from server.");
            LOGGER.info(text.toString());

            try {
                MCRMailer.send(sender, addr, subject, text.toString(), false);
            } catch (Exception ex) {
                LOGGER.error("Can't send a mail to " + addr);
            }
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + cd.myfile));
    }

    /**
     * The method start the editor to modify ACL of a metadata object that is
     * stored in the server. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>tf_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void seditacl(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.mysemcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        // read object
        MCRObjectService service = new MCRObjectService();
        List <String>permlist = AI.getPermissionsForID(cd.mysemcrid);
        for (int i = 0; i < permlist.size(); i++) {
            org.jdom.Element ruleelm = AI.getRule(cd.mysemcrid, (String) permlist.get(i));
            ruleelm = normalizeACLforSWF(ruleelm);
            service.addRule((String) permlist.get(i), ruleelm);
        }
        org.jdom.Element serviceelm = service.createXML();
        if (LOGGER.isDebugEnabled()) {
            org.jdom.Document dof = new org.jdom.Document();
            dof.addContent(serviceelm);
            byte[] xml = MCRUtils.getByteArray(dof);
            System.out.println(new String(xml));
        }

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.put("service", serviceelm);
        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();
        params.put("sourceUri", "session:service");
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("mcrid", cd.mysemcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * Normalize the ACL to use in the SWF ACL editor. Some single conditions
     * are one step to high in the hierarchy of the condition tree. This method
     * move it down and normalized the output.
     * 
     * @param ruleelm
     *            The XML access condition from the ACL system
     */
    @SuppressWarnings("unchecked")
    private final org.jdom.Element normalizeACLforSWF(org.jdom.Element ruleelm) {
        if (LOGGER.isDebugEnabled()) {
            try {
                MCRUtils.writeElementToSysout(ruleelm);
            } catch (Exception e) {
                LOGGER.warn("Can't write ACL Element input for SWF.");
            }
        }
        // build new condition element
        org.jdom.Element newcondition = new org.jdom.Element("condition");
        newcondition.setAttribute("format", "xml");
        // build new boolean AND element
        org.jdom.Element newwrapperand = new org.jdom.Element("boolean");
        newwrapperand.setAttribute("operator", "and");
        newcondition.addContent(newwrapperand);
        // build new boolean TRUE element
        org.jdom.Element newtrue = new org.jdom.Element("boolean");
        newtrue.setAttribute("operator", "true");
        // check rule
        if (ruleelm == null) {
            LOGGER.warn("Rule element is null.");
            return newcondition;
        }
        try {
            // check of boolean AND element
            org.jdom.Element oldwrapperand = ruleelm.getChild("boolean");
            if (oldwrapperand == null) {
                return newcondition;
            }

            org.jdom.Element newuser = (org.jdom.Element) newtrue.clone();
            newuser.detach();
            org.jdom.Element newdate = (org.jdom.Element) newtrue.clone();
            newdate.detach();
            org.jdom.Element newip = (org.jdom.Element) newtrue.clone();
            newip.detach();
            org.jdom.Element newelm = null;

            List<org.jdom.Element> parts = oldwrapperand.getChildren();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 2)
                    break;
                org.jdom.Element oldelm = (org.jdom.Element) parts.get(i).detach();
                // if (oldelm.getChildren().size() == 0)
                // continue;
                if (oldelm.getName().equals("condition")) {
                    org.jdom.Element newwrapper = new org.jdom.Element("boolean");
                    newwrapper.setAttribute("operator", "or");
                    newwrapper.addContent(oldelm);
                    newelm = newwrapper;
                } else {
                    newelm = oldelm;
                }
                String testfield = "";
                List<org.jdom.Element> innercond = newelm.getChildren();
                for (int j = 0; j < innercond.size(); j++) {
                    org.jdom.Element cond = (org.jdom.Element) innercond.get(j);
                    if (cond.getName().equals("condition")) {
                        testfield = cond.getAttributeValue("field");
                    }
                }
                if (testfield.equals("user") || testfield.equals("group")) {
                    newuser = newelm;
                }
                if (testfield.equals("date")) {
                    newdate = newelm;
                }
                if (testfield.equals("ip")) {
                    newip = newelm;
                }
            }
            newwrapperand.addContent(newuser.detach());
            newwrapperand.addContent(newdate.detach());
            newwrapperand.addContent(newip.detach());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (LOGGER.isDebugEnabled()) {
            try {
                MCRUtils.writeElementToSysout(newcondition);
            } catch (Exception e) {
                LOGGER.warn("Can't write ACL Element output for SWF.");
            }
        }
        return newcondition;
    }

    /**
     * The method start the editor to modify a derivate object that is stored in
     * the server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>se_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void seditder(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.mysemcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        StringBuffer sb = new StringBuffer();
        Properties params = new Properties();
        sb.append("request:receive/").append(cd.mysemcrid).append("?XSL.Style=editor");
        params.put("sourceUri", sb.toString());
        sb = new StringBuffer();
        sb.append(getBaseURL()).append("receive/").append(cd.myremcrid);
        params.put("cancelUrl", sb.toString());
        params.put("se_mcrid", cd.mysemcrid);
        params.put("re_mcrid", cd.myremcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        sb = new StringBuffer();
        sb.append(getBaseURL()).append(pagedir).append("editor_form_commit-derivate.xml");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));
    }

    /**
     * The method start the editor to modify a metadata object that is stored in
     * the server. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>tf_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void seditobj(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.mytfmcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mytfmcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        StringBuffer sb = new StringBuffer();
        // TODO: should transform mcrobject and use "session:" to save roundtrip
        Properties params = new Properties();
        sb.append("request:receive/").append(cd.mytfmcrid).append("?XSL.Style=editor");
        params.put("sourceUri", sb.toString());
        sb = new StringBuffer();
        sb.append(getBaseURL()).append("receive/").append(cd.mytfmcrid);
        params.put("cancelUrl", sb.toString());
        params.put("mcrid", cd.mytfmcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        String base = getBaseURL() + cd.myfile;
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method set a new derivate object that is stored in the server. The
     * method use the input parameter: <b>type</b>,<b>step</b> <b>se_mcrid</b>
     * and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void snewder(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        cd.mystep = "addfile";
        cd.mysemcrid = WFM.getNextDrivateID(new MCRObjectID(cd.myremcrid)).getId();
        saddfile(job, cd);
    }

    /**
     * The method set the main file of a derivate object that is stored in the
     * server. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void ssetfile(MCRServletJob job, CommonData cd) throws IOException {
        if (!MCRAccessManager.checkPermission(cd.myremcrid, "writedb")) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        MCRDerivate der = new MCRDerivate();
        der.receiveFromDatastore(cd.mysemcrid);
        der.getDerivate().getInternals().setMainDoc(cd.extparm);

        try {
            der.updateXMLInDatastore();
        } catch (MCRException ex) {
            LOGGER.error("Exception while store to derivate " + cd.mysemcrid);
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append("servlets/MCRFileNodeServlet/").append(cd.mysemcrid).append("/?hosts=local");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * The method start the file upload to add to the metadata object that is
     * stored in the workflow. The method use the input parameter: <b>type</b>,
     * <b>step</b>, <b>re_mcrid</b> and <b>se_mcrid</b>. Access rights must
     * be 'create-'type.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void waddfile(MCRServletJob job, CommonData cd) throws IOException {
        if (!AI.checkPermission("create-" + cd.mytype)) {
            job.getResponse().sendRedirect(getBaseURL() + usererrorpage);
            return;
        }

        StringBuffer sb = new StringBuffer(pagedir);
        sb.append("editor_").append(cd.mytype).append("_editor.xml");

        String fuhid = new MCRUploadHandlerMyCoRe(cd.myremcrid, cd.mysemcrid, "new", getBaseURL() + sb.toString()).getID();
        cd.myfile = pagedir + "fileupload_new.xml";

        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();
        params.put("XSL.UploadID", fuhid);
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("XSL.target.param.1", "method=formBasedUpload");
        params.put("XSL.target.param.2", "uploadId=" + fuhid);
        params.put("mcrid", cd.mysemcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        params.put("remcrid", cd.myremcrid);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method commit a object including all derivates that is stored in the
     * workflow to the server. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>se_mcrid</b>. Access rights must be 'writedb'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wcommit(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.mysemcrid, "writedb");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        try {
            if (WFM.commitMetadataObject(cd.mytype, cd.mysemcrid)) {
                WFM.deleteMetadataObject(cd.mytype, cd.mysemcrid);

                List <String>addr = WFM.getMailAddress(cd.mytype, "wcommit");

                if (addr.size() != 0) {
                    String sender = WFM.getMailSender();
                    String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
                    String subject = "Automaticaly message from " + appl;
                    StringBuffer text = new StringBuffer();
                    text.append("The object of type ").append(cd.mytype).append(" with ID ").append(cd.mysemcrid).append(
                            " was commited from workflow to the server.");
                    LOGGER.info(text.toString());

                    try {
                        MCRMailer.send(sender, addr, subject, text.toString(), false);
                    } catch (Exception ex) {
                        LOGGER.error("Can't send a mail to " + addr);
                    }
                }

                StringBuffer sb = new StringBuffer("receive/").append(cd.mysemcrid);
                cd.myfile = sb.toString();
            } else {
                cd.myfile = storeerrorpage;
            }
        } catch (MCRActiveLinkException e) {
            try {
                generateActiveLinkErrorpage(job.getRequest(), job.getResponse(), "Error while commiting work to the server.", e);
                return;
            } catch (Exception se) {
                cd.myfile = storeerrorpage;
            }
        } catch (MCRException e) {
            cd.myfile = storeerrorpage;
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + cd.myfile));
    }

    /**
     * The method delete a derivate from an object that is stored in the
     * workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wdelder(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.myremcrid, "deletewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        StringBuffer sb = new StringBuffer(pagedir);
        sb.append("editor_").append(cd.mytype).append("_editor.xml");

        new MCRObjectID(cd.mysemcrid);
        WFM.deleteDerivateObject(cd.mytype, cd.mysemcrid);

        List addr = WFM.getMailAddress(cd.mytype, "wdelder");

        if (addr.size() != 0) {
            String sender = WFM.getMailSender();
            String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
            String subject = "Automaticaly message from " + appl;
            StringBuffer text = new StringBuffer();
            text.append("The derivate with ID ").append(cd.mysemcrid).append(" was removed from workflow.");
            LOGGER.info(text.toString());

            try {
                MCRMailer.send(sender, addr, subject, text.toString(), false);
            } catch (Exception ex) {
                LOGGER.error("Can't send a mail to " + addr);
            }
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));
    }

    /**
     * The method delete a file from the derivate object that is stored in the
     * workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>se_mcrid</b> and <b>re_mcrid</b>. Access rights must be 'deletewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wdelfile(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.myremcrid, "deletewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        int all = 0;

        int i = cd.extparm.indexOf("####nrall####");
        int j = 0;

        if (i != -1) {
            j = cd.extparm.indexOf("####", i + 13);
            all = Integer.parseInt(cd.extparm.substring(i + 13, j));
        }

        i = cd.extparm.indexOf("####nrthe####");

        if (i != -1) {
            j = cd.extparm.indexOf("####", i + 13);
            Integer.parseInt(cd.extparm.substring(i + 13, j));
        }

        if (all > 1) {
            String derpath = WFM.getDirectoryPath(cd.mytype);
            i = cd.extparm.indexOf("####filename####");

            if (i != -1) {
                String filename = cd.extparm.substring(i + 16, cd.extparm.length());

                try {
                    File fi = new File(derpath, filename);
                    fi.delete();
                } catch (Exception ex) {
                    LOGGER.warn("Can't remove file " + filename);
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append(pagedir).append("editor_").append(cd.mytype).append("_editor.xml");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

    /**
     * The method delete a metadata object that is stored in the workflow. The
     * method use the input parameter: <b>type</b>,<b>step</b> and
     * <b>se_mcrid</b>. Access rights must be 'deletewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wdelobj(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.mysemcrid, "deletewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        StringBuffer sb = new StringBuffer(pagedir);
        sb.append("editor_").append(cd.mytype).append("_editor.xml");

        new MCRObjectID(cd.mysemcrid);
        WFM.deleteMetadataObject(cd.mytype, cd.mysemcrid);

        List <String>addr = WFM.getMailAddress(cd.mytype, "wdelobj");

        if (addr.size() != 0) {
            String sender = WFM.getMailSender();
            String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
            String subject = "Automaticaly message from " + appl;
            StringBuffer text = new StringBuffer();
            text.append("The object of type ").append(cd.mytype).append(" with ID ").append(cd.mysemcrid).append(" was removed from the workflow.");
            LOGGER.info(text.toString());

            try {
                MCRMailer.send(sender, addr, subject, text.toString(), false);
            } catch (Exception ex) {
                LOGGER.error("Can't send a mail to " + addr);
            }
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));
    }

    /**
     * The method start the editor to modify the metadata object ACL that is
     * stored in the workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>se_mcrid</b>. Access rights must be 'writewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void weditacl(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.mysemcrid, "writewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        // read file
        StringBuffer sb = new StringBuffer();
        sb.append(CONFIG.getString("MCR.SWF.Directory." + cd.mcrmysemcrid.getTypeId())).append(SLASH).append(cd.mysemcrid).append(".xml");
        org.jdom.Element service = null;
        try {
            File fi = new File(sb.toString());
            if (fi.isFile() && fi.canRead()) {
                MCRObject obj = new MCRObject();
                obj.setFromURI(sb.toString());
                service = obj.getService().createXML();
            } else {
                LOGGER.error("Can't read file " + sb.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGGER.error("Can't read file " + sb.toString());
        }

        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.put("service", service);
        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();
        params.put("sourceUri", "session:service");
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("mcrid", cd.mysemcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method start the editor modify derivate metadata that is stored in
     * the workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>re_mcrid</b> and <b>se_mcrid</b>. Access rights must be 'writewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void weditder(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.myremcrid, "writewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        Properties params = new Properties();
        StringBuffer sb = new StringBuffer();
        sb.append("file://").append(WFM.getDirectoryPath(cd.mytype)).append(SLASH).append(cd.mysemcrid).append(".xml");
        params.put("sourceUri", sb.toString());
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("se_mcrid", cd.mysemcrid);
        params.put("re_mcrid", cd.myremcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        sb = new StringBuffer();
        sb.append(getBaseURL()).append(pagedir).append("editor_form_editor-derivate.xml");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(sb.toString(), params)));
    }

    /**
     * The method start the editor to modify a metadata object that is stored in
     * the workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>se_mcrid</b>. Access rights must be 'writewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void weditobj(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.mysemcrid, "writewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        File wfFile = new File(CONFIG.getString("MCR.SWF.Directory." + cd.mytype), cd.mysemcrid + ".xml");
        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();
        params.put("sourceUri", wfFile.toURI().toString());
        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("mcrid", cd.mysemcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method build a new derivate and start the file upload to add to the
     * metadata object that is stored in the workflow. The method use the input
     * parameter: <b>type</b>, <b>step</b>, <b>re_mcrid</b> and <b>se_mcrid</b>.
     * Access rights must be 'create-'type.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wnewder(MCRServletJob job, CommonData cd) throws IOException {
        if (!AI.checkPermission("create-" + cd.mytype)) {
            (new MCRObjectID()).decrementOneFreeId((new MCRObjectID(cd.mytfmcrid)).getBase());
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }

        cd.myremcrid = cd.mysemcrid;
        cd.mysemcrid = WFM.getNextDrivateID(new MCRObjectID(cd.myremcrid)).getId();
        waddfile(job, cd);
    }

    /**
     * The method start the editor to create new metadata object that will be
     * stored in the workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * and <b>tf_mcrid</b>. Access rights must be 'create-'type.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wnewobj(MCRServletJob job, CommonData cd) throws IOException {
        if (!AI.checkPermission("create-" + cd.mytype)) {
            (new MCRObjectID()).decrementOneFreeId((new MCRObjectID(cd.mytfmcrid)).getBase());
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }

        String base = getBaseURL() + cd.myfile;
        Properties params = new Properties();

        // start changes for submitting xml templates
        LOGGER.debug("calling buildXMLTemplate...");
        Enumeration<?> e = job.getRequest().getParameterNames();
        HashMap<String, String> templatePairs = new HashMap<String, String>();
        while (e.hasMoreElements()) {
            String name = (String) (e.nextElement());
            String value = job.getRequest().getParameter(name);
            if (name.startsWith("_xml_")) {
                templatePairs.put(URLEncoder.encode(name.substring(5), "UTF-8"), URLEncoder.encode(value, "UTF-8"));
            } else {
                params.put(name, value);

            }
        }
        if (templatePairs.size() > 0) {
            StringBuilder sb = new StringBuilder("buildxml:_rootName_=mycoreobject");
            for (Map.Entry<String, String> entry : templatePairs.entrySet()) {
                sb.append('&').append(entry.getKey()).append('=').append(entry.getValue());
            }
            params.put("sourceUri", sb.toString());
        } else {
            LOGGER.debug("XMLTemplate is empty");
        }
        // end changes

        params.put("cancelUrl", getBaseURL() + cancelpage);
        params.put("mcrid", cd.mytfmcrid);
        params.put("type", cd.mytype);
        params.put("step", cd.mystep);
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(buildRedirectURL(base, params)));
    }

    /**
     * The method start the editor to modify a derivate object that is stored in
     * the workflow. The method use the input parameter: <b>type</b>,<b>step</b>
     * <b>re_mcrid</b> and <b>se_mcrid</b>. Access rights must be 'writewf'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wsetfile(MCRServletJob job, CommonData cd) throws IOException {
        org.jdom.Element rule = WFM.getRuleFromFile(cd.myremcrid, "writewf");
        if (!AI.checkPermission(rule)) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + usererrorpage));
            return;
        }
        if (cd.mysemcrid.length() == 0) {
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + mcriderrorpage));
            return;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(WFM.getDirectoryPath(cd.mytype)).append(SLASH).append(cd.mysemcrid).append(".xml");

        MCRDerivate der = new MCRDerivate();
        der.setFromURI(sb.toString());

        if (cd.extparm.startsWith("####main####")) {
            der.getDerivate().getInternals().setMainDoc(cd.extparm.substring(cd.mysemcrid.length() + 1 + 12, cd.extparm.length()));
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
        sb.append(pagedir).append("editor_").append(cd.mytype).append("_editor.xml");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + sb.toString()));
    }

    /**
     * This method implements the error exit to <em>index.xml</em> if the
     * action method was not found or has an error. It implements the TODO
     * 'wrongtodo'.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void wrongtodo(MCRServletJob job, CommonData cd) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(getBaseURL()).append("index.html");
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
    }

}
