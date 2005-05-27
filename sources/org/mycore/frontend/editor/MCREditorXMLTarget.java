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

import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.Document;
import org.mycore.common.*;

/**
 * Saves an XML document that was edited with EditorServlet to an underlying
 * system.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public interface MCREditorXMLTarget {
    /**
     * Returns true, if the current servlet user is allowed to edit the object
     * with the given ID.
     */
    public boolean isEditingAllowed(HttpServletRequest request,
            ServletContext context, String objectID) throws Exception;

    /**
     * Saves the object with the given ID to the underlying system and returns a
     * URL to redirect the browser to after saving. If no URL is returned,
     * EditorServlet will display the XML object by forwarding it to
     * LayoutServlet.
     * 
     * @return the URL to redirect the browser to after saving
     */
    public String saveDocument(Document object, String objectID,
            ServletContext context) throws Exception;
}

