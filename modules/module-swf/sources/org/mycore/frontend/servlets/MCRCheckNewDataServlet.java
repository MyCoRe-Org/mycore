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

package org.mycore.frontend.servlets;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.*;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.frontend.servlets.*;
import org.mycore.frontend.editor.*;
import org.mycore.frontend.workflow.*;
import org.mycore.user.*;

/**
 * The servlet store the MCREditorServlet output XML in a file of a MCR type 
 * dependencies directory, check it dependence of the MCR type and store 
 * the XML in a file in this directory or if an error was occured start the 
 * editor again with <b>todo</b> <em>repair</em>.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRCheckNewDataServlet extends MCRCheckDataBase
{
/**
 * The method check the privileg of this action.
 *
 * @param privs the ArrayList  of privilegs
 * @return true if the privileg exist, else return false
 **/
public final boolean hasPrivileg(ArrayList privs, String type)
  {
  if (!privs.contains("create-"+type)) return false;
  return true;
  }

/**
 * The method is a dummy and return an URL with the next working step.
 *
 * @param ID the MCRObjectID of the MCRObject
 * @return the next URL as String
 **/
public final String getNextURL(MCRObjectID ID) throws Exception
  {
  // return all is ready
  StringBuffer sb = new StringBuffer();
  sb.append(CONFIG.getString( "MCR.editor_page_dir","" )).append("editor_").append(ID.getTypeId()).append("_editor.xml");
  return sb.toString();
  }

/**
 * The method send a message to the mail address for the MCRObjectType.
 *
 * @param ID the MCRObjectID of the MCRObject
 **/
 public final void sendMail(MCRObjectID ID)
   {
   MCRWorkflowManager wfm = MCRWorkflowManager.instance();
   List addr = wfm.getMailAddress(ID.getTypeId(),"wnewobj");
   if (addr.size() == 0) return;
   String sender = wfm.getMailSender();
   String appl = CONFIG.getString("MCR.editor_mail_application_id","DocPortal");
   String subject = "Automaticaly message from "+appl;
   StringBuffer text = new StringBuffer();
   text.append("Es wurde ein neues Objekt vom Typ ").append(ID.getTypeId())
     .append(" mit der ID ").append(ID.getId())
     .append(" in den Workflow eingestellt.");
   logger.info(text.toString());
   try {
     MCRMailer.send(sender,addr,subject,text.toString(),false); }
   catch (Exception ex) {
     logger.error("Can't send a mail to "+addr); }
   }

}
