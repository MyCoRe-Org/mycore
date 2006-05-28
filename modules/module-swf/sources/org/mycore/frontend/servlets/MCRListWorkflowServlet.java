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

// package
package org.mycore.frontend.servlets;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * This class contains a servlet as extention of MCRServlet. The Servlet read
 * the object metadata files from the workflow, transform them and analyze also
 * the appended derivates. It return a XML file with the following content.
 * <p />
 * &gt;mcr_workflow type="..." step="..."&lt; <br />
 * &gt;item ID="..."&lt; <br />
 * &gt;label&lt;Die 99 582 am Lokschuppen in Schönheide&gt;/label&lt; <br />
 * &gt;data&lt;Jens Kupferschmidt&gt;/data&lt; <br />
 * &gt;data&lt;2004-06-08&gt;/data&lt; <br />
 * &gt;derivate ID="..." label="..."&lt; <br />
 * &gt;file size="..." main="true|false" &lt;...&gt;/file&lt; <br />
 * &gt;/derivate&lt; <br />
 * &gt;/item&lt; <br />
 * &gt;/mcr_workflow&lt; <br />
 * Call this servlet with
 * <b>.../servlets/MCRListWorkflowServlet/XSL.Style=xml&type=...&step=... </b>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRListWorkflowServlet extends MCRServlet {
    // The LOGGER
    private static Logger LOGGER = Logger.getLogger(MCRListWorkflowServlet.class.getName());

    // The workflow manager
    private static MCRSimpleWorkflowManager WFM = null;

    // The file slash
    private static String SLASH = System.getProperty("file.separator");

    private static String DefaultLang = null;

    // The Access Manager
    private static MCRAccessInterface AI = MCRAccessManager.getAccessImpl();

    /** Initialisation of the servlet */
    public void init() throws MCRConfigurationException, javax.servlet.ServletException {
        super.init();
        WFM = MCRSimpleWorkflowManager.instance();
        DefaultLang = MCRConfiguration.instance().getString("MCR.metadata_default_lang", "en");
    }

    /**
     * This method overrides doGetPost of MCRServlet and put the generated DOM
     * in the LayoutServlet. <br />
     * Input parameter are <br />
     * <ul>
     * <li>type - the MyCoRe type</li>
     * <li>step - the workflow step</li>
     * </ul>
     * 
     * @param job
     *            an instance of MCRServletJob
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // get the type
        String type = getProperty(job.getRequest(), "type").trim();
        LOGGER.debug("MCRListWorkflowServlet : type = " + type);

        // get the step
        String step = getProperty(job.getRequest(), "step").trim();
        LOGGER.debug("MCRListWorkflowServlet : step = " + step);

        // get the lang
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        LOGGER.debug("MCRListWorkflowServlet : lang = " + lang);

        // read directory
        ArrayList workfiles = new ArrayList();
        ArrayList derifiles = new ArrayList();

        if (AI.checkPermission("create-" + type)) {
            workfiles = WFM.getAllObjectFileNames(type);
            derifiles = WFM.getAllDerivateFileNames(type);
        }

        String dirname = WFM.getDirectoryPath(type);

        // read the derivate XML files
        ArrayList derobjid = new ArrayList();
        ArrayList derderid = new ArrayList();
        ArrayList dermain = new ArrayList();
        ArrayList derlabel = new ArrayList();
        org.jdom.Document der_in;
        org.jdom.Element der;
        String mainfile;
        String label;
        String derid;
        String objid;
        String dername;

        for (int i = 0; i < derifiles.size(); i++) {
            dername = (String) derifiles.get(i);

            StringBuffer sd = (new StringBuffer(dirname)).append(SLASH).append(dername);
            mainfile = "";
            label = "Derivate of " + dername.substring(0, dername.length() - 4);
            objid = "";

            try {
                der_in = MCRXMLHelper.parseURI(sd.toString(), false);

                // LOGGER.debug("Derivate file "+dername+" was readed.");
                der = der_in.getRootElement();
                label = der.getAttributeValue("label");
                derid = der.getAttributeValue("ID");

                org.jdom.Element s1 = der.getChild("derivate");

                if (s1 != null) {
                    org.jdom.Element s2 = s1.getChild("linkmetas");

                    if (s2 != null) {
                        org.jdom.Element s3 = s2.getChild("linkmeta");

                        if (s3 != null) {
                            objid = s3.getAttributeValue("href", org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
                        }
                    }

                    s2 = s1.getChild("internals");

                    if (s2 != null) {
                        org.jdom.Element s3 = s2.getChild("internal");

                        if (s3 != null) {
                            mainfile = s3.getAttributeValue("maindoc");
                        }
                    }
                }

                derobjid.add(objid);
                derderid.add(derid);
                derlabel.add(label);
                dermain.add(mainfile);
            } catch (Exception ex) {
                LOGGER.warn("Can't parse workflow file " + dername);
            }
        }

        // create a XML JDOM tree with master tag mcr_workflow
        // prepare the transformer stylesheet
        String xslfile = "mycoreobject-" + type + "-to-workflow.xsl";
        File styleFile = getStylesheetFile("/WEB-INF/stylesheets/", xslfile);

        // build the frame of mcr_workflow
        org.jdom.Element root = new org.jdom.Element("mcr_workflow");
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.setAttribute("type", type);
        root.setAttribute("step", step);

        org.jdom.Document workflow_in = null;
        org.jdom.Element writewf = null;
        org.jdom.Element deletewf = null;
        org.jdom.Element writedb = null;
        boolean bdeletewf = false;
        boolean bwritedb = false;

        // initialize transformer
        MCRXSLTransformation transform = MCRXSLTransformation.getInstance();
        TransformerHandler handler = transform.getTransformerHandler(transform.getStylesheet(new StreamSource(styleFile)));
        Map parameters = new HashMap();
        parameters.put("DefaultLang", DefaultLang);
        parameters.put("CurrentLang", lang);
        MCRXSLTransformation.setParameters(handler, parameters);
        // run the loop over all objects in the workflow
        for (int i = 0; i < workfiles.size(); i++) {
            String wfile = (String) workfiles.get(i);
            StringBuffer sb = (new StringBuffer(dirname)).append(SLASH).append(wfile);
            org.jdom.Element elm = null;

            try {
                workflow_in = MCRXMLHelper.parseURI(sb.toString(), false);
                MCRObject obj = new MCRObject();
                obj.setFromJDOM(workflow_in);
                MCRObjectService service = obj.getService();
                int j = service.getRuleIndex("writewf");
                if (j != -1) {
                    writewf = service.getRule(j).getCondition();
                    if (!AI.checkPermission(writewf)) {
                        continue;
                    }
                } else {
                    continue;
                }
                j = service.getRuleIndex("deletewf");
                if (j != -1) {
                    deletewf = service.getRule(j).getCondition();
                    bdeletewf = AI.checkPermission(deletewf);
                }
                j = service.getRuleIndex("writedb");
                if (j != -1) {
                    writedb = service.getRule(j).getCondition();
                    bwritedb = AI.checkPermission(writedb);
                }

                // LOGGER.debug("Workflow file "+wfile+" was readed.");
            } catch (Exception ex) {
                // ex.printStackTrace();
                LOGGER.warn("Can't parse workflow file " + wfile);

                continue;
            }

            try {
                elm = MCRXSLTransformation.transform(workflow_in, handler.getTransformer()).getRootElement();
                elm.detach();
            } catch (Exception ex) {
                LOGGER.error("Error while tranforming XML workflow file " + wfile);

                continue;
            }

            String ID = elm.getAttributeValue("ID");
            elm.setAttribute("deletewf", (new Boolean(bdeletewf)).toString());
            elm.setAttribute("writedb", (new Boolean(bwritedb)).toString());

            // LOGGER.debug("The data ID is "+ID);
            try {
                for (int j = 0; j < derifiles.size(); j++) {
                    if (ID.equals(derobjid.get(j))) {
                        dername = (String) derifiles.get(j);
                        LOGGER.debug("Check the derivate file " + dername);

                        String derpath = (String) derderid.get(j);
                        mainfile = (String) dermain.get(j);

                        org.jdom.Element deriv = new org.jdom.Element("derivate");
                        deriv.setAttribute("ID", (String) derderid.get(j));
                        deriv.setAttribute("label", (String) derlabel.get(j));

                        File dir = new File(dirname, derpath);
                        LOGGER.debug("Derivate under " + dir.getName());

                        if (dir.isDirectory()) {
                            ArrayList dirlist = MCRUtils.getAllFileNames(dir);

                            for (int k = 0; k < dirlist.size(); k++) {
                                org.jdom.Element file = new org.jdom.Element("file");
                                file.setText(derpath + SLASH + (String) dirlist.get(k));

                                File thisfile = new File(dir, (String) dirlist.get(k));
                                file.setAttribute("size", String.valueOf(thisfile.length()));

                                if (mainfile.equals(dirlist.get(k))) {
                                    file.setAttribute("main", "true");
                                } else {
                                    file.setAttribute("main", "false");
                                }

                                deriv.addContent(file);
                            }

                            derifiles.remove(j);
                            derobjid.remove(j);
                            derderid.remove(j);
                            dermain.remove(j);
                            derlabel.remove(j);
                            j--;
                        }

                        elm.addContent(deriv);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Error while read derivates for XML workflow file " + (String) workfiles.get(i));
                LOGGER.error(ex.getMessage());
            }

            root.addContent(elm);
        }

        org.jdom.Document workflow_doc = new org.jdom.Document(root);

        // return the JDOM
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", workflow_doc);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

    /**
     * Gets a File object for the given filename and directory, or returns null
     * if no such file exists.
     */
    protected File getStylesheetFile(String dir, String name) {
        String path = getServletContext().getRealPath(dir + name);
        File file = new File(path);

        if (!file.exists()) {
            LOGGER.debug("MCRListWorkflowServlet did not find stylesheet " + name);

            return null;
        }

        if (!file.canRead()) {
            String msg = "XSL stylesheet " + path + " not readable";
            throw new MCRConfigurationException(msg);
        }

        return file;
    }
}
