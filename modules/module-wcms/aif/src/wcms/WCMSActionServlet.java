/*
 * CMSActionServlet.java
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
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUtils;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 *
 * @author  m5brmi-sh, Thomas Scheffler (yagee)
 * @version
 */
public class WCMSActionServlet extends WCMSServlet {
    private Namespace ns = Namespace.XML_NAMESPACE;

    /*Session, userDB*/
    private String userID = null; // UserID of the current user
    private String userRealName = null; // Name of the current user
    private String userClass = null; // Class which a current user belongs to {sysadmin, admin, editor, author}
    private List rootNodes; // List of nodes under wich the current user can perform actions like add, edit and delete

    /*Session, WCMSChooseServlet*/
    private String href = null;
    private String action = null;
    private String mode = null;
    private String dir = null;
    private String currentLang = null;
    private String defaultLang = null;
    private String currentLangLabel = null;

    /*Request*/
    private String target = null; // Target where to open the current file adding(editing) {_blank, _self}
    private String style = null; // Font style showing the element in the navigation {bold, normal}
    private String label = null; // Label showing the element in the navigation
    private String content = null; // Content saved as xml file
    private String contentCurrentLang = null; // Translated content of the current Language saved as xml file
    private String link = null; // Representing an url linking to the current element
    private String changeInfo = null; // Contains changes made during edit (like commit in cvs)
    private String realyDel = null; // Marks a flag for confirmation security request on the delete action
    private String labelPath = null; // Complete path of labels on the acestor axis to the current element
    private String replaceMenu = null; // Representing a Parameter, allowing an navigation element to replace the previous navigation structure only with its subelements. Can be "true" or "false".
    private String masterTemplate = null; // Represents the action of templates (set, change, remove)

    private String fileName = null; // href attribut for the new element and posiition where the element is saved on the server in case of mode = intern
    private String contentFileBackup = null; // Path for content backup (should be data type File)
    private String naviFileBackup = null; // Path for navigation.xml backup (should be data type File)

    private String error = null; // Errors, such as "empty form fields" aso
    private String attribute = null;
    private String avalue = null;

    private boolean dcbActionAdd = false;
    private String dcbValueAdd = null;
    private boolean dcbActionDelete = false;
    private String dcbValueDelete = null;

    private String sessionParam = null;
    private String addAtPosition = null;

    char fs = File.separatorChar;

    String [] imageList, documentList;
    File hrefFile;
    Element actElem;
    File [] masterTemplates;

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        sessionParam="final";
    		MCRSession mcrSession= MCRSessionMgr.getCurrentSession();
        contentFileBackup = null;
        naviFileBackup = null;
        hrefFile = null;
        error = href = labelPath = content = label = link = dir = null;
        changeInfo = null;
        masterTemplates = new File(super.CONFIG.getString("MCR.WCMS.templatePath")+"master/".replace('/', File.separatorChar)).listFiles();
        userID = (String)mcrSession.get("userID");
        userClass = (String)mcrSession.get("userClass");
        userRealName = (String)mcrSession.get("userRealName");
        rootNodes = (List)mcrSession.get("rootNodes");
        action = (String)mcrSession.get("action");
        mode = (String)mcrSession.get("mode");
        href = (String)mcrSession.get("href");
        dir = (String)mcrSession.get("dir");
        currentLang = (String)mcrSession.get("currentLang");
        defaultLang = (String)mcrSession.get("defaultLang");
        if ( mcrSession.get("addAtPosition")!= null)
            addAtPosition = (String)mcrSession.get("addAtPosition");

        target = request.getParameter("target");
        style = request.getParameter("style");
        label = request.getParameter("label");
        content = request.getParameter("content");
        //System.out.println("first in content value:"+content+"-----------------------------------------------------------------------------------------------");
        contentCurrentLang = request.getParameter("content_currentLang");

        //System.out.println("request.getParameter(codeValidationDisable) = "+request.getParameter("codeValidationDisable") +"........................................" );
        
        if ( request.getParameter("codeValidationDisable") == null ) {
        	/* xhtml code validation (valid parameters are:
        	 *  	cyberneko 	- String
        	 * 		jtidy 		- String )
        	 */
        	System.out.println("code validation an");
        	codeValidation(VALIDATOR);
        	
        }	
        currentLangLabel = request.getParameter("label_currentLang");

        /*if (content != null ) {
            if ( content.endsWith("\n") ) content = content.substring(0, content.length() - 2);
        }*/

        /* check for dynamic content bindings */
        /* add */


        if ( request.getParameter("dcbActionAdd") != null && !request.getParameter("dcbActionAdd").equals("")
			&& request.getParameter("dcbValueAdd") != null && !request.getParameter("dcbValueAdd").equals("") ) {
			dcbActionAdd = true;
			dcbValueAdd = request.getParameter("dcbValueAdd");
        }
        else dcbActionAdd = false;
        /* remove */
		if ( request.getParameter("dcbActionDelete") != null && !request.getParameter("dcbActionDelete").equals("")
			&& request.getParameter("dcbValueDelete") != null && !request.getParameter("dcbValueDelete").equals("") ) {
			dcbActionDelete = true;
			dcbValueDelete = request.getParameter("dcbValueDelete");
		}
		else dcbActionDelete = false;
		/* END OF: check for dynamic content bindings */

        if ( request.getParameter("href") != null)
            link = request.getParameter("href"); //.toLowerCase();

        changeInfo = request.getParameter("changeInfo");
        if ( request.getParameter("delete") != null )
            realyDel = request.getParameter("delete");
        else realyDel = "";

        labelPath = request.getParameter("labelPath");
        replaceMenu = request.getParameter("replaceMenu");
        if ( replaceMenu == null ) replaceMenu = "false";
        masterTemplate = request.getParameter("masterTemplate");
        File naviFile = new File(super.CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));

        fileName = href;
 /* -------------------------------------------------------------------- */

        if ( action.equals("add") && mode.equals("intern") ) {
            if ( link != null && !link.toLowerCase().endsWith(".xml") && !link.toLowerCase().endsWith(".html") ) link = link + ".xml";
            fileName = href + link;
            if( addAtPosition.equals("child") ) {
		//implement here (add, intern, child)3
                labelPath = labelPath + '/' +label;
                if ( href.toLowerCase().endsWith(".xml") || href.toLowerCase().endsWith(".html") ) {
                    fileName = href.substring(0, href.lastIndexOf('.')) + '/' + link;
                }
            }
            else {
		//implement here (add, intern, predecessor, successor)1
                labelPath = labelPath.substring(0, labelPath.indexOf('/')+1)+label;
                href = getParentAttribute( naviFile, "href", href, "href", "dir" );
                if ( href.toLowerCase().endsWith(".xml") || href.toLowerCase().endsWith(".html") ) {
                    fileName = href.substring(0, href.lastIndexOf('.')) + '/' + link;
                }
                else {
                    fileName = href + link;
                }
                href = (String)mcrSession.get("href");
            }
        }

        if ( action.equals("add") && mode.equals("extern") ) {
            fileName = link;
            if ( !(link.toLowerCase().startsWith("http") || link.toLowerCase().startsWith("ftp:") || link.toLowerCase().startsWith("mailto:")) ) {
                //fileName = "http://" + link;
            	fileName = link;            	
            }

            if( addAtPosition.equals("child") ) {
		//implement here (add, extern, child)4
                labelPath = labelPath+ '/' +label;
            }
            else {
		//implement here (add, extern, predecessor, successor)2
                labelPath = labelPath.substring(0, labelPath.indexOf('/')+1)+label;
            }
        }

        if ( action.equals("edit") && mode.equals("intern") ) {
            //implement here (edit, intern)5
        }

        if ( action.equals("edit") && mode.equals("extern") ) {
            //implement here (edit, extern)6
            fileName = link;
            if ( !(link.toLowerCase().startsWith("http") || link.toLowerCase().startsWith("ftp:") || link.toLowerCase().startsWith("mailto:")) ) {
                //fileName = "http://" + link;
            	fileName = link;
            }
        }

        if ( action.equals("delete") ) {
            //implement here (delete)7
        }

        if ( action.equals("translate") ) label = currentLangLabel;

/*-------------------------------------------------------------------*/

        if (!dir.equals("false")) {
            attribute = "dir";
            avalue = (String)mcrSession.get("dir");
        }

        else {
            attribute = "href";
            avalue = href;
        }

        hrefFile = new File(getServletContext().getRealPath("") + fileName.replace('/', fs));

        /*---------------------- Variable Test Output -------------------------*/
        //System.out.println("----- Variable Test Output ------");
        //System.out.println("hrefFile: "+hrefFile);

        //System.out.println("---------Session--------");
        //System.out.println("userID: "+userID);
        //System.out.println("userClass: "+userClass);
        //System.out.println("rootNodes: "+rootNodes);
        //System.out.println("action: "+action);
        //System.out.println("mode: "+mode);
        //System.out.println("href: "+href);
        //System.out.println("defaultLang: "+defaultLang);
        //System.out.println("currentLang: "+currentLang);
        //System.out.println("currentLangLabel: "+currentLangLabel);
        //System.out.println("---------Request--------");
        //System.out.println("label: "+label);
        //System.out.println("target: "+target);
        //System.out.println("style: "+style);
        //System.out.println("content: "+content);
        //System.out.println("contentCurrentLang: "+contentCurrentLang);
        //System.out.println("link: "+link);
        //System.out.println("realy_delete: "+realyDel);
        //System.out.println("fileName: "+fileName);
        //System.out.println("labelPath: "+labelPath);
        //System.out.println("---------Error--------");
        //System.out.println("error: "+error);
        //System.out.println("avalue: "+avalue);

        //System.out.println("replaceMenu: "+replaceMenu);
		//System.out.println("masterTemplate: "+masterTemplate);
        /*---------------------------------------------------------------------*/

        if ( !realyDel.equals("false") ) {
            doItAll(request, response, naviFile, hrefFile, action, mode, addAtPosition );
        }
        else {
            sessionParam="choose";
            generateOutput(request, error, response, naviFile, label, fileName);
        }
    }

    public void generateOutput(HttpServletRequest request, String error, HttpServletResponse response, File naviFile, String label, String fileName){
        try {
        	MCRSession mcrSession= MCRSessionMgr.getCurrentSession();
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(naviFile);
            Element root = doc.getRootElement();
            validate(root);
            Element rootOut = new Element("cms");
            Document jdom = new Document(rootOut);
            rootOut.addContent(new Element("session").setText(sessionParam));
            rootOut.addContent(new Element("href").setText(fileName));
            rootOut.addContent(new Element("label").setText(label));
            rootOut.addContent(new Element("target").setText(target));
            rootOut.addContent(new Element("style").setText(style));
            rootOut.addContent(new Element("addAtPosition").setText(addAtPosition));
            rootOut.addContent(new Element("action").setAttribute("mode", mode).setText(action));
            rootOut.addContent(new Element("sessionID").setText(mcrSession.getID()));
            rootOut.addContent(new Element("userID").setText(userID));
            rootOut.addContent(new Element("userClass").setText(userClass));
            Iterator rootNodesIterator = rootNodes.iterator();
            while (rootNodesIterator.hasNext()) {
                Element rootNode = (Element)rootNodesIterator.next();
                rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
            }
            if ( action.equals("delete") ) {
                File [] contentTemplates = new File((super.CONFIG.getString("MCR.WCMS.templatePath")+"content/").replace('/', File.separatorChar)).listFiles();
                Element templates = new Element("templates");
                Element contentTemp = new Element("content");
                for (int i=0; i < contentTemplates.length; i++) {
                    if ( !contentTemplates[i].isDirectory() ) {
                        contentTemp.addContent(new Element("template").setText(contentTemplates[i].getName()));
                    }
                }
                templates.addContent(contentTemp);
                rootOut.addContent(templates);
            }
            else {
                imageList = null;
                documentList = null;

                Element images = new Element("images");
                rootOut.addContent(images);
                imageList = (new File(super.CONFIG.getString("MCR.WCMS.imagePath").replace('/', File.separatorChar))).list();
                for (int i=0; i < imageList.length; i++) {
                    images.addContent(new Element("image").setText(super.CONFIG.getString("MCR.WCMS.imagePath")+imageList[i]));
                }

                Element documents = new Element("documents");
                rootOut.addContent(documents);
                documentList = (new File(super.CONFIG.getString("MCR.WCMS.documentPath").replace('/', File.separatorChar))).list();
                for (int i=0; i < documentList.length; i++) {
                    documents.addContent(new Element("document").setText(super.CONFIG.getString("MCR.WCMS.imagePath")+documentList[i]));
                }
                Element templates = new Element("templates");
                Element master = new Element("master");
                for (int i = 0; i < masterTemplates.length; i++ ) {
                    if ( masterTemplates[i].isDirectory() && masterTemplates[i].getName().compareToIgnoreCase("cvs") != 0) {
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
					}
					catch (JDOMException jex) {
						content = "<section>"+content+"</section>";
                    	saxDoc = saxb.build(new StringReader(content));
                       	Element html = saxDoc.getRootElement();
                       	html.detach();
			           	Element contentElem = new Element("section");
			           	contentElem.setContent(html);
			           	rootOut.addContent(contentElem);
            			rootOut.addContent(new Element("error").setText(error));
					}
            	}
            	else {
					try {
						saxDoc = saxb.build(new StringReader(contentCurrentLang));
			           	Element html = saxDoc.getRootElement();
			           	html.detach();
			           	Element contentElem = new Element("section");
			           	contentElem.addContent(html);
			           	rootOut.addContent(contentElem);
            			rootOut.addContent(new Element("error").setText(error));
					}
					catch (JDOMException jex) {
						content = "<section>"+contentCurrentLang+"</section>";
                    	saxDoc = saxb.build(new StringReader(contentCurrentLang));
                    	Element html = saxDoc.getRootElement();
			           	html.detach();
			           	Element contentElem = new Element("section");
			           	contentElem.setContent(html);
			           	rootOut.addContent(contentElem);
            			rootOut.addContent(new Element("error").setText(error));
					}
				}
            }

            request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            //request.setAttribute("XSL.Style", "xml");
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        }

        catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }
    }

    public void updateFooter() {
        try {
            File footer = new File(super.CONFIG.getString("MCR.WCMS.footer").replace('/', File.separatorChar));
            SAXBuilder builder = new SAXBuilder();
            Document doc;
            if (!footer.exists()) {
                footer.getParentFile().mkdirs();
                doc = builder.build(footer);
                Element root = doc.getRootElement();
                //System.out.println("Footer gibts noch nicht.");
            }
            else doc = builder.build(footer);
            Element root = doc.getRootElement();
            root.setAttribute("date", getDate())
                .setAttribute("time", getTime())
                .setAttribute("labelPath", labelPath)
                .setAttribute("lastEditor", userRealName);
            XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
            xmlout.output(doc, new FileOutputStream(footer));
        }
        catch (Exception e) {
            e.printStackTrace();
            //System.out.println(e.getMessage());
        }
    }

    public void updateXMLFileFooter(File hrefFile) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(hrefFile);
            Element root = doc.getRootElement();
            if (root.getChild("meta") != null){
                if (root.getChild("meta").getChild("log") != null){
                root.getChild("meta").getChild("log").setAttribute("date", getDate())
                                                     .setAttribute("time", getTime())
                                                     .setAttribute("labelPath", labelPath)
                                                     .setAttribute("lastEditor", userRealName);
                }
                else root.getChild("meta").addContent(new Element("log").setAttribute("date", getDate())
                                                                        .setAttribute("time", getTime())
                                                                        .setAttribute("labelPath", labelPath)
                                                                        .setAttribute("lastEditor", userRealName));
            }
            else {
                Element meta = new Element("meta");
                meta.addContent(new Element("log").setAttribute("date", getDate())
                                                  .setAttribute("time", getTime())
                                                  .setAttribute("labelPath", labelPath)
                                                  .setAttribute("lastEditor", userRealName));
                root.addContent(meta);
            }
            XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
            xmlout.output(doc, new FileOutputStream(hrefFile));
        }
        catch (Exception e) {
            e.printStackTrace();
            //System.out.println(e.getMessage());
        }
    }

    public void writeToLogFile(String action, String contentFileBackup) {
        try {
            File logFile = new File(super.CONFIG.getString("MCR.WCMS.logFile").replace('/', File.separatorChar));
            SAXBuilder builder = new SAXBuilder();
            Document doc;
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
                doc = new Document(new Element("loggings"));
                //System.out.println("Logfile wurde unter"+logFile.toString()+"angelegt.");
            }
            else doc = builder.build(logFile);
            Element root = doc.getRootElement();
            if (contentFileBackup == null) contentFileBackup = "";
            if (changeInfo == null) {
                root.addContent(new Element("log").setAttribute("date", getDate())
                                                  .setAttribute("time", getTime())
                                                  .setAttribute("userRealName", userRealName)
                                                  .setAttribute("labelPath", labelPath)
                                                  .setAttribute("doneAction", action)
                                                  .setAttribute("backupContentFile", contentFileBackup)
                                                  .setAttribute("backupNavigationFile", naviFileBackup));
            }
            else {
                Element log = new Element("log");
                log.setAttribute("date", getDate())
                   .setAttribute("time", getTime())
                   .setAttribute("userRealName", userRealName)
                   .setAttribute("labelPath", labelPath)
                   .setAttribute("doneAction", action)
                   .setAttribute("backupContentFile", contentFileBackup)
                   .setAttribute("backupNavigationFile", naviFileBackup)
                   .addContent(new Element("note").setText(changeInfo));
                root.addContent(log);
            }

            XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
            xmlout.output(doc, new FileOutputStream(logFile));
        }
        catch (Exception e) {
            e.printStackTrace();
            //System.out.println(e.getMessage());
        }
    }

    public String getDate(){
        Calendar gregCal = GregorianCalendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(gregCal.getTime());
        return date;
    }

    public String getTime(){
        Calendar gregCal = GregorianCalendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time = sdf.format(gregCal.getTime());
        return time;
    }

    public void validate(Element element) {
        List elements = element.getChildren();
        Iterator elementIterator = elements.iterator();

        while (elementIterator.hasNext()) {
            Element child = (Element)elementIterator.next();
            if (child.getAttribute("href") != null) {
                if (child.getAttributeValue("href").equals(href)) {
                    mode = child.getAttributeValue("type");
                    while (((Element)child.getParent()).getChild("label") != null){
                        child = (Element)child.getParent();
                    }
                    return;
                }
                validate (child);
            }
        }
    }

    public Element findActElem(Element element, String attr, String value) {
        List elements = element.getChildren();
        Iterator elementIterator = elements.iterator();
        Element tempresult;

        while (elementIterator.hasNext()) {
            Element child = (Element)elementIterator.next();
            if (child.getAttribute(attr) != null && child.getAttribute(attr).getValue().equals(value)) {
                return child;
            }
            tempresult=findActElem(child, attr, value);
            if (tempresult != null) return tempresult;
        }
        return null;
    }

    public Element findActElem(Element element, String attr, String value, Namespace ns) {
	        List elements = element.getChildren();
	        Iterator elementIterator = elements.iterator();
	        Element tempresult;

	        while (elementIterator.hasNext()) {
	            Element child = (Element)elementIterator.next();
	            if (child.getAttribute(attr, ns) != null && child.getAttributeValue(attr, ns).equals(value)) {
	                return child;
	            }
	            tempresult=findActElem(child, attr, value, ns);
	            if (tempresult != null) return tempresult;
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

    public String makeBackup (File inputFile) {
        File backupFile = null;
        if (inputFile.toString().endsWith(fs+"navigation.xml")) {
            backupFile = new File(super.CONFIG.getString("MCR.WCMS.backupPath").replace('/', File.separatorChar)+fs+"navi"+fs+"navigation.xml");
        }
        else backupFile = new File(super.CONFIG.getString("MCR.WCMS.backupPath").replace('/', File.separatorChar)+href.replace('/', File.separatorChar));
        if (inputFile.exists()) {
            try {
                BufferedInputStream bi = new BufferedInputStream(new FileInputStream(inputFile));
                if ( backupFile.exists() ) {
                    int version = 1;
                    String backupPath = backupFile.toString();
                    while ( backupFile.exists() ) {
                        backupFile = new File(backupPath+"."+String.valueOf(version));
                        version++;
                    }
                }
                else {
                    backupFile.getParentFile().mkdirs();
                    //backupFile.createNewFile();
                }
                BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(backupFile));
                MCRUtils.copyStream(bi,bo);
                bi.close();
                bo.close();
            }

            catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                error = fnfe.getMessage();

            }
            catch (IOException ioe) {
                ioe.printStackTrace();
                error = ioe.getMessage();
            }
        }
        return backupFile.toString();
    }

    public void modify(File inputFile) {
        if (action.equals("add")) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                Element actElem = findActElem(root, attribute, avalue);
                List neighbors = ((Element)actElem.getParent()).getChildren();

                int position = neighbors.indexOf(actElem);
                if (addAtPosition.equals("predecessor") || addAtPosition.equals("successor")) {
                    if (addAtPosition.equals("successor")) position += 1;
                    if (0 <= position && position <= neighbors.size())
                    {
						if ( !masterTemplate.equals("delete") && !masterTemplate.equals("noAction") )
						{
	                        neighbors.add(position, new Element("item")
	                                                                   .setAttribute("href", fileName)
	                                                                   .setAttribute("type", mode)
	                                                                   .setAttribute("target", target)
	                                                                   .setAttribute("style", style)
	                                                                   .setAttribute("replaceMenu", replaceMenu)
                                                                           .setAttribute("template", masterTemplate)
                                                                           .addContent( new Element("label").setAttribute("lang",defaultLang, ns).setText(label)));
						}
						else
						{
							neighbors.add(position, new Element("item")
                                                   .setAttribute("href", fileName)
                                                   .setAttribute("type", mode)
												   .setAttribute("target", target)
                                                   .setAttribute("style", style)
												   .setAttribute("replaceMenu", replaceMenu)
                                                   .addContent(new Element("label").setAttribute("lang", defaultLang, ns).setText(label)));
						}
                    }
                }
                if (addAtPosition.equals("child")) {
                    if ( !masterTemplate.equals("delete") && !masterTemplate.equals("noAction") ) {
						Element itemElement = new Element("item").setAttribute("href", fileName)
																						.setAttribute("type", mode)
																						.setAttribute("target", target)
																						.setAttribute("style", style)
																						.setAttribute("replaceMenu", replaceMenu)
																						.setAttribute("template", masterTemplate);
						itemElement.addContent( new Element("label").setAttribute("lang", defaultLang, ns).setText(label) );
						actElem.addContent(itemElement);
                    }
                    else {
						Element itemElement = new Element("item").setAttribute("href", fileName)
																						.setAttribute("type", mode)
																						.setAttribute("target", target)
																						.setAttribute("style", style)
																						.setAttribute("replaceMenu", replaceMenu);
						itemElement.addContent(new Element("label").setAttribute("lang", defaultLang, ns).setText(label));
						actElem.addContent(itemElement);
                    }
                }

				/* dynamic content binding */
				/* set */
				if ( dcbActionAdd == true ) {
					Element addedChildElement = findActElem(root, "href", fileName);
					Element dcb = new Element("dynamicContentBinding");
					dcb.addContent( new Element("rootTag").setText(dcbValueAdd) );
					addedChildElement.addContent(dcb);
				}
				/* END OF: set */
				/* END OF: dynamic content binding */

                XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                xmlout.output(doc, new FileOutputStream(inputFile));
            }
            catch (Exception e) {}
        }

        if (action.equals("edit")) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);
                List labels = actElem.getChildren("label");
                Iterator li = labels.iterator();
                while (li.hasNext()) {
                    Element lie = (Element)li.next();
                    if (lie.getAttributeValue("lang", ns).equals(defaultLang)) {
                        lie.setText(label);
                    }
                }
                actElem.setAttribute("target", target)
                       .setAttribute("style", style);
                if ( mode.equals("extern") && link != null ) {
                    actElem.setAttribute("href", fileName);
					if ( !masterTemplate.equals("noAction") ) {
						if ( masterTemplate.equals("delete") ) {
						   actElem.removeAttribute("template");
						}
						else {
						   actElem.setAttribute("template", masterTemplate);
						}
					}

                    //fileName = link;
                }
                else {
                     actElem.setAttribute("replaceMenu", replaceMenu);

                     if ( !masterTemplate.equals("noAction") ) {
						 if ( masterTemplate.equals("delete") ) {
						 	actElem.removeAttribute("template");
						 }
						 else {
							actElem.setAttribute("template", masterTemplate);
						 }
                     }
                    //fileName = href;
                }

                /* dynamic content binding */
                /* set */
                if ( dcbActionAdd == true ) {
					if ( actElem.getChild("dynamicContentBinding") == null ) {
					Element dcb = new Element("dynamicContentBinding");
					dcb.addContent( new Element("rootTag").setText(dcbValueAdd) );
					actElem.addContent(dcb);
					}
					else actElem.getChild("dynamicContentBinding").addContent( new Element("rootTag").setText(dcbValueAdd) );
                }
                /* END OF: set */
                /* remove */
				if ( dcbActionDelete == true ) {
					List dcbChildren = actElem.getChild("dynamicContentBinding").getChildren();
					Iterator elementIterator = dcbChildren.iterator();
					boolean childrenRemoved = false;
					while ( childrenRemoved != true ) {
						Element child = (Element)elementIterator.next();
						if ( child.getValue().equals(dcbValueDelete) ) {
							child.detach();
							childrenRemoved = true;
						}
					}
					if ( countChildren(actElem.getChild("dynamicContentBinding")) == 0 ) actElem.getChild("dynamicContentBinding").detach();
				}
                /* END OF: remove */
				/* END OF: dynamic content binding */

                XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                outputter.output(doc, new FileOutputStream(inputFile));
            }
            catch (Exception e) {}
        }
        if (action.equals("delete")) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);
                fileName = ((Element)actElem.getParent()).getAttributeValue("href");
                label = actElem.getChildText("label");
                actElem.detach();

                XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                outputter.output(doc, new FileOutputStream(inputFile));
            }
            catch (Exception e) {}
        }

        if (action.equals("translate")) {
            try {
                SAXBuilder builder = new SAXBuilder();
                Document doc = builder.build(inputFile);
                Element root = doc.getRootElement();
                validate(root);
                actElem = findActElem(root, attribute, avalue);
                List labels = actElem.getChildren("label");
                Iterator li = labels.iterator();
                boolean newEntry = true;
                while (li.hasNext()) {
                    Element lie = (Element)li.next();
                    if (lie.getAttributeValue("lang", ns).equals(currentLang)) {
                        lie.setText(label);
                        newEntry = false;
                    }
                }
                if (newEntry) {
                    actElem.addContent(new Element("label").setAttribute("lang", currentLang, ns).setText(label));
                }
                XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                xmlout.output(doc, new FileOutputStream(inputFile));
            }
            catch (Exception e) {}
        }
    }

    public boolean validXHTML( File htmlFile ) throws IOException {
        boolean validXHTML = true;
        /*just to be implemented */
        return validXHTML;
    }

    public String getParentAttribute( File inputFile, String attribute, String avalue, String parentAttribute, String altParentAttribute ) {
        /* builds a jdom document from inputFile
         * searches for Element with given attribute and value
         * returns value of attribut of parent or alternative value if previous one dosen't exist
         */
        String reval = "";
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(inputFile);
            Element root = doc.getRootElement();
            validate(root);
            actElem = findActElem(root, attribute, avalue);
            if ( ((Element)actElem.getParent()).getAttributeValue(parentAttribute) == null ) {
                while ( actElem.getParent() instanceof org.jdom.Element ) {
                    if ( ((Element)actElem.getParent()).getAttributeValue(altParentAttribute) != null ) {
                        reval = ((Element)actElem.getParent()).getAttributeValue(altParentAttribute) + reval;
                    }
                    actElem = (Element)actElem.getParent();
                }
            }
            else {
                reval = ((Element)actElem.getParent()).getAttributeValue(parentAttribute);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            reval = "error";
        }
        return reval;
    }

    public void makeAction( String action ) throws IOException {
        if ( action.equals("add") ) {
            if ( mode.equals("intern") ) {
                if ( !hrefFile.exists() ) {
                    hrefFile.getParentFile().mkdir();
                    BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(hrefFile));
                    StringBuffer output=new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
						.append("<!DOCTYPE MyCoReWebPage>\n")
						.append("<MyCoReWebPage>\n")
						.append("\t<section xml:lang=\""+defaultLang+"\" title=\""+label+"\">\n")
						.append(content)
						.append("\t</section>\n")
						.append("</MyCoReWebPage>\n");
                    BufferedInputStream bi = new BufferedInputStream(
							new ByteArrayInputStream(output.toString()
									.getBytes(OUTPUT_ENCODING)));
                    MCRUtils.copyStream(bi,bo);
                    bi.close();
                    bo.close();
                }
                else {
                    error = "Unter diesem Pfad existiert bereits ein File mit diesem Filename!";
                    return;
                }
            }
            else return;
        }
 /*---- build new ----*/
        if ( action.equals("edit") ) {
            if ( mode.equals("intern") ) {
                try {
                    SAXBuilder builder = new SAXBuilder();
                    builder.setEntityResolver(new ResolveDTD());
                    Document doc = new Document();
                    try {
                    	doc = builder.build(new StringReader(content));
                    	Element html = doc.getRootElement();
                    	html.detach();
                    	builder = new SAXBuilder();
						doc = builder.build(hrefFile);
						Element root = doc.getRootElement();
						validate(root);
						Element actElem = findActElem(root, "lang", defaultLang, ns);
						actElem.setAttribute("title", label);
                    	actElem.setContent(html);
		    		}
					catch (JDOMException jex) {
						content = "<section xml:lang=\""+defaultLang+"\" title=\""+label+"\" >"+content+"</section>";
						//content.replaceAll("&", "&amp;");
						doc = builder.build(new StringReader(content));
						Element html = doc.getRootElement();
						html.detach();
						builder = new SAXBuilder();
						doc = builder.build(hrefFile);
						Element root = doc.getRootElement();
						validate(root);
						Element actElem = findActElem(root, "lang", defaultLang, ns);
						Element parent = (Element)actElem.getParent();
						parent.removeContent(actElem);
						parent.addContent(html);
					}

                    XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                    xmlout.output(doc, new FileOutputStream(hrefFile));
                }
                catch (JDOMException je) {
					je.printStackTrace();
                }

               /* PrintWriter po = new PrintWriter(new BufferedWriter(new FileWriter(hrefFile)));
                po.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                po.println("<!DOCTYPE MyCoReWebPage>");
                po.println("<MyCoReWebPage>");
                po.println("\t<section xml:lang=\""+defaultLang+"\" title=\""+label+"\" >");
                po.print(content);
                po.println("\t</section>");
                po.println("</MyCoReWebPage>");
                po.flush();
                po.close();*/
            }
            else return;
        }

        if ( action.equals("translate") && mode.equals("intern") ) {
            try {

                SAXBuilder builder = new SAXBuilder();
                builder.setEntityResolver(new ResolveDTD());
                Document doc = new Document();
                try {
                	doc = builder.build(new StringReader(contentCurrentLang));
                	Element html = doc.getRootElement();
                	html.detach();
                	builder = new SAXBuilder();
                	doc = builder.build(hrefFile);
                	Element root = doc.getRootElement();
                	validate(root);
                	Element actElem = findActElem(root, "lang", currentLang, ns);
                	if (actElem == null) {
                	    root.addContent(new Element("section").setAttribute("lang", currentLang, ns)
                	                                          .setAttribute("title", label));
                	}
                	actElem = findActElem(root, "lang", currentLang, ns);
                	actElem.setAttribute("title", label);
                	actElem.setContent(html);
				}

                catch (JDOMException jex) {
					contentCurrentLang = "<section xml:lang=\""+currentLang+"\" title=\""+currentLangLabel+"\" >"+contentCurrentLang+"</section>";
					//contentCurrentLang.replaceAll("&", "&amp;");
					doc = builder.build(new StringReader(contentCurrentLang));
					Element html = doc.getRootElement();
					html.detach();
					builder = new SAXBuilder();
					doc = builder.build(hrefFile);
					Element root = doc.getRootElement();
					validate(root);
					Element actElem = findActElem(root, "lang", currentLang, ns);
					if (actElem == null) {
					    root.addContent(html);
                	}
					actElem = findActElem(root, "lang", currentLang, ns);
					Element parent = (Element)actElem.getParent();
					parent.removeContent(actElem);
					parent.addContent(html);
				}

                XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                xmlout.output(doc, new FileOutputStream(hrefFile));
            }
            catch (JDOMException je) {
				je.printStackTrace();
            }
                /*PrintWriter po = new PrintWriter(new BufferedWriter(new FileWriter(hrefFile)));
                po.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                po.println("<!DOCTYPE MyCoReWebPage>");
                po.println("<MyCoReWebPage>");
                po.println("\t<section xml:lang=\""+currentLang+"\" title=\""+label+"\">");
                po.print(contentCurrentLang);
                po.println("\t</section>");
                po.println("</MyCoReWebPage>");
                po.flush();
                po.close();*/
        }

/*---- END ----*/

        if ( action.equals("delete") ) {
            if ( realyDel.equals("true") ) {
                if ( mode.equals("intern") ) {
                    hrefFile.delete();
                    File testFile = hrefFile;
                    while (testFile.getParentFile().listFiles().length < 1) {
                        testFile.getParentFile().delete();
                        testFile = testFile.getParentFile();
                    }
                }
            }
            else sessionParam = "choose";
        }
    }

    public boolean checkInput() {
        if ( !action.equals("delete") && ((fileName == null || fileName.equals("")) || (label == null || label.equals(""))) ) {
            error = "emptyFormField";
            return false;
        }
        /*if (action.equals("add") && hrefFile.exists()) {
			error = "fileAllreadyExists";
			System.out.println("File gibts schon");
			return false;
		}*/
        return true;
    }

    public void doItAll( HttpServletRequest request, HttpServletResponse response, File naviFile, File hrefFile, String action, String mode, String addAtPosition) throws IOException {
        if ( (checkInput()) == false ) {
            sessionParam = "action";
            generateOutput(request, error, response, naviFile, label, fileName);
            return;
        }
        if ( !action.equals("add") && mode.equals("intern") ) contentFileBackup = makeBackup(hrefFile);
        naviFileBackup = makeBackup(naviFile);
        modify(naviFile);
        makeAction(action);
        if ( !action.equals("delete") && mode.equals("intern") ) updateXMLFileFooter(hrefFile);
        updateFooter();
        writeToLogFile(action, contentFileBackup);
        generateOutput(request, error, response, naviFile, label, fileName);
    }

    public void codeValidation(String validator) {
    	System.out.println("XHTML code validation using "+VALIDATOR);
    	//JTidy Configuration
    	Tidy tidy = new Tidy();
    	//Dict dict = new Dict("toc", (short)4, 8, ParserImpl.BLOCK, null);
    	tidy.setXHTML(true);
    	tidy.setInputEncoding(OUTPUT_ENCODING);
    	tidy.setOutputEncoding(OUTPUT_ENCODING);
    	tidy.setPrintBodyOnly(true);
    	tidy.setIndentContent(true);
    	tidy.setForceOutput(true);
    	tidy.setMakeClean(true);
    	tidy.setMakeBare(true);
    	tidy.setQuoteAmpersand(true);
    	tidy.setQuoteMarks(true);
    	tidy.setQuoteNbsp(true);
    	
    	if (content != null) {
    		try {
    			ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(OUTPUT_ENCODING));
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			if (validator.equals("cyberneko")) {
    				org.cyberneko.html.parsers.DOMParser parser = new 
					org.cyberneko.html.parsers.DOMParser();
    				parser.parse( new InputSource(bais) );
    				System.out.println(parser.getDocument().getChildNodes());
    				DOMBuilder builder = new DOMBuilder();
    				Document document = builder.build( parser.getDocument() );                
                    XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                    xmlout.output(document.getRootElement().getChild("BODY").getContent(), baos);
                    content = baos.toString(OUTPUT_ENCODING);
    			}
    			else {
    				tidy.parse(bais, baos);  				
    			}
    			content = baos.toString(OUTPUT_ENCODING);
				baos.flush();
				baos.close();
    			bais.close();
    		}
    		catch (Exception e){
    			e.printStackTrace();
    		}
    	}
    	
    	if (contentCurrentLang != null) {
    		try {
    			ByteArrayInputStream bais = new ByteArrayInputStream(contentCurrentLang.getBytes(OUTPUT_ENCODING));
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			if (validator.equals("cyberneko")) {
    				org.cyberneko.html.parsers.DOMParser parser = new 
					org.cyberneko.html.parsers.DOMParser();
    				parser.parse( new InputSource(bais) );
    				DOMBuilder builder = new DOMBuilder();
    				Document document = builder.build( parser.getDocument() );                
                    XMLOutputter xmlout = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding(OUTPUT_ENCODING));
                    xmlout.output(document.getRootElement().getChild("BODY").getContent(), baos);
    			}
    			else {
    				tidy.parse(bais, baos);
    			}
    			contentCurrentLang = baos.toString(OUTPUT_ENCODING);
				baos.flush();
				baos.close();
    			bais.close();   		
    		}
    		catch (Exception e){
    			e.printStackTrace();
    		}
    	}
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
	public static class ResolveDTD implements EntityResolver {
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader(" "));
		}
	}
}