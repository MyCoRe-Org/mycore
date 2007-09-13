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

package org.mycore.frontend.wcms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.MCRLayoutUtilities;

public class MCRWCMSAdminServlet extends MCRWCMSServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRWCMSAdminServlet.class);

    /*
     * (non-Javadoc)
     * 
     * @see wcms.WCMSServlet#processRequest(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // http://141.35.20.199:8291/servlets/MCRWCMSAdminServlet;jsessionid=32e28trrnp07r?action=logs&sort=date&sortOrder=descending

        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String todo = getTodo(request);

        // generate output XML
        Element root = new Element("cms");
        Document jdom = new Document(root);
        root.addContent(new Element("session").setText(todo));
        root.addContent(new Element("userID").setText(mcrSession.get("userID").toString()));
        root.addContent(new Element("userClass").setText(mcrSession.get("userClass").toString()));

        // process request
        if (todo.equals("exit")) {
            exitWCMS(request, response);
        } else if (todo.equals("getMultimediaConfig")) {
            Document docOut = new Document(new Element("cms"));
            getMultimediaConfig(docOut.getRootElement());
            getLayoutService().sendXML(request, response, docOut);
        } else if (todo.equals("choose")) {
            generateXML_managPage(mcrSession, root);
            getLayoutService().doLayout(request, response, jdom);
        } else if (todo.equals("logs")) {
            generateXML_logs(request, root);
            getLayoutService().doLayout(request, response, jdom);
        } else if (todo.equals("managGlobal") && mcrSession.get("userClass").equals("admin")) {
            generateXML_managGlobal(root);
            getLayoutService().doLayout(request, response, jdom);
        } else if (todo.equals("saveGlobal") && mcrSession.get("userClass").equals("admin"))
            generateXML_saveGlobal(request, response);
        else if (todo.equals("view") && (request.getParameter("file") != null && !request.getParameter("file").equals(""))) {
            // live content version requested
            if (request.getParameter("file").toString().subSequence(0, 4).equals("http")) {
                String url = request.getParameter("file");
                url = url + "?XSL.href=" + request.getParameter("XSL.href");
                // archived navi version requested
                if (request.getParameter("XSL.navi") != null && !request.getParameter("XSL.navi").equals("")
                                && !request.getParameter("XSL.navi").toString().subSequence(0, 4).equals("http")) {
                    url = url + "&XSL.navi=" + request.getParameter("XSL.navi");
                    response.sendRedirect(response.encodeRedirectURL(url));
                } else
                    response.sendRedirect(response.encodeRedirectURL(url));
            }
            // archived content version requested
            else
                getLayoutService().doLayout(request, response, new File(request.getParameter("file")));
        }
        // manage read access
        else if (todo.equals(MCRWCMSUtilities.getPermRightsManagementReadAccess()) && MCRWCMSUtilities.manageReadAccess()) {
            Element answer = new Element("cms");
            answer.addContent(new Element("rightsManagement").setAttribute("mode", MCRWCMSUtilities.getPermRightsManagementReadAccess()));
            getLayoutService().doLayout(request, response, new Document(answer));
        }
        // manage wcms access
        else if (todo.equals(MCRWCMSUtilities.getPermRightsManagementWCMSAccess()) && MCRWCMSUtilities.manageWCMSAccess()) {
            Element answer = new Element("cms");
            answer.addContent(new Element("rightsManagement").setAttribute("mode", MCRWCMSUtilities.getPermRightsManagementWCMSAccess()));
            getLayoutService().doLayout(request, response, new Document(answer));
        } else
            getLayoutService().doLayout(request, response, jdom);
    }

    private void exitWCMS(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getParameter("address") != null && !request.getParameter("address").equals("")) {
            String exitURL = request.getParameter("address");
            response.sendRedirect(response.encodeRedirectURL(exitURL));
        } else {
            String exitURL = request.getContextPath() + "/servlets/MCRWCMSAdminServlet?action=choose";
            response.sendRedirect(response.encodeRedirectURL(exitURL));
        }
    }

    /**
     * Returns the task for the servlet call
     * 
     * @param request
     * @return
     */
    final String getTodo(HttpServletRequest request) {
        if (request.getParameter("action") != null && !request.getParameter("action").equals("")) {
            return request.getParameter("action");
        }
        if (request.getParameter("todo") != null && !request.getParameter("todo").equals("")) {
            return request.getParameter("todo");
        }
        LOGGER.error("action AND todo == null. Call without a given request parameter.");
        return null;
    }

    public void generateXML_managPage(MCRSession mcrSession, Element root) {
        List rootNodes = (List) mcrSession.get("rootNodes");
        File[] contentTemplates = new File((CONFIG.getString("MCR.templatePath") + "content/").replace('/', File.separatorChar)).listFiles();
        root.addContent(new Element("userRealName").setText(mcrSession.get("userRealName").toString()));
        root.addContent(new Element("userClass").setText(mcrSession.get("userClass").toString()));
        root.addContent(new Element("error").setText(""));

        Iterator rootNodesIterator = rootNodes.iterator();

        while (rootNodesIterator.hasNext()) {
            Element rootNode = (Element) rootNodesIterator.next();
            root.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
        }

        Element templates = new Element("templates");
        Element contentTemp = new Element("content");

        for (int i = 0; i < contentTemplates.length; i++) {
            if (!contentTemplates[i].isDirectory()) {
                contentTemp.addContent(new Element("template").setText(contentTemplates[i].getName()));
            }
        }

        templates.addContent(contentTemp);
        root.addContent(templates);
    }

    public void generateXML_logs(HttpServletRequest request, Element rootOut) {
        String sort = request.getParameter("sort");
        String sortOrder = request.getParameter("sortOrder");
        String error;

        try {
            File logFile = new File(CONFIG.getString("MCR.WCMS.logFile").replace('/', File.separatorChar));

            if (!logFile.exists()) {
                error = "Logfile nicht gefunden!";
            }

            Element root = new SAXBuilder().build(logFile).getRootElement();
            Element test = (Element) root.clone();
            rootOut.addContent(test);
        } catch (Exception e) {
            error = e.getMessage();

            System.out.println(error);
        }

        rootOut.addContent(new Element("sort").setAttribute("order", sortOrder).setText(sort));
    }

    public void generateXML_managGlobal(Element rootOut) {
        // generate template list
        rootOut.addContent(getTemplates());
    }

    public void generateXML_saveGlobal(HttpServletRequest request, HttpServletResponse response) {
        try {
            String pathToNavi = new String(CONFIG.getString("MCR.navigationFile").replace('/', File.separatorChar));
            Document naviBase = new Document();
            naviBase = XMLFile2JDOM(pathToNavi);

            Element NaviBaseRoot = naviBase.getRootElement();

            // save default template if changed
            // get default template from navigatioBase
            String defaultTemplateNaviBase = XPath.newInstance("/navigation/@template").valueOf(naviBase);

            // get set def. templ. by aif
            String defaultTemplateAIF = new String();

            if ((request.getParameter("defTempl") != null) && !(request.getParameter("defTempl").equals(""))) {
                defaultTemplateAIF = request.getParameter("defTempl");
            }

            if (!(defaultTemplateNaviBase.equals(defaultTemplateAIF))) {
                // save changed naviBase
                NaviBaseRoot.setAttribute("template", defaultTemplateAIF);

                File navigationBase = new File(CONFIG.getString("MCR.navigationFile").replace('/', File.separatorChar));
                XMLOutputter xmlOut = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                xmlOut.output(naviBase, new FileOutputStream(navigationBase));
            }

            // forward to strarting page
            String address = new String();
            StringBuffer buffer = request.getRequestURL();
            String queryString = request.getQueryString();

            if (queryString != null) {
                buffer.append("?").append(queryString);
            }

            address = buffer.toString();

            String contextPath = request.getContextPath() + "/";
            int pos = address.indexOf(contextPath, 9);
            address = address.substring(0, pos) + contextPath + "servlets/MCRWCMSLoginServlet";
            response.sendRedirect(response.encodeRedirectURL(response.encodeURL(address)));
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * public boolean exitWCMS(HttpServletRequest request) { if
     * (request.getParameter("back") != null &&
     * request.getParameter("back").equals("true")) return true; return false; }
     */

}
