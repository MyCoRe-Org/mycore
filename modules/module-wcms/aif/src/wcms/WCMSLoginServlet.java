/**
 * WCMSLoginServlet.java
 *
 * @author: Michael Brendel, Andreas Trappe
 * @contact: michael.brendel@uni-jena.de, andreas.trappe@uni-jena.de
 * @version: 0.81
 * @last update: 11/25/2003
 *
 * Copyright (C) 2003 University of Jena, Germany
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
 * along with this program, normally in the file sources/gpl.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package wcms;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import wcms.util.HashCipher;
//import wcms.util.WCMSProperties;
import org.mycore.common.*;

/**
 * Loginprocess for Web-Content-Management-System (WCMS).
 */
public class WCMSLoginServlet extends HttpServlet {

    /**
     * Identify and assign the system dependent file seperator character.
     */
    char fs = File.separatorChar;

    /**
     * Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Destroys the servlet.
     */
    public void destroy() {
    }

    /**
     * Handles the HTTP POST Method.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

            doGetPost(request, response, request.getSession(true));
    }

    /**
     * Handles the HTTP GET Method.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

            doGetPost(request, response, request.getSession(true));
    }

    /**
     * Main program called by doGet and doPost.
     */
    private void doGetPost(HttpServletRequest request, HttpServletResponse response, HttpSession session)
        throws ServletException, IOException {

        String userID = null;
        String userPassword = null;
        String userRealName = null;
        String userClass = null;
        List rootNodes = (List)session.getAttribute("rootNodes");
        boolean loginOk = false;
        MCRConfiguration mcrConf = MCRConfiguration.instance();

        try {
            userID = request.getParameter("userID").trim();
            userPassword = request.getParameter("userPassword").trim();
            userPassword = HashCipher.crypt(userPassword);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }

        if (session.getAttribute("status") != null && userPassword == null && userID == null) {
            if (session.getAttribute("status").equals("loggedIn")) {
                userID = (String)session.getAttribute("userID");
                userRealName = (String)session.getAttribute("userRealName");
                userClass = (String)session.getAttribute("userClass");
                loginOk = true;
            }
        }
        if (session.getAttribute("status") == null && userPassword == null && userID == null) {
            response.sendRedirect(mcrConf.getString("sessionError"));
        }

        /**
         * Look into the WCMSUserDB.xml file and check for the given User.
         * If the check was successful, loginOk is set to true.
         */
        try {
            File dbfile = new File(mcrConf.getString("wcmsUserDBFile").replace('/', File.separatorChar));
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(dbfile);
            Element root = doc.getRootElement();
            List users = root.getChildren();
            Iterator userIterator = users.iterator();

            while (userIterator.hasNext() && loginOk == false) {
                Element iter = (Element)userIterator.next();
                if (iter.getAttribute("userID").getValue().equals(userID) && iter.getAttribute("userPassword").getValue().equals(userPassword)) {
                    userRealName = iter.getAttributeValue("userRealName");
                    userClass = iter.getAttributeValue("userClass");
                    rootNodes = iter.getChildren();
                    loginOk = true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            PrintWriter errorOut = response.getWriter();
            errorOut.println(e.getMessage());
            errorOut.close();
        }

        System.out.println("loginValue = "+loginOk);

        /**
         * Build jdom object dependence on the given login data.
         */
        Element rootOut = new Element("cms");
        Document jdom = new Document(rootOut);

        /**
         * loginOk:
         * <cms>
         *     <session>choose</session>
         *     <userID>userID<userID>
         *     <userRealName>userRealName</userRealName>
         *     <userClass>userClass</userClass>
         *     <error></error>
         *     <rootNode href="no"|"yes">rootNode</rootNode>{0,*}
         * </cms>
         *
         * and add some attributes to the session object:
         * <session ... uid=uid userClass="admin"|"editor"|"autor" rootNodes=rootNodes />
         */

        if (loginOk) {
            System.out.println(session+" - "+userID+" - "+userRealName+" - "+userClass);
            session.setAttribute("status", "loggedIn");
            if (!userID.equals(session.getAttribute("userID"))) session.setAttribute("userID", userID);
            if (!userRealName.equals(session.getAttribute("userRealName"))) session.setAttribute("userRealName", userRealName);
            if (!userClass.equals(session.getAttribute("userClass"))) session.setAttribute("userClass", userClass);
            if (!rootNodes.equals(session.getAttribute("rootNodes"))) session.setAttribute("rootNodes", rootNodes);
            rootOut.addContent(new Element("session").setText("welcome"));
            rootOut.addContent(new Element("userID").setText(userID));
            rootOut.addContent(new Element("userRealName").setText(userRealName));
            rootOut.addContent(new Element("userClass").setText(userClass));
            rootOut.addContent(new Element("error").setText(""));
            Iterator rootNodesIterator = rootNodes.iterator();
            while (rootNodesIterator.hasNext()) {
                Element rootNode = (Element)rootNodesIterator.next();
                rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
            }
        }

        /**
         * !loginOk:
         * <cms>
         *     <session>login</session>
         *     <error>denied</error>
         * </cms>
         */
        else {
            rootOut.addContent(new Element("session").setText("login"));
            rootOut.addContent(new Element("error").setText("denied"));
        }

        /**
         * Transfer content of jdom object to MCRLayoutServlet.
         */
        request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);

        /**
         * Activate the following Line to see the XML output of the jdom object.
         */
        //req.setAttribute("XSL.Style", "xml");
        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(request, response);
        }
    }