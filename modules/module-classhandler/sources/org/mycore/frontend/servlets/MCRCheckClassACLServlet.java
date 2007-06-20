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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.mycore.user.MCRUserMgr;

/**
 * The servlet store the MCREditorServlet output XML in a file of a MCR type
 * dependencies directory, check it dependence of the MCR type and store the XML
 * in a file in this directory or if an error was occured start the editor again
 * with <b>todo </b> <em>repair</em>.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCheckClassACLServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    // The logger
    protected static Logger LOGGER = Logger.getLogger(MCRCheckClassACLServlet.class);

    // The Access Manager
    protected static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    // the configured permissions
    private static String storedrules = CONFIG.getString("MCR.Access.StorePermissions", "read,write,delete");

    // The User Manager
    protected static MCRUserMgr UM = MCRUserMgr.instance();

    /**
     * This method overrides doGetPost of MCRServlet and handels all actions
     * against the ACL data.
     * 
     * @param job
     *            the MCRServlet job instance
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // read the XML data
        MCREditorSubmission sub = (MCREditorSubmission) (job.getRequest().getAttribute("MCREditorSubmission"));
        org.jdom.Document indoc = sub.getXML();

        // read the parameter
        MCRRequestParameters parms;

        if (sub == null) {
            parms = new MCRRequestParameters(job.getRequest());
        } else {
            parms = sub.getParameters();
        }

        String oldmcrid = parms.getParameter("mcrid");
        LOGGER.debug("XSL.target.param.0 = " + oldmcrid);
        MCRObjectID ID = new MCRObjectID(oldmcrid);

        // get the MCRSession object for the current thread from the session
        // manager.
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String lang = mcrSession.getCurrentLanguage();
        LOGGER.info("LANG = " + lang);

        // create a service object and prepare it
        org.jdom.Element outelm = prepareService((org.jdom.Document) indoc.clone(), ID, job, lang);

        // Save the prepared metadata object
        boolean okay = storeService(outelm, job, ID);

        // call the getNextURL and sendMail methods
        String url = getNextURL(ID, okay);
        sendMail(ID);

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(getBaseURL() + url));
    }

    /**
     * The method return an URL with the next working step. If okay flag is
     * true, the object will present else it shows the error page.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param okay
     *            the return value of the store operation
     * @return the next URL as String
     */
    protected String getNextURL(MCRObjectID ID, boolean okay) throws MCRActiveLinkException {
        StringBuffer sb = new StringBuffer();
        if (okay) {
            sb.append("browse?mode=edit");
        } else {
            sb.append(CONFIG.getString("MCR.SWF.PageDir", "")).append(CONFIG.getString("MCR.SWF.PageErrorStore", "editor_error_store.xml"));
        }
        return sb.toString();
    }

    /**
     * The method send a message to the mail address for the MCRObjectType.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     */
    public final void sendMail(MCRObjectID ID) {
        LOGGER.warn("Send mail is not configured for modify ACLs");
    }

    /**
     * The method read the incoming servacls JDOM tree in a MCRService and
     * prepare this by the following rules. After them it return a JDOM Element
     * of servacls as clone of the prepared data.
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
    protected org.jdom.Element prepareService(org.jdom.Document jdom_in, MCRObjectID ID, MCRServletJob job, String lang) throws Exception {
        org.jdom.Element elm_out = null;
        ArrayList <String>logtext = new ArrayList<String>();
        org.jdom.Element root = jdom_in.getRootElement();
        if (root != null) {
            org.jdom.Element servacls = root.getChild("servacls");
            if (servacls != null) {
                List servacllist = servacls.getChildren("servacl");
                if (servacllist.size() != 0) {
                    for (int i = 0; i < servacllist.size(); i++) {
                        org.jdom.Element servacl = (org.jdom.Element) servacllist.get(i);
                        org.jdom.Element outcond = servacl.getChild("condition");
                        if (outcond != null) {
                            org.jdom.Element outbool = outcond.getChild("boolean");
                            if (outbool != null) {
                                List inbool = outbool.getChildren("boolean");
                                String outoper = outbool.getAttributeValue("operator");
                                if (inbool.size() != 0 && outoper != null && !outoper.equals("true")) {
                                    for (int j = 0; j < inbool.size(); j++) {
                                        List incondlist = ((org.jdom.Element) inbool.get(j)).getChildren("condition");
                                        int k = incondlist.size();
                                        if (k != 0) {
                                            for (int l = 0; l < k; l++) {
                                                org.jdom.Element incond = (org.jdom.Element) incondlist.get(l);
                                                String condvalue = incond.getAttributeValue("value");
                                                if (condvalue == null || (condvalue = condvalue.trim()).length() == 0) {
                                                    ((org.jdom.Element) inbool.get(j)).removeContent(incond);
                                                    k--;
                                                    l--;
                                                    continue;
                                                }
                                                String condfield = incond.getAttributeValue("field");
                                                if (condfield.equals("user")) {
                                                    if (!UM.existUser(condvalue)) {
                                                        ((org.jdom.Element) inbool.get(j)).removeContent(incond);
                                                        k--;
                                                        l--;
                                                        continue;
                                                    }
                                                }
                                                if (condfield.equals("group")) {
                                                    if (!UM.existGroup(condvalue)) {
                                                        ((org.jdom.Element) inbool.get(j)).removeContent(incond);
                                                        k--;
                                                        l--;
                                                        continue;
                                                    }
                                                }
                                            }
                                            if (k == 1) {
                                                org.jdom.Element newtrue = new org.jdom.Element("boolean");
                                                newtrue.setAttribute("operator", "true");
                                                ((org.jdom.Element) inbool.get(j)).addContent(newtrue);
                                            }
                                        } else {
                                            logtext.add("Can't find an inner condition element.");
                                        }
                                    }
                                } else {
                                    if (outoper == null || !outoper.equals("true")) {
                                        logtext.add("Wrong structure of MyCoRe ACL JDOM in boolean.");
                                    }
                                }
                            } else {
                                outbool = new org.jdom.Element("boolean");
                                outbool.setAttribute("operator", "true");
                                outcond.addContent(outbool);
                            }
                        } else {
                            logtext.add("Can't find a condition element.");
                        }
                    }
                } else {
                    logtext.add("Can't find a servacl element.");
                }
            } else {
                logtext.add("Can't find the servacls element.");
            }
        } else {
            logtext.add("The service part is null.");
        }
        elm_out = (org.jdom.Element) root.clone();
        errorHandlerValid(job, logtext, ID, lang);
        return elm_out;
    }

    /**
     * The method store the incoming service data from the ACL editor to the
     * workflow.
     * 
     * @param outelm
     *            the service subelement of an MCRObject
     * @param job
     *            the MCRServletJob instance
     * @param ID
     *            the MCRObjectID
     */
    public final boolean storeService(org.jdom.Element outelm, MCRServletJob job, MCRObjectID ID) {
        // check current state
        List li = AI.getPermissionsForID(ID.getId());
        int aclsize = 0;
        if (li != null) {
            aclsize = li.size();
        }
        if (aclsize == 0) {
            LOGGER.warn("They are no ACLs defined for classification " + ID.getId());
            return false;
        }
        // check incoming ACLs
        int rulesize = 0;
        MCRObjectService serv = new MCRObjectService();
        try {
            serv.setFromDOM(outelm);
            rulesize = serv.getRulesSize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rulesize == 0) {
            LOGGER.warn("The new ACL conditions for classification" + ID.getId() + " are empty!");
        }
        while (0 < rulesize) {
            org.jdom.Element conditions = serv.getRule(0).getCondition();
            String permission = serv.getRule(0).getPermission();
            if (storedrules.indexOf(permission) != -1) {
                if (li.contains(permission)) {
                    MCRAccessManager.updateRule(ID.getId(), permission, conditions, "");
                } else {
                    MCRAccessManager.addRule(ID.getId(), permission, conditions, "");
                }
            }
            serv.removeRule(0);
            rulesize--;
        }

        LOGGER.info("ACL of classification " + ID.getId() + " stored in the server.");
        return true;
    }

    /**
     * An internal method to handle validation errors.
     * 
     * @param job
     *            the MCRServletJob instance
     * @param logtext
     *            a list of log texts as strings
     * @param ID
     *            the current MCRObjectID
     * @param lang
     *            the current language
     */
    private final void errorHandlerValid(MCRServletJob job, List logtext, MCRObjectID ID, String lang) throws Exception {
        if (logtext.size() == 0) {
            return;
        }

        // write to the log file
        for (int i = 0; i < logtext.size(); i++) {
            LOGGER.error(logtext.get(i));
        }

        // prepare editor with error messages
        String pagedir = CONFIG.getString("MCR.editor_page_dir", "");
        String myfile = pagedir + CONFIG.getString("MCR.editor_page_error_formular", "editor_error_formular.xml");
        org.jdom.Document jdom = null;

        try {
            InputStream in = (new URL(getBaseURL() + myfile + "?XSL.Style=xml")).openStream();

            if (in == null) {
                throw new MCRConfigurationException("Can't read editor file " + myfile);
            }

            jdom = new org.jdom.input.SAXBuilder().build(in);

            org.jdom.Element root = jdom.getRootElement();
            List sectionlist = root.getChildren("section");

            for (int i = 0; i < sectionlist.size(); i++) {
                org.jdom.Element section = (org.jdom.Element) sectionlist.get(i);

                if (!section.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE).equals(lang.toLowerCase())) {
                    continue;
                }

                org.jdom.Element p = new org.jdom.Element("p");
                section.addContent(0, p);

                org.jdom.Element center = new org.jdom.Element("center");

                // the error message
                org.jdom.Element table = new org.jdom.Element("table");
                table.setAttribute("width", "80%");

                for (int j = 0; j < logtext.size(); j++) {
                    org.jdom.Element tr = new org.jdom.Element("tr");
                    org.jdom.Element td = new org.jdom.Element("td");
                    org.jdom.Element el = new org.jdom.Element("font");
                    el.setAttribute("color", "red");
                    el.addContent((String) logtext.get(j));
                    td.addContent(el);
                    tr.addContent(td);
                    table.addContent(tr);
                }

                center.addContent(table);
                section.addContent(1, center);
                p = new org.jdom.Element("p");
                section.addContent(2, p);

                // the edit button
                org.jdom.Element form = section.getChild("form");
                form.setAttribute("action", job.getResponse().encodeRedirectURL(getBaseURL() + "servlets/MCRStartEditorServlet"));

                org.jdom.Element input1 = new org.jdom.Element("input");
                input1.setAttribute("name", "lang");
                input1.setAttribute("type", "hidden");
                input1.setAttribute("value", lang);
                form.addContent(input1);

                org.jdom.Element input2 = new org.jdom.Element("input");
                input2.setAttribute("name", "se_mcrid");
                input2.setAttribute("type", "hidden");
                input2.setAttribute("value", ID.getId());
                form.addContent(input2);

                org.jdom.Element input3 = new org.jdom.Element("input");
                input3.setAttribute("name", "type");
                input3.setAttribute("type", "hidden");
                input3.setAttribute("value", ID.getTypeId());
                form.addContent(input3);
            }
        } catch (org.jdom.JDOMException e) {
            throw new MCRException("Can't read editor file " + myfile + " or it has a parse error.", e);
        }

        // restart editor
        job.getRequest().setAttribute("XSL.Style", lang);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), jdom);
    }
}
