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

package org.mycore.frontend.wcms;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRWCMSLoginServlet extends MCRWCMSServlet {
	private static final long serialVersionUID = 1L;

	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// init 
		MCRSession session = MCRSessionMgr.getCurrentSession();
		String userID = session.getCurrentUserID();
        String userRealName = session.getCurrentUserID();
        String userClass = "admin";
        // fill session
        session.put("status", "loggedIn");
        session.put("userID", userID);
        session.put("userRealName", userRealName);
        session.put("userClass", userClass);
        session.put("rootNodes", new ArrayList());
        // build jdom
        Element root = new Element("cms");
        Document jdom = new Document(root);
        root.addContent(new Element("session").setText("welcome"));
        root.addContent(new Element("userID").setText(userID));
        root.addContent(new Element("userRealName").setText(userRealName));
        root.addContent(new Element("userClass").setText(userClass));
        root.addContent(new Element("error").setText(""));
        root.addContent(new Element("rootNode").setAttribute("href", "href value").setText("root node..."));
        root.addContent(new Element("modus").setText("true"));
        // do layout
        getLayoutService().doLayout(request, response, jdom);
	}    
}