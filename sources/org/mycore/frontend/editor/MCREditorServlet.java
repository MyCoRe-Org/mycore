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

package org.mycore.frontend.editor;

import java.io.*;
import java.util.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.mycore.common.*;
import org.mycore.frontend.servlets.*;

/**
 * This servlet provides funtions to edit XML documents using
 * HTML forms.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public class MCREditorServlet extends MCRServlet
{
  protected void doGetPost( MCRServletJob job )
  {
    try
    {
      ServletContext context = getServletContext();
      MCREditorRequest er = new MCREditorRequest( job.getRequest(), job.getResponse(), context );  
      er.processRequest();
    }
    catch( Exception ex )
    { 
      try{ job.getResponse().sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ); }
      catch( Exception ignored ){}
      String msg = "Error while processing EditorServlet request";
      throw new MCRException( msg, ex ); 
    }
  }
}

