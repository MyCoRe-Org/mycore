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

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.editor.MCRRequestParameters;
import org.xml.sax.SAXParseException;

/**
 * This class is a special to work wit the interactive input from the dialog of
 * ACL (Access Control List) changes.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
abstract public class MCRCheckACLBase extends MCRCheckBase {

    private static final long serialVersionUID = 1L;
    private static Logger LOGGER = Logger.getLogger(MCRCheckACLBase.class);

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
        Document indoc = sub.getXML();

        // read the parameter
        MCRRequestParameters parms = sub.getParameters();

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
        MCRObjectID ID = MCRObjectID.getInstance(oldmcrid);

        // check access
        if (!checkAccess(ID)) {
            job.getResponse().sendRedirect(MCRFrontendUtil.getBaseURL() + usererrorpage);
            return;
        }

        // create a service object and prepare it
        Element outelm = prepareService((Document) indoc.clone(), ID, job, lang);

        // Save the prepared metadata object
        boolean okay = storeService(outelm, job, ID);

        // call the getNextURL and sendMail methods
        String url = getNextURL(ID, okay);
        sendMail(ID);

        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + url));
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
     * @throws SAXParseException 
     * @throws MCRException 
     */
    abstract public boolean storeService(Element outelm, MCRServletJob job, MCRObjectID ID) throws MCRException, SAXParseException, IOException;

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
    protected Element prepareService(Document jdom_in, MCRObjectID ID, MCRServletJob job, String lang) throws Exception {
        Element elm_out;
        ArrayList<String> logtext = new ArrayList<String>();
        Element root = jdom_in.getRootElement();
        if (root != null) {
            Element servacls = root.getChild("servacls");
            if (servacls != null) {
                @SuppressWarnings("unchecked")
                List<Element> servacllist = servacls.getChildren("servacl");
                if (servacllist.size() != 0) {
                    for (Element servacl : servacllist) {
                        Element outcond = servacl.getChild("condition");
                        if (outcond != null) {
                            Element outbool = outcond.getChild("boolean");
                            if (outbool != null) {
                                @SuppressWarnings("unchecked")
                                List<Element> inbool = outbool.getChildren("boolean");
                                String outoper = outbool.getAttributeValue("operator");
                                if (inbool.size() != 0 && outoper != null && !outoper.equals("true")) {
                                    for (Element anInbool : inbool) {
                                        @SuppressWarnings("unchecked")
                                        List<Element> incondlist = anInbool.getChildren("condition");
                                        int k = incondlist.size();
                                        if (k != 0) {
                                            for (int l = 0; l < k; l++) {
                                                Element incond = incondlist.get(l);
                                                String condvalue = incond.getAttributeValue("value");
                                                if (condvalue == null || (condvalue = condvalue.trim()).length() == 0) {
                                                    anInbool.removeContent(incond);
                                                    k--;
                                                    l--;
                                                    continue;
                                                }
                                                String condfield = incond.getAttributeValue("field");
                                                if (condfield.equals("date")) {
                                                    if (MCRUtils.convertDateToISO(condvalue) == null) {
                                                        anInbool.removeContent(incond);
                                                        k--;
                                                        l--;
                                                    }
                                                }
                                            }
                                            if (k == 1) {
                                                String inbooloper = anInbool.getAttributeValue("operator");
                                                if ((inbooloper != null) && inbooloper.toLowerCase(Locale.ROOT).equals("and")) {
                                                    Element newtrue = new Element("boolean");
                                                    newtrue.setAttribute("operator", "true");
                                                    anInbool.addContent(newtrue);
                                                } else {
                                                    Element newfalse = new Element("boolean");
                                                    newfalse.setAttribute("operator", "false");
                                                    anInbool.addContent(newfalse);
                                                }
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
                                outbool = new Element("boolean");
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
        elm_out = (Element) root.clone();
        errorHandlerValid(job, logtext, ID, lang);
        return elm_out;
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
    private void errorHandlerValid(MCRServletJob job, List<String> logtext, MCRObjectID ID, String lang) throws Exception {
        if (logtext.size() == 0) {
            return;
        }

        // write to the log file
        for (String aLogtext1 : logtext) {
            LOGGER.error(aLogtext1);
        }

        // prepare editor with error messages
        String pagedir = MCRConfiguration.instance().getString("MCR.SWF.PageDir", "");
        String myfile = pagedir + MCRConfiguration.instance().getString("MCR.SWF.PageErrorFormular", "editor_error_formular.xml");
        Document jdom;

        try {
            InputStream in = (new URL(MCRFrontendUtil.getBaseURL() + myfile + "?XSL.Style=xml")).openStream();

            if (in == null) {
                throw new MCRConfigurationException("Can't read editor file " + myfile);
            }

            jdom = new org.jdom2.input.SAXBuilder().build(in);

            Element root = jdom.getRootElement();
            @SuppressWarnings("unchecked")
            List<Element> sectionlist = root.getChildren("section");

            for (Element section : sectionlist) {
                if (!section.getAttributeValue("lang", Namespace.XML_NAMESPACE).equals(lang.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                Element p = new Element("p");
                section.addContent(0, p);

                Element center = new Element("center");

                // the error message
                Element table = new Element("table");
                table.setAttribute("width", "80%");

                for (String aLogtext : logtext) {
                    Element tr = new Element("tr");
                    Element td = new Element("td");
                    Element el = new Element("font");
                    el.setAttribute("color", "red");
                    el.addContent(aLogtext);
                    td.addContent(el);
                    tr.addContent(td);
                    table.addContent(tr);
                }

                center.addContent(table);
                section.addContent(1, center);
                p = new Element("p");
                section.addContent(2, p);

                // the edit button
                Element form = section.getChild("form");
                form.setAttribute("action", job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + "servlets/MCRStartEditorServlet"));

                Element input1 = new Element("input");
                input1.setAttribute("name", "lang");
                input1.setAttribute("type", "hidden");
                input1.setAttribute("value", lang);
                form.addContent(input1);

                Element input2 = new Element("input");
                input2.setAttribute("name", "se_mcrid");
                input2.setAttribute("type", "hidden");
                input2.setAttribute("value", ID.toString());
                form.addContent(input2);

                Element input3 = new Element("input");
                input3.setAttribute("name", "type");
                input3.setAttribute("type", "hidden");
                input3.setAttribute("value", ID.getTypeId());
                form.addContent(input3);
            }
        } catch (org.jdom2.JDOMException e) {
            throw new MCRException("Can't read editor file " + myfile + " or it has a parse error.", e);
        }

        // restart editor
        job.getRequest().setAttribute("XSL.Style", lang);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
    }
    
}
