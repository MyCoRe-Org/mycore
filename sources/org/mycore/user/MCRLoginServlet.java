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
import mycore.user.*;

/**
 * This servlet is used to login a user to the mycore system.
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */

public class MCRLoginServlet extends HttpServlet
{
  /**
   * This method overrides doGet of HttpServlet. It is the most important method
   * of this servlet.
   */
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
                    throws IOException, ServletException
  {
    try
    {
      String userID;
      HttpSession theSession;

      // get the current session; create a new one if it doesn't exist
      theSession = request.getSession(true);
      response.setContentType("text/html");   // MIME type to return is HTML
      PrintWriter out = response.getWriter(); // get a handle to the output stream

      // print out common html head data
      out.println("<HTML><HEAD>");
      out.println("<TITLE>" + "Login" + "</TITLE>");
      out.println("</HEAD>");
      out.println("<BODY bgcolor=\"#DCFFDC\">");
      out.println("<P><BR><CENTER><H2>MyCoRe Login</H2></CENTER><P>");

      // Test if a logoff request has been submitted. If yes, invalidate the session
      // and create a new one.
      String invalidate = request.getParameter("INVALIDATE");
      if (invalidate != null) {
        theSession.invalidate();
        theSession = request.getSession(true);
      }

      if (theSession.isNew()) {        // first time the client requests this page or
        printAuthenticationForm(out);  // a previous session has been invalidated
      }
      else  // client has already established a session
      {
        if (theSession.getAttribute("XSL.CurrentUser") == null)
        {
          // client has submitted the previous login data form, but there is not yet
          // an entry in the session data. We get the data and try to login to the system.

          userID = (String)request.getParameter("UserID").trim();  // get the userID from the request
          String passwd = request.getParameter("Password").trim(); // get the password from the request

          if (MCRUserMgr.instance().retrieveUser(userID) == null)  // no such user available
            printAuthenticationFailed(out, "Diese User ID ist nicht bekannt.");
          else {
            if (MCRUserMgr.instance().login(userID, passwd)) {
              theSession.setAttribute("XSL.CurrentUser", userID);  // store the user ID in the session
              printAuthenticationOK(out, userID);
              printTaskList(out, userID);  // show all the possible tasks for this user
            }
            else { // user exists but wrong passowrd
              printAuthenticationFailed(out, "Die User ID ist bekannt aber das Passwort ist falsch.");
            }
          }
        }
        else {
          // client has previously submitted the login data form and there is an
          // entry in the session data
          userID = (String)theSession.getAttribute("XSL.CurrentUser");
          printAuthenticationOK(out, userID);
          printTaskList(out, userID);  // show all the possible tasks for this user
        }
      }
      out.println("</BODY></HTML>");
      out.close();
    }

    catch (Exception e) {
      throw new ServletException("Exception occured: " +e);
    }
  } // end of doGet()

  /** Überschreiben der Methode doPost() */
  public void doPost(HttpServletRequest request,
                     HttpServletResponse response)
                     throws IOException, ServletException
  {
    doGet(request, response);
  }

  /** HTML-Eingabeformular für die Login-Daten */
  private void printAuthenticationForm(PrintWriter out)
  {
    // generate HTML form requesting login data
    out.println("<P><CENTER>Sie sind nicht angemeldet. Bitte geben Sie Ihre Login-Daten ein:</CENTER><P>");
    out.println("<P><HR><FORM METHOD=\"POST\">");
    out.println("<CENTER><TABLE BORDER=\"0\">");
    out.println("<TR>");
    out.println("<TD>User ID : </TD>");
    out.println("<TD><INPUT TYPE=\"TEXT\" SIZE=14 NAME=\"UserID\"></TD>");
    out.println("</TR><TR>");
    out.println("<TD>Passwort: </TD>");
    out.println("<TD><INPUT TYPE=\"PASSWORD\" SIZE=14 NAME=\"Password\"></TD>");
    out.println("</TR>");
    out.println("</TABLE></CENTER><P>");
    out.println("<CENTER><INPUT TYPE=\"SUBMIT\" NAME=\"USER\" VALUE=\"bitte anmelden\"></CENTER>");
    out.println("</FORM><P><HR><P>");
  }

  /** HTML-Ausgabe für eine gelungene Anmeldung */
  private void printAuthenticationOK(PrintWriter out, String userID)
  {
    out.println("<P><CENTER>Sie sind angemeldet als: <FONT COLOR=\"#FF0000\">"+userID+"</FONT></CENTER><P>");
    out.println("<P><CENTER>Um eine andere User ID verwenden zu können, müssen");
    out.println("Sie sich zunächst abmelden.</CENTER><P>");
    out.println("<P><FORM METHOD=\"POST\">");
    out.println("<INPUT TYPE=\"HIDDEN\" NAME=\"INVALIDATE\" VALUE=\"TRUE\">");
    out.println("<CENTER><INPUT TYPE=\"SUBMIT\" VALUE=\"abmelden\"></CENTER>");
    out.println("</FORM><HR><P>");
  }

  /** HTML-Ausgabe für eine misslungene Anmeldung */
  private void printAuthenticationFailed(PrintWriter out, String whatIsWrong)
  {
    out.println("<P><CENTER>Ihre Anmeldedaten sind nicht korrekt.</CENTER><BR>");
    out.println("<CENTER><U>Fehler:</U> "+whatIsWrong+"</CENTER><BR>");
    out.println("<CENTER>Bitte versuchen Sie es noch einmal!</CENTER><P>");
    out.println("<P><FORM METHOD=\"POST\">");
    out.println("<INPUT TYPE=\"HIDDEN\" NAME=\"INVALIDATE\" VALUE=\"TRUE\">");
    out.println("<CENTER><INPUT TYPE=\"SUBMIT\" VALUE=\"wiederholen\"></CENTER>");
    out.println("</FORM><HR><P>");
  }

  /** HTML-Ausgabe für Aufgaben, die von der angemeldeten UserID ausgeführt werden können */
  private void printTaskList(PrintWriter out, String userID) throws ServletException
  {
    try {
      MCRUser theUser = MCRUserMgr.instance().retrieveUser(userID);
      out.println("<P>Sie haben die Privilegien, die folgenden Tätigkeiten durchzuführen:<P>");
      out.println("<UL>");
      if (theUser.isUpdateAllowed()) {
        out.println("<LI><A HREF=\"ChangePassword\">Passwortänderung (aktueller user)</A>");
        out.println("<LI><A HREF=\"EditUser?userID="+userID+"\">Datenänderung (aktueller user)</A>");
      }
      if (theUser.hasPrivilege("create user"))
        out.println("<LI><A HREF=\"CreateUser\">Einrichten einer neuen User ID</A>");
      if (theUser.hasPrivilege("modify user"))
        out.println("<LI><A HREF=\"EditUser\">Ändern der Daten anderer User Accounts</A>");
      if (theUser.hasPrivilege("list all users"))
        out.println("<LI><A HREF=\"ListAllUsers\">Auflisten aller Benutzer/-innen</A>");
      if (theUser.hasPrivilege("list all privileges"))
        out.println("<LI><A HREF=\"ListPrivilegeSet\">Auflisten der vorhandenen Privilegien im System</A>");
      out.println("</UL><P><HR>");
    }

    catch (Exception e) {
      throw new ServletException("Exception occured: " +e);
    }
  }
}
