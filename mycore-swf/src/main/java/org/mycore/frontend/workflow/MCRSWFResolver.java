/**
 * 
 */
package org.mycore.frontend.workflow;

import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPath;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.common.xml.MCRXMLResource;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSWFResolver implements URIResolver {
    private static final Logger LOGGER = Logger.getLogger(MCRSWFResolver.class);

    private static MCRSimpleWorkflowManager WFM = MCRSimpleWorkflowManager.instance();

    private static String SLASH = System.getProperty("file.separator");

    public static String DefaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG);

    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String key = href.substring(href.indexOf(":") + 1);
        LOGGER.debug("Reading xml from query result using key :" + key);

        String[] param;
        StringTokenizer tok = new StringTokenizer(key, "&");
        Hashtable<String, String> params = new Hashtable<String, String>();

        while (tok.hasMoreTokens()) {
            param = tok.nextToken().split("=");
            if (param.length == 1) {
                params.put(param[0], "");
            } else {
                params.put(param[0], param[1]);
            }
        }

        String baseParam = params.get("base");
        String type = params.get("type");
        String step = params.get("step");
        String with_derivate = params.get("with_derivate");
        try {
            Document workFlow = MCRSWFResolver.getWorkFlow(baseParam, type, step, with_derivate);
            return new JDOMSource(workFlow);
        } catch (Exception e) {
            LOGGER.error("Error while getting workflow document.", e);
            throw new TransformerException(e);
        }
    }

    private static org.jdom2.Document getWorkFlow(String base, String type, String step, String with_derivate) throws IOException, JDOMException {
        if (base != null) {
            base = base.trim();
            LOGGER.debug("Property from request : base = " + base);
        }

        if (type != null) {
            type = type.trim();
            LOGGER.debug("Property from request : type = " + type);
        }

        if (step != null) {
            step = step.trim();
            LOGGER.debug("Property from request : step = " + step);
        }

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
            throw new IllegalArgumentException(msg);
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

        String derBase = base;
        if (base != null && MCRAccessManager.checkPermission("create-" + base)) {
            workfiles = WFM.getAllObjectFileNames(base);
            derBase = base.substring(0, base.indexOf('_')) + "_derivate";
            derifiles = WFM.getAllDerivateFileNames(derBase);
        } else {
            if (MCRAccessManager.checkPermission("create-" + type)) {
                workfiles = WFM.getAllObjectFileNames(type);
                derifiles = WFM.getAllDerivateFileNames(type);
            } else {
                workfiles = new ArrayList<String>();
                derifiles = new ArrayList<String>();
            }
        }

        File dirname = WFM.getDirectoryPath(type);
        File derivateDirectory = derBase != null ? WFM.getDirectoryPath(derBase) : dirname;
        File objectDirectory = base != null ? WFM.getDirectoryPath(base) : dirname;

        // read the derivate XML files
        ArrayList<String> derobjid = new ArrayList<String>();
        ArrayList<String> derderid = new ArrayList<String>();
        ArrayList<String> dermain = new ArrayList<String>();
        ArrayList<String> derlabel = new ArrayList<String>();
        ArrayList<String> dertitle = new ArrayList<String>();
        org.jdom2.Document der_in;
        org.jdom2.Element der;
        String mainfile;
        String label;
        String title;
        String derid;
        String objid;
        String dername;

        for (String derifile : derifiles) {
            dername = (String) derifile;

            File derivateFile = new File(derivateDirectory, dername);
            mainfile = "";
            label = "Derivate of " + dername.substring(0, dername.length() - 4);
            objid = "";

            try {
                der_in = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(derivateFile));
                // LOGGER.debug("Derivate file "+dername+" was readed.");
                der = der_in.getRootElement();
                label = der.getAttributeValue("label");
                derid = der.getAttributeValue("ID");
                title = "";

                XPath objidpath = XPath.newInstance("/mycorederivate/derivate/linkmetas/linkmeta");
                XPath maindocpath = XPath.newInstance("/mycorederivate/derivate/internals/internal");
                XPath titlepath = XPath.newInstance("/mycorederivate/derivate/titles/title[lang('" + lang + "')]");
                for (Object node : objidpath.selectNodes(der_in)) {
                    Element elm = (Element) node;
                    objid = elm.getAttributeValue("href", XLINK_NAMESPACE);
                }
                for (Object node : maindocpath.selectNodes(der_in)) {
                    Element elm = (Element) node;
                    mainfile = elm.getAttributeValue("maindoc");
                }
                for (Object node : titlepath.selectNodes(der_in)) {
                    Element elm = (Element) node;
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
        MCRContent styleSheet = MCRXMLResource.instance().getResource(xslfile);
        if (styleSheet == null) {
            xslfile = "xsl/mycoreobject-" + type + "-to-workflow.xsl";
            styleSheet = MCRXMLResource.instance().getResource(xslfile);
            if (styleSheet == null) {
                xslfile = "xsl/mycoreobject-to-workflow.xsl";
                styleSheet = MCRXMLResource.instance().getResource(xslfile);
            }
        }

        // build the frame of mcr_workflow
        org.jdom2.Element root = new org.jdom2.Element("mcr_workflow");
        root.addNamespaceDeclaration(XSI_NAMESPACE);
        if (base != null) {
            root.setAttribute("base", base);
        } else {
            root.setAttribute("base", "");
        }
        root.setAttribute("type", type);
        root.setAttribute("step", step);
        root.setAttribute("with_derivate", with_derivate);

        org.jdom2.Document workflow_in = null;
        org.jdom2.Element writewf = null;
        org.jdom2.Element deletewf = null;
        org.jdom2.Element writedb = null;
        boolean bdeletewf = false;
        boolean bwritedb = false;

        // initialize transformer
        MCRXSLTransformation transform = MCRXSLTransformation.getInstance();
        TransformerHandler handler = transform.getTransformerHandler(transform.getStylesheet(styleSheet.getSource()));
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("DefaultLang", DefaultLang);
        parameters.put("CurrentLang", lang);
        MCRXSLTransformation.setParameters(handler, parameters);
        MCRAccessInterface ai = MCRAccessManager.getAccessImpl();
        // run the loop over all objects in the workflow
        for (String workfile : workfiles) {
            String wfile = (String) workfile;
            File wf = new File(objectDirectory, wfile);
            Element elm = null;

            try {
                workflow_in = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(wf));
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
                j = service.getRuleIndex(PERMISSION_WRITE);
                if (j != -1) {
                    writedb = service.getRule(j).getCondition();
                    bwritedb = ai.checkPermission(writedb);
                } else {
                    bwritedb = MCRAccessManager.checkPermission(obj.getId().toString(), PERMISSION_WRITE);
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
            elm.setAttribute(PERMISSION_WRITE, String.valueOf(bwritedb));

            // LOGGER.debug("The data ID is "+ID);
            try {
                for (int j = 0; j < derifiles.size(); j++) {
                    if (ID.equals(derobjid.get(j))) {
                        dername = (String) derifiles.get(j);
                        LOGGER.debug("Check the derivate file " + dername);

                        String derpath = (String) derderid.get(j);
                        mainfile = (String) dermain.get(j);

                        Element deriv = new Element("derivate");
                        deriv.setAttribute("ID", (String) derderid.get(j));
                        deriv.setAttribute("label", (String) derlabel.get(j));
                        title = (String) dertitle.get(j);
                        if ((title != null) && (title.length() != 0)) {
                            deriv.setAttribute("title", title);
                        }

                        File dir = new File(derivateDirectory, derpath);
                        LOGGER.debug("Derivate under " + dir.getName());

                        if (dir.isDirectory()) {
                            ArrayList<String> dirlist = MCRUtils.getAllFileNames(dir);

                            for (String aDirlist : dirlist) {
                                Element file = new Element("file");
                                file.setText(derpath + SLASH + (String) aDirlist);

                                File thisfile = new File(dir, (String) aDirlist);
                                file.setAttribute("size", String.valueOf(thisfile.length()));

                                if (mainfile.equals(aDirlist)) {
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
                LOGGER.error("Error while read derivates for XML workflow file " + (String) workfile, ex);
            }
            root.addContent(elm);
        }

        org.jdom2.Document workflow_doc = new org.jdom2.Document(root);
        return workflow_doc;
    }

}
