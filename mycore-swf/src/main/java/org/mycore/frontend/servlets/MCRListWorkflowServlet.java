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

// package
package org.mycore.frontend.servlets;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.jdom.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xml.MCRXMLResource;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.ifs2.MCRContent;
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
 * &gt;label&lt;Die 99 582 am Lokschuppen in Schoenheide&gt;/label&lt; <br />
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
 * @version $Revision$ $Date: 2010-12-29 09:18:22 +0100 (Mi, 29. Dez
 *          2010) $
 */
public class MCRListWorkflowServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRListWorkflowServlet.class.getName());

    private static MCRSimpleWorkflowManager WFM = null;

    private static String SLASH = System.getProperty("file.separator");

    private static String DefaultLang = null;

    /** Initialization of the servlet */
    public void init() throws MCRConfigurationException, javax.servlet.ServletException {
        super.init();
        WFM = MCRSimpleWorkflowManager.instance();
        DefaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG);
    }

    /**
     * This method overrides doGetPost of MCRServlet and put the generated DOM
     * in the LayoutService. <br />
     * Input parameter are <br />
     * <ul>
     * <li>base - the MyCoRe ID base or</li>
     * <li>type - the MyCoRe ID type</li>
     * <li>step - the workflow step</li>
     * </ul>
     * 
     * @param job
     *            an instance of MCRServletJob
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        String base = getProperty(job.getRequest(), "base");
        if (base != null) {
            base = base.trim();
            LOGGER.debug("Property from request : base = " + base);
        }

        String type = getProperty(job.getRequest(), "type");
        if (type != null) {
            type = type.trim();
            LOGGER.debug("Property from request : type = " + type);
        }

        String step = getProperty(job.getRequest(), "step");
        if (step != null) {
            step = step.trim();
            LOGGER.debug("Property from request : step = " + step);
        }

        String with_derivate = getProperty(job.getRequest(), "with_derivate");
        if (with_derivate != null) {
            with_derivate = with_derivate.trim();
            if (!with_derivate.equals("true"))
                with_derivate = "false";
        } else {
            with_derivate = "false";
        }
        LOGGER.debug("Property from request : with_derivate = " + with_derivate);

        if (((base == null) && (type == null)) || (step == null)) {
            String msg = "Error: HTTP request has no base or type argument";
            LOGGER.error(msg);
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        LOGGER.debug("Property from request : lang = " + lang);

        if (type == null) {
            int ibase = base.indexOf('_');
            if (ibase == -1) {
                type = base;
            } else {
                type = base.substring(ibase + 1);
            }
        }

        ArrayList<String> workfiles;
        ArrayList<String> derifiles;

        if (base != null && MCRAccessManager.checkPermission("create-" + base)) {
            workfiles = WFM.getAllObjectFileNames(base);
            derifiles = WFM.getAllDerivateFileNames(base);
        } else {
            if (MCRAccessManager.checkPermission("create-" + type)) {
                workfiles = WFM.getAllObjectFileNames(type);
                derifiles = WFM.getAllDerivateFileNames(type);
            } else {
                workfiles = new ArrayList<String>();
                derifiles = new ArrayList<String>();
            }
        }

        File dirname = null;
        if (base != null) {
            dirname = WFM.getDirectoryPath(base);
        } else {
            dirname = WFM.getDirectoryPath(type);
        }

        // read the derivate XML files
        ArrayList<String> derobjid = new ArrayList<String>();
        ArrayList<String> derderid = new ArrayList<String>();
        ArrayList<String> dermain = new ArrayList<String>();
        ArrayList<String> derlabel = new ArrayList<String>();
        ArrayList<String> dertitle = new ArrayList<String>();
        org.jdom.Document der_in;
        org.jdom.Element der;
        String mainfile;
        String label;
        String title;
        String derid;
        String objid;
        String dername;

        for (int i = 0; i < derifiles.size(); i++) {
            dername = (String) derifiles.get(i);

            File derivateFile = new File(dirname, dername);
            mainfile = "";
            label = "Derivate of " + dername.substring(0, dername.length() - 4);
            objid = "";

            try {
                der_in = MCRXMLParserFactory.getNonValidatingParser().parseXML(MCRContent.readFrom(derivateFile));
                // LOGGER.debug("Derivate file "+dername+" was readed.");
                der = der_in.getRootElement();
                label = der.getAttributeValue("label");
                derid = der.getAttributeValue("ID");
                title = "";

                XPath objidpath = XPath.newInstance("/mycorederivate/derivate/linkmetas/linkmeta");
                XPath maindocpath = XPath.newInstance("/mycorederivate/derivate/internals/internal");
                XPath titlepath = XPath.newInstance("/mycorederivate/derivate/titles/title[lang(\'" + lang + "\')]");
                for (Object node : objidpath.selectNodes(der_in)) {
                    org.jdom.Element elm = (org.jdom.Element) node;
                    objid = elm.getAttributeValue("href", XLINK_NAMESPACE);
                }
                for (Object node : maindocpath.selectNodes(der_in)) {
                    org.jdom.Element elm = (org.jdom.Element) node;
                    mainfile = elm.getAttributeValue("maindoc");
                }
                for (Object node : titlepath.selectNodes(der_in)) {
                    org.jdom.Element elm = (org.jdom.Element) node;
                    title = elm.getText();
                }

                derobjid.add(objid);
                derderid.add(derid);
                derlabel.add(label);
                dertitle.add(title);
                dermain.add(mainfile);
            } catch (Exception ex) {
                if (LOGGER.isDebugEnabled()) {
                    ex.printStackTrace();
                }
                LOGGER.warn("Can't parse workflow file " + dername);
            }
        }

        // create a XML JDOM tree with master tag mcr_workflow
        // prepare the transformer stylesheet
        String xslfile = "xsl/mycoreobject-" + base + "-to-workflow.xsl";
        Document styleSheet = MCRXMLResource.instance().getResource(xslfile);
        if (styleSheet == null) {
            xslfile = "xsl/mycoreobject-" + type + "-to-workflow.xsl";
            styleSheet = MCRXMLResource.instance().getResource(xslfile);
            if (styleSheet == null) {
                xslfile = "xsl/mycoreobject-to-workflow.xsl";
                styleSheet = MCRXMLResource.instance().getResource(xslfile);
            }
        }

        // build the frame of mcr_workflow
        org.jdom.Element root = new org.jdom.Element("mcr_workflow");
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        if (base != null) {
            root.setAttribute("base", base);
        } else {
            root.setAttribute("base", "");
        }
        root.setAttribute("type", type);
        root.setAttribute("step", step);
        root.setAttribute("with_derivate", with_derivate);

        org.jdom.Document workflow_in = null;
        org.jdom.Element writewf = null;
        org.jdom.Element deletewf = null;
        org.jdom.Element writedb = null;
        boolean bdeletewf = false;
        boolean bwritedb = false;

        // initialize transformer
        MCRXSLTransformation transform = MCRXSLTransformation.getInstance();
        TransformerHandler handler = transform.getTransformerHandler(transform.getStylesheet(new JDOMSource(styleSheet)));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("DefaultLang", DefaultLang);
        parameters.put("CurrentLang", lang);
        MCRXSLTransformation.setParameters(handler, parameters);
        MCRAccessInterface ai = MCRAccessManager.getAccessImpl();
        // run the loop over all objects in the workflow
        for (int i = 0; i < workfiles.size(); i++) {
            String wfile = (String) workfiles.get(i);
            File wf = new File(dirname, wfile);
            org.jdom.Element elm = null;

            try {
                workflow_in = MCRXMLParserFactory.getNonValidatingParser().parseXML(MCRContent.readFrom(wf));
                MCRObject obj = new MCRObject(workflow_in);
                MCRObjectService service = obj.getService();
                int j = service.getRuleIndex("writewf");
                if (j != -1) {
                    writewf = service.getRule(j).getCondition();
                    if (!ai.checkPermission(writewf)) {
                        continue;
                    }
                }
                j = service.getRuleIndex("deletewf");
                if (j != -1) {
                    deletewf = service.getRule(j).getCondition();
                    bdeletewf = ai.checkPermission(deletewf);
                } else {
                    bdeletewf = true;
                }
                j = service.getRuleIndex("writedb");
                if (j != -1) {
                    writedb = service.getRule(j).getCondition();
                    bwritedb = ai.checkPermission(writedb);
                } else {
                    bwritedb = MCRAccessManager.checkPermission(obj.getId().toString(), "writedb");
                }
            } catch (Exception ex) {
                if (LOGGER.isDebugEnabled()) {
                    ex.printStackTrace();
                }
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
            elm.setAttribute("deletewf", String.valueOf(bdeletewf));
            elm.setAttribute("writedb", String.valueOf(bwritedb));

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
                        title = (String) dertitle.get(j);
                        if ((title != null) && (title.length() != 0)) {
                            deriv.setAttribute("title", title);
                        }

                        File dir = new File(dirname, derpath);
                        LOGGER.debug("Derivate under " + dir.getName());

                        if (dir.isDirectory()) {
                            ArrayList<String> dirlist = MCRUtils.getAllFileNames(dir);

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
                            dertitle.remove(j);
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
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), MCRContent.readFrom(workflow_doc));
    }
}
