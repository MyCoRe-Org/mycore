/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.user;

import java.util.ArrayList;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet returns a XML Object that contains the name of a user and his
 * coresponding user privileges.
 *
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRUserPrivServlet extends MCRServlet
  {
  private static Logger LOGGER=Logger.getLogger(MCRUserPrivServlet.class);


  /**
   * This method overrides doGetPost of MCRServlet.<br />
   * The method looks for one parameters - privilege (optional).
   * If we get a priviliege the method search for the privilege of the user
   * of the currenst session context. If it is true this privilege was set in
   * the answer XML-stream. If this parameter is not you get all privileges of
   * the user as XML-stream.<br />
   * The syntax of the stream is<br /><br />
   * &lt;mycoreuserpriv<br />
   * ...&gt;<br />
   * &lt;user ID="..."&gt;<br />
   * &lt;privilege&gt;<br />
   * ...<br />
   * &lt;/privilege&gt;<br />
   * &lt;/user&gt;<br />
   * &lt;/mycoreuserpriv&gt;<br />
   */
  public void doGetPost(MCRServletJob job) throws Exception
    {
    // read the privilege parameter
    String searchpriv = getProperty(job.getRequest(), "privilege");
    if (searchpriv == null) { searchpriv = ""; }
    searchpriv.trim();
    LOGGER.info("Search privilege = "+searchpriv);

    // get the MCRSession object for the current thread from the session manager.
    MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    String userid = mcrSession.getCurrentUserID();
    LOGGER.info("Curren user      = "+userid);

    // prepare the document
    org.jdom.Element root = new org.jdom.Element("mycoreuserpriv");
    org.jdom.Document jdom = new org.jdom.Document(root);
    if (userid.length()!=0) {
      org.jdom.Element uelm = new org.jdom.Element("user").setAttribute("ID",
        userid);
      // if we have no given privileg
      if (searchpriv.length() == 0) {
        ArrayList privs = MCRUserMgr.instance().retrieveAllPrivsOfTheUser(userid);
        for (int i=0;i<privs.size();i++) {
          uelm.addContent(new org.jdom.Element("privilege").setText(
            (String)privs.get(i)));
          }
        }
      // else has the user this privilege
      else {
        if (MCRUserMgr.instance().hasPrivilege(userid,searchpriv)) {
          uelm.addContent(new org.jdom.Element("privilege").setText(searchpriv));
          }
        }
      root.addContent(uelm);
      }

    job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
    job.getRequest().setAttribute("XSL.Style", "xml");

    RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
    rd.forward(job.getRequest(), job.getResponse());
    }
  }
