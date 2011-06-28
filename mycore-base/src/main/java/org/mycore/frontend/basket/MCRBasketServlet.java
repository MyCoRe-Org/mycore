/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.basket;

import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.frontend.basket.MCRBasket;
import org.mycore.frontend.basket.MCRBasketManager;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Provides the web front end to manage baskets and their contents.
 * Required parameter is the type of basket and the action to perform.
 * For a basket of objects, possible requests would be:
 * 
 * BasketServlet?type=objects&action=show
 *   to output the contents of the objects basket using basket-{type}.xsl
 * BasketServlet?type=objects&action=clear
 *   to remove all entries in the objects basket.  
 * BasketServlet?type=objects&action=add&id=DocPortal_document_00774301&uri=mcrobject:DocPortal_document_00774301
 *   to add a new entry with ID DocPortal_document_00774301 to the basket, reading its contents from URI mcrobject:DocPortal_document_00774301
 * BasketServlet?type=objects&action=add&id=DocPortal_document_00774301&uri=mcrobject:DocPortal_document_00774301&resolve=true
 *   to add a new entry with ID DocPortal_document_00774301 to the basket, immediately resolving content from the given URI.
 * BasketServlet?type=objects&action=remove&id=DocPortal_document_00774301
 *   to remove the entry with ID DocPortal_document_00774301 from the basket  
 * BasketServlet?type=objects&action=up&id=DocPortal_document_00774301
 *   to move the entry with ID DocPortal_document_00774301 one position up in the basket  
 * BasketServlet?type=objects&action=down&id=DocPortal_document_00774301
 *   to move the entry with ID DocPortal_document_00774301 one position down in the basket  
 * BasketServlet?type=objects&action=comment&id=DocPortal_document_00774301
 *   to change the comment stored in the basket. This is called
 *   using EditorServlet submission, see basket-edit.xml for an example. 
 * 
 * @author Frank L\u00fctzenkirchen
 **/
public class MCRBasketServlet extends MCRServlet {
    private final static Logger LOGGER = Logger.getLogger(MCRBasketServlet.class);

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String type = req.getParameter("type");
        String action = req.getParameter("action");
        String id = req.getParameter("id");
        String uri = req.getParameter("uri");
        boolean resolveContent = "true".equals(req.getParameter("resolve"));

        LOGGER.info(type + " " + action + " " + (id == null ? "" : id));

        MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(type);

        if ("add".equals(action)) {
            MCRBasketEntry entry = new MCRBasketEntry(id, uri);
            basket.add(entry);
            if (resolveContent)
                entry.resolveContent();
        } else if ("remove".equals(action))
            basket.removeEntry(id);
        else if ("up".equals(action))
            basket.up(basket.get(id));
        else if ("down".equals(action))
            basket.down(basket.get(id));
        else if ("clear".equals(action))
            basket.clear();
        else if ("comment".equals(action)) {
            MCREditorSubmission sub = (MCREditorSubmission) (req.getAttribute("MCREditorSubmission"));
            String comment = sub.getXML().getRootElement().getChildTextTrim("comment");
            basket.get(id).setComment(comment);
        } else if ("show".equals(action)) {
            req.setAttribute("XSL.Style", type);
            Document xml = new MCRBasketXMLBuilder(true).buildXML(basket);
            getLayoutService().doLayout(req, res, xml);
            return;
        }

        res.sendRedirect(getServletBaseURL() + "MCRBasketServlet?action=show&type=" + type);
    }
}
