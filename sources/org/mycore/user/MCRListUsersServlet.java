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

package mycore.user;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import mycore.common.*;
import org.w3c.dom.*;

/**
 * This servlet lists all users of the system. It uses the user manager (MCRUserMgr)
 * to get a DOM document of all users, checks whether the current user has the
 * privilege to list the users and then pass the document to MCRLayoutServlet.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */

public class MCRListUsersServlet extends HttpServlet
{
  /**
   * This method handles HTTP GET requests.
   *
   * @param request the HTTP request instance
   * @param response the HTTP response instance
   * @exception IOException for java I/O errors.
   * @exception ServletException for servlet engine errors.
   */
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
                    throws IOException, ServletException
  {
    try
    {
      HttpSession theSession = request.getSession(true); // get the current session
      String currentUserID   = (String)theSession.getAttribute("XSL.CurrentUser");
      String styleSheet      = "listAllUsers";

      if (currentUserID == null) // we do not have a logged on user
      {
        // use special stylesheet here
        // direct the client to the logon servlet
      }
      else  // ok, we have a logged on user
      {
        // At first we test whether the currently logged on user *really* has the
        // privilege to use this servlet.

        MCRUser currentUser = MCRUserMgr.instance().retrieveUser(currentUserID);
        if (!currentUser.hasPrivilege("list all users")) {
          showNoPrivilegePage(response);
          throw new MCRException("Not enough privileges");
        }

        // Now we get the DOM representation of all users and pass it to
        // the layout servlet.

        Document doc = MCRUserMgr.instance().getAllUsersAsDOM();
        request.setAttribute("MCRLayoutServlet.Input.DOM", doc);
        request.setAttribute("XSL.Style", styleSheet);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward( request, response );
      }
    }

    catch (MCRException e){}
    catch (Exception e) {
      throw new ServletException("Exception occured: " +e);
    }
  }

  /** This method handles HTTP POST requests and simply forwards them to doGet(). */
  public void doPost(HttpServletRequest request,
                     HttpServletResponse response)
                     throws IOException, ServletException
  { doGet(request, response); }

  /**
   * This helper method simply prints out the message that the currently logged
   * on user does not have the necessary privileges to use this servlet.
   *
   * @param response    corresponding HttpServeltResponse object
   */
  private void showNoPrivilegePage(HttpServletResponse response) throws IOException
  {
    response.setContentType("text/html");   // MIME type to return is HTML
    PrintWriter out = response.getWriter(); // get a handle to the output stream

    out.println("<HTML><BODY BACKGROUND=\"images/background_1.jpg\"><CENTER><P/>");
    out.println("<H3>Sie haben nicht genügend Privilegien, um dieses Servlet zu benutzen.</H3>");
    out.println("<P/>Bitte melden Sie sich mit einer privilegierten User ID an.<P/>");
    out.println("<A HREF=\"/mycore/Login\">Anmeldung</A>");
    out.println("</CENTER></BODY></HTML>");
    out.close();
  }
}
