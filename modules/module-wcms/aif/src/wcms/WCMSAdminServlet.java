 /**
 * WCMSAdminServlet.java
 *
 * @author: Michael Brendel, Andreas Trappe
 * @contact: michael.brendel@uni-jena.de, andreas.trappe@uni-jena.de
 * @version: 0.81
 * @last update: 10/12/2003
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
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.input.SAXBuilder;
import org.jdom.*;
import org.mycore.common.MCRConfiguration;

public class WCMSAdminServlet extends HttpServlet {
    MCRConfiguration mcrConf = MCRConfiguration.instance();

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        if ( request.getSession(false) != null ) {
            processRequest(request, response);
        }
        else {
            response.sendRedirect(mcrConf.getString("MCR.WCMS.sessionError"));
        }
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        /* Validate if user has been authentificated */
        if ( request.getSession(false) != null ) {
            processRequest(request, response);
        }
        else {
            response.sendRedirect(mcrConf.getString("MCR.WCMS.sessionError"));
        }

    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }

    /** Destroys the servlet.
     */
    public void destroy() {

    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        if (request.getSession().getAttribute("status").equals("loggedIn")){
            String action = request.getParameter("action");
            List rootNodes = (List)request.getSession().getAttribute("rootNodes");
            File [] contentTemplates = new File((mcrConf.getString("MCR.WCMS.templatePath")+"content/").replace('/', File.separatorChar)).listFiles();
            Element rootOut = new Element("cms");
            Document jdom = new Document(rootOut);
            rootOut.addContent(new Element("session").setText(action));
            rootOut.addContent(new Element("userID").setText(request.getSession().getAttribute("userID").toString()));
            rootOut.addContent(new Element("userClass").setText(request.getSession().getAttribute("userClass").toString()));
            if (action.equals("choose")){
                rootOut.addContent(new Element("userRealName").setText(request.getSession().getAttribute("userRealName").toString()));
                rootOut.addContent(new Element("userClass").setText(request.getSession().getAttribute("userClass").toString()));
                rootOut.addContent(new Element("error").setText(""));
                Iterator rootNodesIterator = rootNodes.iterator();
                while (rootNodesIterator.hasNext()) {
                    Element rootNode = (Element)rootNodesIterator.next();
                    rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
                }
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

            if (action.equals("logs")){
                String sort = request.getParameter("sort");
                String sortOrder = request.getParameter("sortOrder");
                char fs = File.separatorChar;
                String error;
                try {
                    File logFile = new File(mcrConf.getString("MCR.WCMS.logFile").replace('/', File.separatorChar));
                    if (!logFile.exists()) error = "Logfile nicht gefunden!";
                    Element root = new SAXBuilder().build(logFile).getRootElement();
                    Element test = (Element)root.clone();
                    rootOut.addContent(test);
                }

                catch (Exception e) {
                    error = e.getMessage();
                    System.out.println(error);
                }

                rootOut.addContent(new Element("sort").setAttribute("order", sortOrder).setText(sort));

            }



            /**
            * Transfer content of jdom object to MCRLayoutServlet.
            */
            request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);

            /**
            * Activate the following Line to see the XML output of the jdom object.
            */
            //request.setAttribute("XSL.Style", "xml");
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        }
        else response.sendRedirect(mcrConf.getString("MCR.WCMS.sessionError"));
    }
}
