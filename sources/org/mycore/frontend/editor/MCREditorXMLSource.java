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

package mycore.editor;

import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.Document;
import mycore.common.*;

/**
 * Loads an XML document from a source to edit it with MCREditorServlet.
 *
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 **/
public interface MCREditorXMLSource
{
  /** 
   * Returns true, if the current servlet user is allowed to edit
   * the object with the given ID.
   **/
  public boolean isEditingAllowed( HttpServletRequest request,
                                   ServletContext     context,
                                   String             objectID )
    throws Exception;

  /**
   * Loads the object with the given ID from the underlying system
   * and returns it as a JDOM XML document.
   *
   * @return the input XML document for editing, or null if no such object exists.
   **/
  public Document loadDocument( String objectID, ServletContext context )
    throws Exception;
}

