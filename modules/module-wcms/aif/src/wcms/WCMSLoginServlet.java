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

package wcms;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServletJob;

import wcms.util.HashCipher;

/**
 * Loginprocess for Web-Content-Management-System (WCMS).
 */
public class WCMSLoginServlet extends WCMSServlet {
    private static Logger LOGGER = Logger.getLogger(WCMSLoginServlet.class);

    /**
     * Identify and assign the system dependent file seperator character.
     */
    char fs = File.separatorChar;

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.frontend.servlets.MCRServlet#doGetPost(org.mycore.frontend.servlets.MCRServletJob)
     */
    protected void doGetPost(MCRServletJob job) throws Exception {
        /*
         * This method is overwritten from WCMSServlet, because ther is no user
         * yet
         */
        processRequest(job.getRequest(), job.getResponse());
    }

    /**
     * Main program called by doGet and doPost.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String userID = null;
        String userPassword = null;
        String userRealName = null;
        String userClass = null;
        String modus = "false";
        String flag = "false";
        List rootNodes = (List) mcrSession.get("rootNodes");
        boolean loginOk = false;
        
        if(request.getParameter("flag")!=null && request.getParameter("flag").equals("true"))
          {
           flag="true";	
          }
        else
          {
           flag="false";	
          }	
            
        MCRConfiguration mcrConf = MCRConfiguration.instance();

        try {
            userID = request.getParameter("userID").trim();
            userPassword = request.getParameter("userPassword").trim();
            userPassword = HashCipher.crypt(userPassword);
           } catch (Exception e) {
            System.err.println(e.getMessage());
        }
                        
        // pssword and userid not set
        if ((userPassword == null) && (userID == null)) {
        	if ((mcrSession.get("status") == null) )
        	  {
               if (flag.equals("true"))
          	     {
          		  response.sendRedirect(response.encodeRedirectURL(mcrConf.getString("MCR.WCMS.sessionError")+"?XSL.Style=xml"));	
          	     }
               else
                 {
                  response.sendRedirect(response.encodeRedirectURL(mcrConf.getString("MCR.WCMS.sessionError")));
                 }
              }
        	else 
        	  {
        	   if (mcrSession.get("status").equals("loggedIn")) 
        	     {
                  userID = (String) mcrSession.get("userID");
                  userRealName = (String) mcrSession.get("userRealName");
                  userClass = (String) mcrSession.get("userClass");
                  loginOk = true;
                 }	
        	}
       	} 
        
        
        /**
         * Look into the WCMSUserDB.xml file and check for the given User. If
         * the check was successful, loginOk is set to true.
         */
        try {
            File dbfile = new File(mcrConf.getString("MCR.WCMS.wcmsUserDBFile").replace('/', File.separatorChar));
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(dbfile);
            Element root = doc.getRootElement();
            List users = root.getChildren();
            Iterator userIterator = users.iterator();

            while (userIterator.hasNext() && (loginOk == false)) {
                Element iter = (Element) userIterator.next();

                if (iter.getAttribute("userID").getValue().equals(userID) && iter.getAttribute("userPassword").getValue().equals(userPassword)) {
                    userRealName = iter.getAttributeValue("userRealName");
                    userClass = iter.getAttributeValue("userClass");
                    rootNodes = iter.getChildren();
                    loginOk = true;
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        // System.out.println("loginValue = "+loginOk);

        /**
         * Build jdom object dependence on the given login data.
         */
        Element rootOut = new Element("cms");
        Document jdom = new Document(rootOut);
        
        /**
         * loginOk: <cms><session>choose </session> <userID>userID <userID>
         * <userRealName>userRealName </userRealName> <userClass>userClass
         * </userClass> <error></error> <rootNode href="no"|"yes">rootNode
         * </rootNode>{0,*} </cms>
         * 
         * and add some attributes to the session object: <session ... uid=uid
         * userClass="admin"|"editor"|"autor" rootNodes=rootNodes />
         */
        if (loginOk) {
            // System.out.println(session+" - "+userID+" - "+userRealName+" -
            // "+userClass);
            mcrSession.put("status", "loggedIn");

            if (!userID.equals(mcrSession.get("userID"))) {
                mcrSession.put("userID", userID);
            }

            if (!userRealName.equals(mcrSession.get("userRealName"))) {
                mcrSession.put("userRealName", userRealName);
            }

            if (!userClass.equals(mcrSession.get("userClass"))) {
                mcrSession.put("userClass", userClass);
            }

            if (!rootNodes.equals(mcrSession.get("rootNodes"))) {
                mcrSession.put("rootNodes", rootNodes);
            }

            rootOut.addContent(new Element("session").setText("welcome"));
            rootOut.addContent(new Element("userID").setText(userID));
            rootOut.addContent(new Element("userRealName").setText(userRealName));
            rootOut.addContent(new Element("userClass").setText(userClass));
            rootOut.addContent(new Element("error").setText(""));
            modus="true"; 


            Iterator rootNodesIterator = rootNodes.iterator();

            while (rootNodesIterator.hasNext()) {
                Element rootNode = (Element) rootNodesIterator.next();
                rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
            }
        }       
        /**
         * !loginOk: <cms><session>login </session> <error>denied </error>
         * </cms>
         */
        else {
            rootOut.addContent(new Element("session").setText("login"));
            rootOut.addContent(new Element("error").setText("denied"));
        }
        rootOut.addContent(new Element("modus").setText(modus));
        /**
         * Transfer content of jdom object to MCRLayoutServlet.
         */
        request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);

        /**
         * Activate the following Line to see the XML output of the jdom object.
         */
        if (mcrSession.get("status")!=null && mcrSession.get("status").equals("loggedIn") )
          {	
           // req.setAttribute("XSL.Style", "xml");
           RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
           rd.forward(request, response);
          }
        flag = "false";
    }
}
