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
import org.apache.commons.fileupload.*;
import org.jdom.*;

import org.mycore.common.*;
import org.mycore.common.xml.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.frontend.servlets.*;
import org.mycore.frontend.editor2.*;
import org.mycore.user.*;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML and store the XML in a file or if an error was occured start the
 * editor again.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

public class MCRCheckCommitFileServlet extends MCRCheckFileBase
{

/**
 * The method check the privileg of this action.
 *
 * @param privs the ArrayList  of privilegs
 * @return true if the privileg exist, else return false
 **/
public final boolean hasPrivileg(ArrayList privs, String type)
  {
  if (!privs.contains("modify-"+type)) return false;
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
  return "";
  }

/**
 * The method is a dummy and return an URL with the next working step.
 *
 * @param ID the MCRObjectID of the MCRObject
 * @param DD the MCRObjectID of the MCRDerivate
 * @return the next URL as String
 **/
public final String getNextURL(MCRObjectID ID, MCRObjectID DD) throws Exception
  {
  // return all is ready
  StringBuffer sb = new StringBuffer();
  sb.append("servlets/MCRStartEditorServlet?todo=scommitder&type=")
    .append(ID.getTypeId()).append("&step=commit&se_mcrid=")
    .append(ID.getId()).append("&re_mcrid=").append(DD.getId())
    .append("&tf_mcrid=").append(ID.getId());
  return sb.toString();
  }

/**
 * The method send a message to the mail address for the MCRObjectType.
 *
 * @param ID the MCRObjectID of the MCRObject
 **/
 public final void sendMail(MCRObjectID ID)
   {
   }

}
