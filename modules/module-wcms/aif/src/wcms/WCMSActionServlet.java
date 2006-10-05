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

/*
 * WCMSActionServlet.java
 *
 * Created on 22. September 2003, 16:09
 */
package wcms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * 
 * @author m5brmi-sh, Thomas Scheffler (yagee)
 * @version
 */
public class WCMSActionServlet extends WCMSServlet {
    private static final Namespace ns = Namespace.XML_NAMESPACE;

    /* Session, userDB */
    private String userID = null; // UserID of the current user

    private String userRealName = null; // Name of the current user

    private String userClass = null; // Class which a current user belongs to

    // {sysadmin, admin, editor, author}
    private List rootNodes = null; // List of nodes under wich the current user

    // can perform actions like add, edit and
    // delete

    /* Session, WCMSChooseServlet */
    private String href = null;

    private String action = null;

    private String mode = null;

    private String dir = null;

    private String currentLang = null;

    private String defaultLang = null;

    private String currentLangLabel = null;

    /* Request */
    private String target = null; // Target where to open the current file

    // adding(editing) {_blank, _self}
    private String style = null; // Font style showing the element in the

    // navigation {bold, normal}
    private String label = null; // Label showing the element in the
                                    // navigation

    private String content = null; // Content saved as xml file

    private String contentCurrentLang = null; // Translated content of the

    // current Language saved as xml
    // file
    private String link = null; // Representing an url linking to the current

    // element
    private String changeInfo = null; // Contains changes made during edit
                                        // (like

    // commit in cvs)
    private String realyDel = null; // Marks a flag for confirmation security

    // request on the delete action
    private String labelPath = null; // Complete path of labels on the
                                        // acestor

    // axis to the current element
    private String replaceMenu = null; // Representing a Parameter, allowing an

    // navigation element to replace the
    // previous navigation structure only
    // with its subelements. Can be "true" or
    // "false".
    private String constrainPopUp = null; // 

    private String masterTemplate = null; // Represents the action of
                                            // templates
    
    private String selfTemplate = null; // Represents the action of
                                           // templates

    // (set, change, remove)
    private String fileName = null; // href attribut for the new element and

    // posiition where the element is saved on
    // the server in case of mode = intern
    private String contentFileBackup = null; // Path for content backup
                                                // (should

    // be data type File)
    private String naviFileBackup = null; // Path for navigation.xml backup

    // (should be data type File)
    private String error = null; // Errors, such as "empty form fields" aso

    private String attribute = null;

    private String avalue = null;

    private String usedParser = null; // Parser that is used for content code

    // validation (xhtml|xml|none);
    private boolean dcbActionAdd = false;

    private String dcbValueAdd = null;

    private boolean dcbActionDelete = false;

    private String dcbValueDelete = null;

    private String sessionParam = null;

    private String addAtPosition = null;
    
    private boolean back;
    
    private String backaddr = null;

    private File naviFile = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));

    private HttpServletRequest request;

    private HttpServletResponse response;

    char fs = File.separatorChar;

    String[] imageList;

    String[] documentList;

    File hrefFile;

    Element actElem;

    File[] masterTemplates;

    static Logger logger = Logger.getLogger(WCMSActionServlet.class);

    MCRSession mcrSession = null;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {
        mcrSession = MCRSessionMgr.getCurrentSession();
        
        setReq(request);
        setResp(response);
        setback();
        setbackaddr();
        initParam(getReq());
        

        if (!realyDel.equals("false")) {
            doItAll(hrefFile, action, mode, addAtPosition);
        } else {
            sessionParam = "choose";
            generateOutput(error, label, fileName);
        }
    }

    public void generateOutput(String error, String label, String fileName) {
        try {
            // MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
            Document doc = getXMLAsJDOM(naviFile);

            // Document doc = builder.build(naviFile);
            Element root = doc.getRootElement();
            validate(root);

            Element rootOut = new Element("cms");
            Document jdom = new Document(rootOut);
            rootOut.addContent(new Element("session").setText(sessionParam));
            rootOut.addContent(new Element("href").setText(fileName));
            rootOut.addContent(new Element("label").setText(label));
            rootOut.addContent(new Element("target").setText(target));
            rootOut.addContent(new Element("style").setText(style));
            rootOut.addContent(new Element("usedParser").setText(usedParser));
            rootOut.addContent(new Element("addAtPosition").setText(addAtPosition));
            rootOut.addContent(new Element("action").setAttribute("mode", mode).setText(action));
            rootOut.addContent(new Element("sessionID").setText(mcrSession.getID()));
            rootOut.addContent(new Element("userID").setText(userID));
            rootOut.addContent(new Element("userClass").setText(userClass));

            // rootNodes Iterator used in case of action==delete &&
            // realyDel==false
            Iterator rootNodesIterator = rootNodes.iterator();

            while (rootNodesIterator.hasNext()) {
                Element rootNode = (Element) rootNodesIterator.next();
                rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
            }

            if (action.equals("delete")) {
                File[] contentTemplates = new File((CONFIG.getString("MCR.WCMS.templatePath") + "content/").replace('/', File.separatorChar)).listFiles();
                Element templates = new Element("templates");
                Element contentTemp = new Element("content");

                for (int i = 0; i < contentTemplates.length; i++) {
                    if (!contentTemplates[i].isDirectory()) {
                        contentTemp.addContent(new Element("template").setText(contentTemplates[i].getName()));
                    }
                }

                templates.addContent(contentTemp);
                rootOut.addContent(templates);
            } else {
                imageList = null;
                documentList = null;

                Element images = new Element("images");
                rootOut.addContent(images);
                imageList = (new File(CONFIG.getString("MCR.WCMS.imagePath").replace('/', File.separatorChar))).list();

                for (int i = 0; i < imageList.length; i++) {
                    images.addContent(new Element("image").setText(CONFIG.getString("MCR.WCMS.imagePath") + imageList[i]));
                }

                Element documents = new Element("documents");
                rootOut.addContent(documents);
                documentList = (new File(CONFIG.getString("MCR.WCMS.documentPath").replace('/', File.separatorChar))).list();

                for (int i = 0; i < documentList.length; i++) {
                    documents.addContent(new Element("document").setText(CONFIG.getString("MCR.WCMS.imagePath") + documentList[i]));
                }

                Element templates = new Element("templates");
                Element master = new Element("master");

                for (int i = 0; i < masterTemplates.length; i++) {
                    if (masterTemplates[i].isDirectory() && (masterTemplates[i].getName().compareToIgnoreCase("cvs") != 0)) {
                        master.addContent(new Element("template").setText(masterTemplates[i].getName()));
                    }
                }

                templates.addContent(master);
                rootOut.addContent(templates);
            }

            if (error != null) {
                SAXBuilder saxb = new SAXBuilder();
                saxb.setEntityResolver(new ResolveDTD());

                Document saxDoc = new Document();

                if (!action.equals("translate")) {
                    try {
                        saxDoc = saxb.build(new StringReader(content));

                        Element html = saxDoc.getRootElement();
                        html.detach();

                        Element contentElem = new Element("section");
                        contentElem.addContent(html);
                        rootOut.addContent(contentElem);
                        rootOut.addContent(new Element("error").setText(error));
                    } catch (JDOMException jex) {
                        try {
                            String contentTmp = "<section>" + content + "</section>";
                            saxDoc = saxb.build(new StringReader(contentTmp));

                            Element html = saxDoc.getRootElement();
                            html.detach();

                            Element contentElem = new Element("section");
                            contentElem.setContent(html);
                            rootOut.addContent(contentElem);
                            rootOut.addContent(new Element("error").setText(error));
                        } catch (Exception e) {
                            /*
                             * Element contentElem = new Element("content");
                             * contentElem.addContent(content);
                             * rootOut.addContent(contentElem);
                             */
                            rootOut.addContent(new Element("error").setText(error));

                            // e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        saxDoc = saxb.build(new StringReader(contentCurrentLang));

                        Element html = saxDoc.getRootElement();
                        html.detach();

                        Element contentElem = new Element("section");
                        contentElem.addContent(html);
                        rootOut.addContent(contentElem);
                        rootOut.addContent(new Element("error").setText(error));
                    } catch (JDOMException jex) {
                        try {
                            String contentCurrentLangTmp = "<section>" + contentCurrentLang + "</section>";
                            saxDoc = saxb.build(new StringReader(contentCurrentLangTmp));

                            Element html = saxDoc.getRootElement();
                            html.detach();

                            Element contentElem = new Element("section");
                            contentElem.setContent(html);
                            rootOut.addContent(contentElem);
                            rootOut.addContent(new Element("error").setText(error));
                        } catch (Exception e) {
                            /*
                             * Element contentElem = new Element("content");
                             * contentElem.addContent(contentCurrentLang);
                             * rootOut.addContent(contentElem);
                             */
                            rootOut.addContent(new Element("error").setText(error));

                            // e.printStackTrace();
                        }
                    }
                }
            }

            request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            String jump = null;
            int filepos = 0;
            String help = null;
            
            // if you used the edittool
            if(getback())
              {
            	if ((checkInput()) == false) {
            		// request.setAttribute("XSL.Style", "xml");
                    RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
                    rd.forward(request, response); 
                  }
            	else
            	  {
	               //if new intern child, predecossor or successor created, go to it	
	               if(action.equals("add") && mode.equals("intern"))
	                 {
	            	  if(addAtPosition.equals("child"))
	            	    {  
	            		 int length = getbackaddr().length();
	                	 help = getbackaddr().substring(0,(length-4));
	                	 filepos = fileName.lastIndexOf("/");
	                	 jump = help.concat(fileName.substring(filepos));  
	            		}
	            	  else
	            	    {
	            		 filepos = fileName.lastIndexOf("/"); 
	            		 int neighbarpos = getbackaddr().lastIndexOf("/");
	            		 help = getbackaddr().substring(0,neighbarpos);
	            		 jump = help.concat(fileName.substring(filepos));
	            		 
	            	    }
	                 }
	               // on edit or external link go back to initiator address
	               else
	                 {
	            	  jump = getbackaddr(); 
	                 }
	            	response.sendRedirect(jump);
            	  }
              }	
            else
              {	
             // request.setAttribute("XSL.Style", "xml");
             RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
             rd.forward(request, response);
              }
          } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }
    }

    public void updateFooterFile() {
        try {
            File footer = new File(CONFIG.getString("MCR.WCMS.footer").replace('/', File.separatorChar));
            Document doc = new Document();

            if (!footer.exists()) footer.getParentFile().mkdirs();
            doc = getXMLAsJDOM(footer);
            Element root = doc.getRootElement();
            root.setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("labelPath", labelPath).setAttribute("lastEditor", userRealName);
            writeJDOMDocumentToFile(doc, footer);

            /*
             * XMLOutputter xmlout = new
             * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
             * xmlout.output(doc, new FileOutputStream(footer));
             */
        } catch (Exception e) {
            e.printStackTrace();

            // System.out.println(e.getMessage());
        }
    }

    public void addLastModifiedToContent(File hrefFile) {
        try {
            Document doc = getXMLAsJDOM(hrefFile);
            Element root = doc.getRootElement();

            if (root.getChild("meta") != null) {
                if (root.getChild("meta").getChild("log") != null) {
                    root.getChild("meta").getChild("log").setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("labelPath", labelPath).setAttribute("lastEditor", userRealName);
                } else {
                    root.getChild("meta").addContent(new Element("log").setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("labelPath", labelPath).setAttribute("lastEditor", userRealName));
                }
            } else {
                Element meta = new Element("meta");
                meta.addContent(new Element("log").setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("labelPath", labelPath).setAttribute("lastEditor", userRealName));
                root.addContent(meta);
            }

            writeJDOMDocumentToFile(doc, hrefFile);

            /*
             * XMLOutputter xmlout = new
             * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
             * xmlout.output(doc, new FileOutputStream(hrefFile));
             */
        } catch (Exception e) {
            e.printStackTrace();

            // System.out.println(e.getMessage());
        }
    }

    public void writeToLogFile(String action, String contentFileBackup) {
        try {
            File logFile = new File(CONFIG.getString("MCR.WCMS.logFile").replace('/', File.separatorChar));
            Document doc;

            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
                doc = new Document(new Element("loggings"));

                // System.out.println("Logfile wurde
                // unter"+logFile.toString()+"angelegt.");
            } else {
                doc = getXMLAsJDOM(logFile);
            }

            Element root = doc.getRootElement();

            if (contentFileBackup == null) {
                contentFileBackup = "";
            }

            if (changeInfo == null) {
                root.addContent(new Element("log").setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("userRealName", userRealName).setAttribute("labelPath", labelPath).setAttribute("doneAction", action).setAttribute("backupContentFile", contentFileBackup).setAttribute("backupNavigationFile", naviFileBackup));
            } else {
                Element log = new Element("log");
                log.setAttribute("date", getDate()).setAttribute("time", getTime()).setAttribute("userRealName", userRealName).setAttribute("labelPath", labelPath).setAttribute("doneAction", action).setAttribute("backupContentFile", contentFileBackup).setAttribute("backupNavigationFile", naviFileBackup).addContent(new Element("note").setText(changeInfo));
                root.addContent(log);
            }

            writeJDOMDocumentToFile(doc, logFile);

            /*
             * XMLOutputter xmlout = new
             * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
             * xmlout.output(doc, new FileOutputStream(logFile));
             */
        } catch (Exception e) {
            e.printStackTrace();

            // System.out.println(e.getMessage());
        }
    }

    public String getDate() {
        Calendar gregCal = GregorianCalendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(gregCal.getTime());

        return date;
    }

    public String getTime() {
        Calendar gregCal = GregorianCalendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time = sdf.format(gregCal.getTime());

        return time;
    }

    public void validate(Element element) {
        List elements = element.getChildren();
        Iterator elementIterator = elements.iterator();

        while (elementIterator.hasNext()) {
            Element child = (Element) elementIterator.next();

            if (child.getAttribute("href") != null) {
                if (child.getAttributeValue("href").equals(href)) {
                    mode = child.getAttributeValue("type");

                    while (((Element) child.getParent()).getChild("label") != null) {
                        child = (Element) child.getParent();
                    }

                    return;
                }

                validate(child);
            }
        }
    }

    public Element findActElem(Element element, String attr, String value) {
        List elements = element.getChildren();
        Iterator elementIterator = elements.iterator();
        Element tempresult;

        while (elementIterator.hasNext()) {
            Element child = (Element) elementIterator.next();

            if ((child.getAttribute(attr) != null) && child.getAttribute(attr).getValue().equals(value)) {
                return child;
            }

            tempresult = findActElem(child, attr, value);

            if (tempresult != null) {
                return tempresult;
            }
        }

        return null;
    }

    public Element findActElem(Element element, String attr, String value, Namespace ns) {
        List elements = element.getChildren();
        Iterator elementIterator = elements.iterator();
        Element tempresult;

        while (elementIterator.hasNext()) {
            Element child = (Element) elementIterator.next();

            if ((child.getAttribute(attr, ns) != null) && child.getAttributeValue(attr, ns).equals(value)) {
                return child;
            }

            tempresult = findActElem(child, attr, value, ns);

            if (tempresult != null) {
                return tempresult;
            }
        }

        return null;
    }

    public int countChildren(Element actElem) {
        List children = actElem.getChildren();
        Iterator elementIterator = children.iterator();
        int counter = 0;

        while (elementIterator.hasNext()) {
            counter++;
            elementIterator.next();
        }

        return counter;
    }

    public String makeBackup(File inputFile) {
        File backupFile = null;

        if (inputFile.toString().endsWith(fs + "navigation.xml")) {
            backupFile = new File(CONFIG.getString("MCR.WCMS.backupPath").replace('/', File.separatorChar) + fs + "navi" + fs + "navigation.xml");
        } else {
            backupFile = new File(CONFIG.getString("MCR.WCMS.backupPath").replace('/', File.separatorChar) + href.replace('/', File.separatorChar));
        }

        if (inputFile.exists()) {
            try {
                BufferedInputStream bi = new BufferedInputStream(new FileInputStream(inputFile));

                if (backupFile.exists()) {
                    int version = 1;
                    String backupPath = backupFile.toString();

                    while (backupFile.exists()) {
                        backupFile = new File(backupPath + "." + String.valueOf(version));
                        version++;
                    }
                } else {
                    backupFile.getParentFile().mkdirs();

                    // backupFile.createNewFile();
                }

                BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(backupFile));
                MCRUtils.copyStream(bi, bo);
                bi.close();
                bo.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                error = fnfe.getMessage();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                error = ioe.getMessage();
            }
        }

        return backupFile.toString();
    }

    public void modifyNavi(File inputFile) {
        if (action.equals("add")) {
            try {
                Document doc = getXMLAsJDOM(inputFile);
                Element root = doc.getRootElement();
                validate(root);

                Element actElem = findActElem(root, attribute, avalue);
                List neighbors = ((Element) actElem.getParent()).getChildren();

                int position = neighbors.indexOf(actElem);

                if (addAtPosition.equals("predecessor") || addAtPosition.equals("successor")) {
                    if (addAtPosition.equals("successor")) {
                        position += 1;
                    }

                    if ((0 <= position) && (position <= neighbors.size())) {
                        if (!masterTemplate.equals("delete") && !masterTemplate.equals("noAction")) {
                            neighbors.add(position, new Element("item").setAttribute("href", fileName).setAttribute("type", mode).setAttribute("target", target).setAttribute("style", style).setAttribute("replaceMenu", replaceMenu).setAttribute("constrainPopUp", constrainPopUp).setAttribute("template", masterTemplate).addContent(
                                    new Element("label").setAttribute("lang", defaultLang, ns).setText(label)));
                        } else {
                            neighbors.add(position, new Element("item").setAttribute("href", fileName).setAttribute("type", mode).setAttribute("target", target).setAttribute("style", style).setAttribute("replaceMenu", replaceMenu).setAttribute("constrainPopUp", constrainPopUp).addContent(new Element("label").setAttribute("lang", defaultLang, ns).setText(label)));
                        }
                    }
                }

                if (addAtPosition.equals("child")) {
                    if (!masterTemplate.equals("delete") && !masterTemplate.equals("noAction")) {
                        Element itemElement = new Element("item").setAttribute("href", fileName).setAttribute("type", mode).setAttribute("target", target).setAttribute("style", style).setAttribute("replaceMenu", replaceMenu).setAttribute("constrainPopUp", constrainPopUp).setAttribute("template", masterTemplate);
                        itemElement.addContent(new Element("label").setAttribute("lang", defaultLang, ns).setText(label));
                        actElem.addContent(itemElement);
                    } else {
                        Element itemElement = new Element("item").setAttribute("href", fileName).setAttribute("type", mode).setAttribute("target", target).setAttribute("style", style).setAttribute("replaceMenu", replaceMenu).setAttribute("constrainPopUp", constrainPopUp);
                        itemElement.addContent(new Element("label").setAttribute("lang", defaultLang, ns).setText(label));
                        actElem.addContent(itemElement);
                    }
                }

                /* dynamic content binding */
                /* set */
                if (dcbActionAdd == true) {
                    Element addedChildElement = findActElem(root, "href", fileName);
                    Element dcb = new Element("dynamicContentBinding");
                    dcb.addContent(new Element("rootTag").setText(dcbValueAdd));
                    addedChildElement.addContent(dcb);
                }

                /* END OF: set */
                /* END OF: dynamic content binding */
                writeJDOMDocumentToFile(doc, inputFile);

                /*
                 * XMLOutputter xmlout = new
                 * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                 * xmlout.output(doc, new FileOutputStream(inputFile));
                 */
            } catch (Exception e) {
            }
        }

        if (action.equals("edit")) {
            try {
                Document doc = getXMLAsJDOM(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);

                List labels = actElem.getChildren("label");
                Iterator li = labels.iterator();

                while (li.hasNext()) {
                    Element lie = (Element) li.next();

                    if (lie.getAttributeValue("lang", ns).equals(defaultLang)) {
                        lie.setText(label);
                    }
                }

                actElem.setAttribute("target", target).setAttribute("style", style);

                if (mode.equals("extern") && (link != null)) {
                    actElem.setAttribute("href", fileName);

                    if (!masterTemplate.equals("noAction")) {
                        if (masterTemplate.equals("delete")) {
                            actElem.removeAttribute("template");
                        } else {
                            actElem.setAttribute("template", masterTemplate);
                        }
                    }

                    // fileName = link;
                } else {
                    actElem.setAttribute("replaceMenu", replaceMenu);
                    actElem.setAttribute("constrainPopUp", constrainPopUp);

                    if (!masterTemplate.equals("noAction")) {
                        if (masterTemplate.equals("delete")) {
                            actElem.removeAttribute("template");
                        } else {
                            actElem.setAttribute("template", masterTemplate);
                        }
                    }

                    // fileName = href;
                }

                /* dynamic content binding */
                /* set */
                if (dcbActionAdd == true) {
                    if (actElem.getChild("dynamicContentBinding") == null) {
                        Element dcb = new Element("dynamicContentBinding");
                        dcb.addContent(new Element("rootTag").setText(dcbValueAdd));
                        actElem.addContent(dcb);
                    } else {
                        actElem.getChild("dynamicContentBinding").addContent(new Element("rootTag").setText(dcbValueAdd));
                    }
                }

                /* END OF: set */
                /* remove */
                if (dcbActionDelete == true) {
                    List dcbChildren = actElem.getChild("dynamicContentBinding").getChildren();
                    Iterator elementIterator = dcbChildren.iterator();
                    boolean childrenRemoved = false;

                    while (childrenRemoved != true) {
                        Element child = (Element) elementIterator.next();

                        if (child.getValue().equals(dcbValueDelete)) {
                            child.detach();
                            childrenRemoved = true;
                        }
                    }

                    if (countChildren(actElem.getChild("dynamicContentBinding")) == 0) {
                        actElem.getChild("dynamicContentBinding").detach();
                    }
                }

                /* END OF: remove */
                /* END OF: dynamic content binding */
                writeJDOMDocumentToFile(doc, inputFile);

                /*
                 * XMLOutputter outputter = new
                 * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                 * outputter.output(doc, new FileOutputStream(inputFile));
                 */
            } catch (Exception e) {
            }
        }

        if (action.equals("delete")) {
            try {
                Document doc = getXMLAsJDOM(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);
                fileName = ((Element) actElem.getParent()).getAttributeValue("href");
                label = actElem.getChildText("label");
                actElem.detach();
                writeJDOMDocumentToFile(doc, inputFile);

                /*
                 * XMLOutputter outputter = new
                 * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                 * outputter.output(doc, new FileOutputStream(inputFile));
                 */
            } catch (Exception e) {
            }
        }

        if (action.equals("translate")) {
            try {
                Document doc = getXMLAsJDOM(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);

                List labels = actElem.getChildren("label");
                Iterator li = labels.iterator();
                boolean newEntry = true;

                while (li.hasNext()) {
                    Element lie = (Element) li.next();

                    if (lie.getAttributeValue("lang", ns).equals(currentLang)) {
                        lie.setText(label);
                        newEntry = false;
                    }
                }

                if (newEntry) {
                    actElem.addContent(new Element("label").setAttribute("lang", currentLang, ns).setText(label));
                }

                writeJDOMDocumentToFile(doc, inputFile);

                /*
                 * XMLOutputter xmlout = new
                 * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                 * xmlout.output(doc, new FileOutputStream(inputFile));
                 */
            } catch (Exception e) {
            }
        }
    }

    public boolean validXHTML(File htmlFile) {
        boolean validXHTML = true;

        /* just to be implemented */
        return validXHTML;
    }

    public String getParentAttribute(File inputFile, String attribute, String avalue, String parentAttribute, String altParentAttribute) {
        /*
         * builds a jdom document from inputFile searches for Element with given
         * attribute and value returns value of attribut of parent or
         * alternative value if previous one dosen't exist
         */
        String reval = "";

        try {
            Document doc = getXMLAsJDOM(inputFile);
            Element root = doc.getRootElement();
            validate(root);
            actElem = findActElem(root, attribute, avalue);

            if (((Element) actElem.getParent()).getAttributeValue(parentAttribute) == null) {
                while (actElem.getParent() instanceof org.jdom.Element) {
                    if (((Element) actElem.getParent()).getAttributeValue(altParentAttribute) != null) {
                        reval = ((Element) actElem.getParent()).getAttributeValue(altParentAttribute) + reval;
                    }

                    actElem = (Element) actElem.getParent();
                }
            } else {
                reval = ((Element) actElem.getParent()).getAttributeValue(parentAttribute);
            }
        } catch (Exception e) {
            e.printStackTrace();
            reval = "error";
        }

        return reval;
    }

    public void makeAction(String action) {
        try {
            // /////////////////////////////////////////////////////////////////////////////////////////////
            // prepare content as sdom document
            // /////////////////////////////////////////////////////////////////////////////////////////////
            Document doc = new Document();

            if (action.equals("add")) {
                if (mode.equals("intern")) {
                    if (!hrefFile.exists()) {
                        hrefFile.getParentFile().mkdir();

                        // BufferedOutputStream bo = new
                        // BufferedOutputStream(new FileOutputStream(hrefFile));
                        StringBuffer head = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").append("<!DOCTYPE MyCoReWebPage>\n").append("<MyCoReWebPage>\n").append("\t<section xml:lang=\"" + defaultLang + "\" title=\"" + label + "\">\n");
                        StringBuffer body = new StringBuffer(content);
                        StringBuffer tail = new StringBuffer("\t</section>\n").append("</MyCoReWebPage>\n");
                        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(((head).append(body).append(tail)).toString().getBytes("UTF-8")));
                        doc = getXMLAsJDOM(bis);
                        bis.close();

                        // writeJDOMDocumentToFile(doc, hrefFile);
                        // MCRUtils.copyStream(bi,bo);
                        // bo.close();
                    } else {
                        error = "Unter diesem Pfad existiert bereits ein File mit diesem Filename!";

                        return;
                    }
                } else {
                    return;
                }
            }

            if (action.equals("edit")) {
                if (mode.equals("intern")) {
                    try {
                        // SAXBuilder builder = new SAXBuilder();
                        // builder.setEntityResolver(new ResolveDTD());
                        doc = new Document();

                        try {
                            doc = getXMLAsJDOM(content);

                            Element html = doc.getRootElement();
                            doc = getXMLAsJDOM(hrefFile);

                            Element root = doc.getRootElement();
                            validate(root);

                            Element actElem = findActElem(root, "lang", defaultLang, ns);
                            actElem.setAttribute("title", label);
							if (html.getName().equals("dummyRoot")){
								actElem.setContent(html.cloneContent());
							}
							else actElem.setContent((Element)html.clone());
                        } catch (Exception ex) {
                            logger.error("Error while updating document, update rejected.", ex);
                        }

                        /*
                         * try { content = " <section
                         * xml:lang=\""+defaultLang+"\" title=\""+label+"\"
                         * >"+content+" </section>"; //content.replaceAll("&",
                         * "&amp;"); doc = getXMLAsJDOM(new
                         * StringReader(content)); Element html =
                         * doc.getRootElement(); html.detach(); //builder = new
                         * SAXBuilder(); doc = getXMLAsJDOM(hrefFile); Element
                         * root = doc.getRootElement(); validate(root); Element
                         * actElem = findActElem(root, "lang", defaultLang, ns);
                         * Element parent = (Element)actElem.getParent();
                         * parent.removeContent(actElem);
                         * parent.addContent(html); } catch (Exception e) {
                         * error = "non_valid_xhtml_content"; sessionParam =
                         * "action"; generateOutput(error, label, fileName);
                         * return; } } //writeJDOMDocumentToFile(doc, hrefFile);
                         * /*XMLOutputter xmlout = new
                         * XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                         * xmlout.output(doc, new FileOutputStream(hrefFile));
                         */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    return;
                }
            }

            if (action.equals("translate") && mode.equals("intern")) {
                try {
                    /*
                     * SAXBuilder builder = new SAXBuilder();
                     * builder.setEntityResolver(new ResolveDTD()); doc = new
                     * Document();
                     */
                    try {
                        doc = getXMLAsJDOM(contentCurrentLang);

                        Element html = doc.getRootElement();

                        // html.detach();
                        // builder = new SAXBuilder();
                        doc = getXMLAsJDOM(hrefFile);

                        Element root = doc.getRootElement();
                        validate(root);

                        Element actElem = findActElem(root, "lang", currentLang, ns);

                        if (actElem == null) {
                            root.addContent(new Element("section").setAttribute("lang", currentLang, ns).setAttribute("title", label));
                        }

                        actElem = findActElem(root, "lang", currentLang, ns);
                        actElem.setAttribute("title", label);
                        actElem.setContent(html.cloneContent());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    /*
                     * try { contentCurrentLang = " <section
                     * xml:lang=\""+currentLang+"\"
                     * title=\""+currentLangLabel+"\" >"+contentCurrentLang+"
                     * </section>"; //contentCurrentLang.replaceAll("&",
                     * "&amp;"); doc = builder.build(new
                     * StringReader(contentCurrentLang)); Element html =
                     * doc.getRootElement(); html.detach(); builder = new
                     * SAXBuilder(); doc = builder.build(hrefFile); Element root =
                     * doc.getRootElement(); validate(root); Element actElem =
                     * findActElem(root, "lang", currentLang, ns); if (actElem ==
                     * null) { root.addContent(html); } actElem =
                     * findActElem(root, "lang", currentLang, ns); Element
                     * parent = (Element)actElem.getParent();
                     * parent.removeContent(actElem); parent.addContent(html); }
                     * catch (Exception e) { error = "non_valid_xhtml_content";
                     * sessionParam = "action"; generateOutput(error, label,
                     * fileName); return; } }
                     */
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // /////////////////////////////////////////////////////////////////////////////////////////////
            // end of: prepare content as sdom document
            // /////////////////////////////////////////////////////////////////////////////////////////////
            // /////////////////////////////////////////////////////////////////////////////////////////////
            // management of prepared content jdom
            // /////////////////////////////////////////////////////////////////////////////////////////////
            if (action.equals("delete")) {
                if (realyDel.equals("true")) {
                    if (mode.equals("intern")) {
                        // if (storeTypMycore) {
                        hrefFile.delete();

                        File testFile = hrefFile;

                        while (testFile.getParentFile().listFiles().length < 1) {
                            testFile.getParentFile().delete();
                            testFile = testFile.getParentFile();
                        }

                        // }
                    }
                } else {
                    sessionParam = "choose";
                }

                return;
            }

            writeJDOMDocumentToFile(doc, hrefFile);

            // /////////////////////////////////////////////////////////////////////////////////////////////
            // end: management of prepared content jdom
            // /////////////////////////////////////////////////////////////////////////////////////////////
        } catch (FileNotFoundException e) {
            error = "File not found. For further information look at the System Output.";
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            error = "Unsupportet Encoding found. For further information look at the System Output.";
            e.printStackTrace();
        } catch (IOException e) {
            error = "IO Error. For further information look at the System Output.";
            e.printStackTrace();
        }
    }

    public boolean checkInput() {
        if (!action.equals("delete") && (((fileName == null) || fileName.equals("")) || ((label == null) || label.equals("")))) {
            error = "emptyFormField";

            return false;
        }

        /*
         * if (action.equals("add") && hrefFile.exists()) { error =
         * "fileAllreadyExists"; System.out.println("File gibts schon"); return
         * false; }
         */
        return true;
    }

    public void doItAll(File hrefFile, String action, String mode, String addAtPosition) {
        logger.debug("addAtPosition ("+addAtPosition+") will never be used."); //FIXME: use or remove addAtPosition
        // verify if html form was filled in correctly
        if ((checkInput()) == false) {
            sessionParam = "action";
            generateOutput(error, label, fileName);

            return;
        }

        // do backups
        if (!action.equals("add") && mode.equals("intern")) {
            contentFileBackup = makeBackup(hrefFile);
        }

        naviFileBackup = makeBackup(naviFile);

        // for testing
        // test();
        // update navigation base
        modifyNavi(naviFile);

        // update content page
        makeAction(action);

        // update footer with
        if (!action.equals("delete") && mode.equals("intern")) {
            addLastModifiedToContent(hrefFile);
        }

        updateFooterFile();

        // update log file
        writeToLogFile(action, contentFileBackup);

        // prepare xml container for MCRLayoutServlet
        generateOutput(error, label, fileName);
    }

    public void codeValidation(String validator) {
        logger.debug("validator ("+validator+") will never be used."); //FIXME: use or remove validator
        try {
            String contentTmp = content;
            String contentCurrentLangTmp = contentCurrentLang;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            logger.debug("Trying XHTML code validation using " + VALIDATOR);

            // JTidy Configuration
            Tidy tidy = new Tidy();
            tidy.setXHTML(true);
            tidy.setInputEncoding(OUTPUT_ENCODING);
            tidy.setOutputEncoding(OUTPUT_ENCODING);
            tidy.setWord2000(true);
            tidy.setPrintBodyOnly(true);
            tidy.setIndentContent(true);
            tidy.setForceOutput(true);
            tidy.setMakeClean(true);
            tidy.setMakeBare(true);
            tidy.setQuoteAmpersand(true);
            tidy.setQuoteMarks(true);
            tidy.setQuoteNbsp(true);
            tidy.setErrout(pw);

            if (content != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(contentTmp.getBytes(OUTPUT_ENCODING));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                tidy.parse(bais, baos);
                contentTmp = baos.toString(OUTPUT_ENCODING);
                baos.flush();
                baos.close();
                bais.close();
            }

            if (contentCurrentLang != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(contentCurrentLangTmp.getBytes(OUTPUT_ENCODING));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                tidy.parse(bais, baos);
                contentCurrentLangTmp = baos.toString(OUTPUT_ENCODING);
                baos.flush();
                baos.close();
                bais.close();
            }

            pw.flush();
            pw.close();

            if (sw.toString().indexOf("Warning: discarding unexpected") != -1) {
                logger.debug("Jumping to XML code validation because content contains at least one non-valid XHTML Element.");

                Tidy tidyXML = new Tidy();

                tidyXML.setXmlOut(true);
                tidyXML.setInputEncoding("UTF-8");
                tidyXML.setOutputEncoding("UTF-8");
                tidyXML.setWord2000(true);
                tidyXML.setIndentContent(true);
                tidyXML.setForceOutput(true);
                tidyXML.setMakeClean(true);
                tidyXML.setMakeBare(true);
                tidyXML.setQuoteAmpersand(true);
                tidyXML.setQuoteMarks(true);
                tidyXML.setQuoteNbsp(true);
                tidyXML.setXmlTags(true);

                if (content != null) {
                    contentTmp = "<dummyroot>" + content + "</dummyroot>";

                    // logger.debug("content vor parsing= "+content);
                    ByteArrayInputStream bais = new ByteArrayInputStream(contentTmp.getBytes("UTF-8"));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    tidyXML.parse(bais, baos);
                    contentTmp = baos.toString("UTF-8");
                    contentTmp = contentTmp.substring(contentTmp.indexOf(">") + 1, contentTmp.lastIndexOf("<"));

                    // logger.debug("content nach parsing= "+content);
                    bais.close();
                    baos.flush();
                    baos.close();
                }

                if (contentCurrentLang != null) {
                    contentCurrentLangTmp = "<dummyroot>" + contentCurrentLangTmp + "</dummyroot>";

                    ByteArrayInputStream baisc = new ByteArrayInputStream(contentCurrentLangTmp.getBytes("UTF-8"));
                    ByteArrayOutputStream baosc = new ByteArrayOutputStream();
                    tidyXML.parse(baisc, baosc);
                    contentCurrentLangTmp = baosc.toString("UTF-8");
                    contentCurrentLangTmp = contentCurrentLangTmp.substring(contentCurrentLangTmp.indexOf(">") + 1, contentCurrentLangTmp.lastIndexOf("<"));
                    baisc.close();
                    baosc.flush();
                    baosc.close();
                }

                usedParser = "xml";
                logger.debug("XML code validation successfully done.");
            } else {
                usedParser = "xhtml";
                logger.debug("XHTML code validation successfully done.");
            }

            content = contentTmp;
            contentCurrentLang = contentCurrentLangTmp;
        } catch (UnsupportedEncodingException e) {
            logger.debug("XHTML/XML code validation unsuccessfully.");
            e.printStackTrace();
        } catch (IOException e) {
            logger.debug("XHTML/XML code validation unsuccessfully.");
            e.printStackTrace();
        }
    }

    /**
     * Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }

    /**
     * @param req
     */
    public void setReq(HttpServletRequest req) {
        this.request = req;
    }

    public HttpServletRequest getReq() {
        return request;
    }

    public void setResp(HttpServletResponse resp) {
        this.response = resp;
    }

    public HttpServletResponse getResp() {
        return response;
    }

    /* new methods */

    // --------------
    public File getFile(String fileName) throws IOException, FileNotFoundException {
        File loadFile = new File(fileName);

        if (!loadFile.exists()) {
            logger.debug("File \"" + fileName + "\" don't exist.");
            setFile(fileName);

            // or maybe better loadFile = setFile(fileName);
            // and setting return type of setFile method to File
            // ???
            getFile(fileName);
        } else {
            if (!loadFile.isFile()) {
                logger.debug("File \"" + fileName + "\" is no valid File.");
            } else {
                if (!loadFile.canRead()) {
                    logger.debug("File \"" + fileName + "\" can't be read.");
                }

                if (!loadFile.canWrite()) {
                    logger.debug("File \"" + fileName + "\" can't be written to.");
                }
            }
        }

        return loadFile;
    }

    /**
     * @param fileName
     * @throws IOException
     */
    public void setFile(String fileName) throws IOException {
        File newFile = new File(fileName);
        newFile.createNewFile();
        logger.debug("New file \"" + fileName + "\" created.");
    }

    /**
     * @param xmlSource -
     *            any source that contains xml-valid content
     * @return JDOM Document
     */
    public Document getXMLAsJDOM(Object xmlSource) {
        SAXBuilder builder = new SAXBuilder();
        Document jdomDoc = new Document();

        try {
            if (xmlSource instanceof String) {
                jdomDoc = builder.build(new ByteArrayInputStream(((String) xmlSource).getBytes("UTF-8")));
            }

            if (xmlSource instanceof File) {
                jdomDoc = builder.build((File) xmlSource);
            }

            /*
             * if (xmlSource instanceof InputSource) { jdomDoc =
             * builder.build((InputSource)xmlSource); }
             */
            if (xmlSource instanceof InputStream) {
                jdomDoc = builder.build((InputStream) xmlSource);
            }

            /*
             * if (xmlSource instanceof Reader) { jdomDoc =
             * builder.build((Reader)xmlSource); }
             */
            if (xmlSource instanceof URL) {
                jdomDoc = builder.build((URL) xmlSource);
            }
        } catch (JDOMException e) {
            logger.debug("JDOM warning: Source is not a valid XML Document.");
            jdomDoc = validateSource(xmlSource);
        } catch (IOException e) {
            logger.debug("IO error: File \"" + xmlSource + "\" can't be parsed as JDOM.");
            e.printStackTrace();
        }

        return jdomDoc;
    }

    public Document validateSource(Object xmlSource) {
        logger.debug("Trying to build a valid XHTML/XML Document from Source using JTidy.");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        /*
         * Construct a new JTidy object and get Configuration |
         * ---------------------------------------------------+ At first,
         * allways try to parse the Document as XHTML. Only if an unknown
         * Element is found and JTidy wants to discard it, the Output format is
         * set to XML.
         */
        Tidy tidy = new Tidy();
        tidy = getTidyConfig(tidy, "xhtml");
        tidy.setErrout(pw);

        BufferedInputStream bis = null;
        ByteArrayInputStream beginTag = new ByteArrayInputStream("<dummyRoot>".getBytes());
        ByteArrayInputStream endTag = new ByteArrayInputStream("</dummyRoot>".getBytes());
        ByteArrayInputStream bais = null;
        Document jdomDoc = null;

        try {
            if (xmlSource != null) {
                if (xmlSource instanceof String) {
                    System.out.println("String");

                    // bis = new BufferedInputStream(new
                    // ByteArrayInputStream(((String)xmlSource).getBytes("UTF-8")));
                    bais = new ByteArrayInputStream(((String) xmlSource).getBytes("UTF-8"));
                }

                if (xmlSource instanceof File) {
                    System.out.println("File");

                    // bis = new BufferedInputStream(new
                    // FileInputStream((File)xmlSource));
                    bis = new BufferedInputStream(new FileInputStream((File) xmlSource));
                }

                /*
                 * if (xmlSource instanceof InputSource) {//check this later
                 * System.out.println("InputSource"); bis = new
                 * BufferedInputStream(((InputSource)xmlSource).getByteStream()); }
                 */
                if (xmlSource instanceof InputStream) { //
                    System.out.println("InputStream");
                    bis = new BufferedInputStream((InputStream) xmlSource);
                }

                /*
                 * if (xmlSource instanceof Reader) {//check this later
                 * System.out.println("Reader"); bis = new
                 * BufferedInputStream(new
                 * ByteArrayInputStream(((Reader)xmlSource).toString().getBytes("UTF-8"))); }
                 * if (xmlSource instanceof URL) { System.out.println("URL");
                 * //bis = new
                 * BufferedInputStream(((URL)xmlSource).openStream()); bis = new
                 * BufferedInputStream( new ByteArrayInputStream(new
                 * String(((URL)xmlSource).getContent().toString()).getBytes("UTF-8"))); }
                 */
            }

            SequenceInputStream sis = new SequenceInputStream(new SequenceInputStream(beginTag, bais), endTag);
            System.out.println("---begin parsing---");

            ByteArrayOutputStream baisCopy = new ByteArrayOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            if (bis == null) {
                MCRUtils.copyStream(sis, baisCopy);
                bais.close();
                sis.close();
            } else {
                MCRUtils.copyStream(bis, baisCopy);
                bis.close();
            }

            tidy.parse(new ByteArrayInputStream(baisCopy.toByteArray()), baos);
            pw.flush();
            pw.close();
            System.out.println(pw);

            if ((sw.toString().indexOf("is not recognized!") != -1) && (sw.toString().indexOf("Warning: discarding unexpected") != -1)) {
                System.out.println("Parsing Document as XML");
                tidy = new Tidy();
                tidy = getTidyConfig(tidy, "xml");
                baos.reset();
                tidy.parse(new ByteArrayInputStream(baisCopy.toByteArray()), baos);
            }

            SAXBuilder builder = new SAXBuilder();
            baos.flush();
            System.out.println("jdom = " + baos.toByteArray().toString());
            jdomDoc = builder.build(new ByteArrayInputStream(baos.toByteArray()));
            baos.flush();
            baos.close();
            System.out.println("---parsing ended---");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("jdomDoc erzeugt");

        return jdomDoc;
    }

    public Tidy getTidyConfig(Tidy tidy, String outputFormat) {
        if (outputFormat.equals("xhtml")) {
            logger.debug("Parsing Document using JTidy. Output as XHTML.");
            tidy.setXHTML(true);
            tidy.setPrintBodyOnly(true);
        } else {
            logger.debug("Parsing Document using JTidy. Output as well-formed XML.");
            tidy.setXmlOut(true);
            tidy.setXmlTags(true);
        }

        tidy.setInputEncoding(OUTPUT_ENCODING);
        tidy.setOutputEncoding(OUTPUT_ENCODING);
        tidy.setWord2000(true);
        tidy.setIndentContent(true);
        tidy.setForceOutput(true);
        tidy.setMakeClean(true);
        tidy.setMakeBare(true);
        tidy.setQuoteAmpersand(true);
        tidy.setQuoteMarks(true);
        tidy.setQuoteNbsp(true);

        return tidy;
    }

    /**
     * @param request
     */
    public void initParam(HttpServletRequest request) {
        usedParser = "none";
        sessionParam = "final";
        contentFileBackup = null;
        naviFileBackup = null;
        hrefFile = null;
        error = href = labelPath = content = label = link = dir = null;
        changeInfo = null;
        masterTemplates = new File(CONFIG.getString("MCR.WCMS.templatePath") + "master/".replace('/', File.separatorChar)).listFiles();
        userID = (String) mcrSession.get("userID");
        userClass = (String) mcrSession.get("userClass");
        userRealName = (String) mcrSession.get("userRealName");
        rootNodes = (List) mcrSession.get("rootNodes");
        action = (String) mcrSession.get("action");
        mode = (String) mcrSession.get("mode");
        href = (String) mcrSession.get("href");
        dir = (String) mcrSession.get("dir");
        currentLang = (String) mcrSession.get("currentLang");
        defaultLang = (String) mcrSession.get("defaultLang");
        
                
        if (mcrSession.get("addAtPosition") != null) {
            addAtPosition = (String) mcrSession.get("addAtPosition");
        }

        target = request.getParameter("target");
        style = request.getParameter("style");
        label = request.getParameter("label");
        content = request.getParameter("content");
        contentCurrentLang = request.getParameter("content_currentLang");

        /* code validation by JTidy */
        if (request.getParameter("codeValidationDisable") == null) {
            logger.debug("Code validation using" + VALIDATOR);
            codeValidation(VALIDATOR);
        }

        /* END: code validation by JTidy */
        currentLangLabel = request.getParameter("label_currentLang");

        /*
         * if (content != null ) { if ( content.endsWith("\n") ) content =
         * content.substring(0, content.length() - 2); }
         */
        /* check for dynamic content bindings */
        /* add */
        if ((request.getParameter("dcbActionAdd") != null) && !request.getParameter("dcbActionAdd").equals("") && (request.getParameter("dcbValueAdd") != null) && !request.getParameter("dcbValueAdd").equals("")) {
            dcbActionAdd = true;
            dcbValueAdd = request.getParameter("dcbValueAdd");
        } else {
            dcbActionAdd = false;
        }

        /* remove */
        if ((request.getParameter("dcbActionDelete") != null) && !request.getParameter("dcbActionDelete").equals("") && (request.getParameter("dcbValueDelete") != null) && !request.getParameter("dcbValueDelete").equals("")) {
            dcbActionDelete = true;
            dcbValueDelete = request.getParameter("dcbValueDelete");
        } else {
            dcbActionDelete = false;
        }

        /* END OF: check for dynamic content bindings */
        if (request.getParameter("href") != null) {
            link = request.getParameter("href"); // .toLowerCase();
        }

        changeInfo = request.getParameter("changeInfo");

        if (request.getParameter("delete") != null) {
            realyDel = request.getParameter("delete");
        } else {
            realyDel = "";
        }

        labelPath = request.getParameter("labelPath");
        replaceMenu = request.getParameter("replaceMenu");
        constrainPopUp = request.getParameter("constrainPopUp");

        if (replaceMenu == null) {
            replaceMenu = "false";
        }

        if (constrainPopUp == null) {
            constrainPopUp = "false";
        }

        masterTemplate = request.getParameter("masterTemplate");
        selfTemplate = request.getParameter("selfTemplate");

        if(selfTemplate != "")
          {
           masterTemplate = selfTemplate;
          }	
        
        fileName = href;

        /* -------------------------------------------------------------------- */
        if (action.equals("add") && mode.equals("intern")) {
            if ((link != null) && !link.toLowerCase().endsWith(".xml") && !link.toLowerCase().endsWith(".html")) {
                link = link + ".xml";
            }

            fileName = href + link;

            if (addAtPosition.equals("child")) {
                // implement here (add, intern, child)3
                labelPath = labelPath + '/' + label;

                if (href.toLowerCase().endsWith(".xml") || href.toLowerCase().endsWith(".html")) {
                    fileName = href.substring(0, href.lastIndexOf('.')) + '/' + link;
                }
            } else {
                // implement here (add, intern, predecessor, successor)1
                labelPath = labelPath.substring(0, labelPath.indexOf('/') + 1) + label;
                href = getParentAttribute(naviFile, "href", href, "href", "dir");

                if (href.toLowerCase().endsWith(".xml") || href.toLowerCase().endsWith(".html")) {
                    fileName = href.substring(0, href.lastIndexOf('.')) + '/' + link;
                } else {
                    fileName = href + link;
                }

                href = (String) mcrSession.get("href");
            }
        }

        if (action.equals("add") && mode.equals("extern")) {
            fileName = link;

            if (!(link.toLowerCase().startsWith("http") || link.toLowerCase().startsWith("ftp:") || link.toLowerCase().startsWith("mailto:"))) {
                // fileName = "http://" + link;
                fileName = link;
            }

            if (addAtPosition.equals("child")) {
                // implement here (add, extern, child)4
                labelPath = labelPath + '/' + label;
            } else {
                // implement here (add, extern, predecessor, successor)2
                labelPath = labelPath.substring(0, labelPath.indexOf('/') + 1) + label;
            }
        }

        if (action.equals("edit") && mode.equals("intern")) {
            // implement here (edit, intern)5
        }

        if (action.equals("edit") && mode.equals("extern")) {
            // implement here (edit, extern)6
            fileName = link;

            if (!(link.toLowerCase().startsWith("http") || link.toLowerCase().startsWith("ftp:") || link.toLowerCase().startsWith("mailto:"))) {
                // fileName = "http://" + link;
                fileName = link;
            }
        }

        if (action.equals("delete")) {
            // implement here (delete)7
        }

        if (action.equals("translate")) {
            label = currentLangLabel;
        }

        /*-------------------------------------------------------------------*/
        if (!dir.equals("false")) {
            attribute = "dir";
            avalue = (String) mcrSession.get("dir");
        } else {
            attribute = "href";
            avalue = href;
        }
        
                       
           hrefFile = new File(getServletContext().getRealPath("") + fileName.replace('/', fs));

        /*---------------------- Variable Test Output -------------------------*/

        // System.out.println("----- Variable Test Output ------");
        // System.out.println("hrefFile: "+hrefFile);
        // System.out.println("---------Session--------");
        // System.out.println("userID: "+userID);
        // System.out.println("userClass: "+userClass);
        // System.out.println("rootNodes: "+rootNodes);
        // System.out.println("action: "+action);
        // System.out.println("mode: "+mode);
        // System.out.println("href: "+href);
        // System.out.println("defaultLang: "+defaultLang);
        // System.out.println("currentLang: "+currentLang);
        // System.out.println("currentLangLabel: "+currentLangLabel);
        // System.out.println("---------Request--------");
        // System.out.println("label: "+label);
        // System.out.println("target: "+target);
        // System.out.println("style: "+style);
        // System.out.println("content: "+content);
        // System.out.println("contentCurrentLang: "+contentCurrentLang);
        // System.out.println("link: "+link);
        // System.out.println("realy_delete: "+realyDel);
        // System.out.println("fileName: "+fileName);
        // System.out.println("labelPath: "+labelPath);
        // System.out.println("---------Error--------");
        // System.out.println("error: "+error);
        // System.out.println("avalue: "+avalue);
        // System.out.println("replaceMenu: "+replaceMenu);
        // System.out.println("masterTemplate: "+masterTemplate);
        /*---------------------------------------------------------------------*/
    }

    /**
     * @param jdomDoc -
     *            JDOM Document
     * @param xmlFile -
     *            File the Document is written to.
     */
    public void writeJDOMDocumentToFile(Document jdomDoc, File xmlFile) throws IOException, FileNotFoundException {
        XMLOutputter xmlOut = new XMLOutputter();
        xmlOut.output(jdomDoc, new FileOutputStream(xmlFile));
        CONFIG.systemModified();
    }

    /**
     * 
     */
    public void test() throws IOException, FileNotFoundException {
        Document jdomDoc = getXMLAsJDOM(content);

        // Document jdomDoc2 = getXMLAsJDOM(getFile("c:\\test.xml"));
        // Document jdomDoc3 = getXMLAsJDOM(new
        // URL("http://java.sun.com/j2se/1.3/docs/api/java/net/URL.html"));
        writeJDOMDocumentToFile(jdomDoc, getFile("C:\\test_string.xml"));

        // writeJDOMDocumentToFile(jdomDoc2, getFile("C:\\test_file.xml"));
        // writeJDOMDocumentToFile(jdomDoc3, getFile("C:\\test_url.xml"));
    }

    // -----------------------

    /* END OF: new methods */

    /**
     * @author m5brmi-s
     * 
     * TODO To change the template for this generated type comment go to Window -
     * Preferences - Java - Code Style - Code Templates
     */
    public static class ResolveDTD implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(" "));
        }
    }

	public boolean getback() {
		return this.back;
	}

	public void setback() {
		if(request.getParameter("back")!=null &&
				request.getParameter("back").equals("true")	) {
			this.back=true;
		}
		else
		{	
			this.back = false;
		}	
	}
	public String getbackaddr() {
		return this.backaddr;
	}

	public void setbackaddr() {
		this.backaddr=request.getParameter("address");
	}
}
