/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.basket;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Provides the web front end to manage baskets and their contents.
 * Required parameter is the type of basket and the action to perform.
 * For a basket of objects, possible requests would be:
 * 
 * BasketServlet?type=objects&amp;action=show
 *   to output the contents of the objects basket using basket-{type}.xsl
 * BasketServlet?type=objects&amp;action=clear
 *   to remove all entries in the objects basket.  
 * BasketServlet?type=objects&amp;action=add&amp;id=DocPortal_document_00774301&amp;uri=mcrobject:DocPortal_document_00774301
 *   to add a new entry with ID DocPortal_document_00774301 to the basket, reading its contents from URI mcrobject:DocPortal_document_00774301
 * BasketServlet?type=objects&amp;action=add&amp;id=DocPortal_document_00774301&amp;uri=mcrobject:DocPortal_document_00774301&amp;resolve=true
 *   to add a new entry with ID DocPortal_document_00774301 to the basket, immediately resolving content from the given URI.
 * BasketServlet?type=objects&amp;action=remove&amp;id=DocPortal_document_00774301
 *   to remove the entry with ID DocPortal_document_00774301 from the basket  
 * BasketServlet?type=objects&amp;action=up&amp;id=DocPortal_document_00774301
 *   to move the entry with ID DocPortal_document_00774301 one position up in the basket  
 * BasketServlet?type=objects&amp;action=down&amp;id=DocPortal_document_00774301
 *   to move the entry with ID DocPortal_document_00774301 one position down in the basket  
 * BasketServlet?type=objects&amp;action=comment&amp;id=DocPortal_document_00774301
 *   to change the comment stored in the basket. This is called
 *   using EditorServlet submission, see basket-edit.xml for an example.
 * BasketServlet?type=objects&amp;action=create&amp;ownerID=DocPortal_basket_01234567
 *    to store a basket in a new file "basket" in a new derivate that is owned by the metadata object with the given ownerID.
 * BasketServlet?type=objects&amp;action=update
 *   to update a persistent basket in its derivate - that is the derivate the basket was loaded from.
 * BasketServlet?action=retrieve&amp;derivateID=DocPortal_derivate_12345678
 *   to retrieve a basket's data from a file "basket.xml" in the given derivate into the current user's session.
 * 
 * @author Frank L\u00fctzenkirchen
 **/
public class MCRBasketServlet extends MCRServlet {
    private static final Logger LOGGER = LogManager.getLogger(MCRBasketServlet.class);

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String type = req.getParameter("type");
        String action = req.getParameter("action");
        String[] uris = req.getParameterValues("uri");
        String[] ids = req.getParameterValues("id");
        String redirect = getProperty(req, "redirect");
        URL referer = getReferer(req);
        boolean resolveContent = "true".equals(req.getParameter("resolve"));

        LOGGER.info("{} {} {}", action, type, ids == null ? "" : ids);

        MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(type);

        if ("add".equals(action)) {
            if (uris.length != ids.length) {
                throw new MCRException("Amount of URIs must match amount of IDs");
            }
            for (int i = 0; i < uris.length; i++) {
                MCRBasketEntry entry = new MCRBasketEntry(ids[i], uris[i]);
                basket.add(entry);
                if (resolveContent) {
                    entry.resolveContent();
                }
            }
        } else if ("remove".equals(action)) {
            for (String id : ids) {
                basket.removeEntry(id);
            }
        } else if ("up".equals(action)) {
            for (String id : ids) {
                basket.up(basket.get(id));
            }
        } else if ("down".equals(action)) {
            for (String id : ids) {
                basket.down(basket.get(id));
            }
        } else if ("clear".equals(action)) {
            basket.clear();
        } else if ("create".equals(action)) {
            String ownerID = req.getParameter("ownerID");
            MCRObjectID ownerOID = MCRObjectID.getInstance(ownerID);
            MCRBasketPersistence.createDerivateWithBasket(basket, ownerOID);
        } else if ("update".equals(action)) {
            MCRBasketPersistence.updateBasket(basket);
        } else if ("retrieve".equals(action)) {
            String derivateID = req.getParameter("derivateID");
            basket = MCRBasketPersistence.retrieveBasket(derivateID);
            type = basket.getType();
            MCRBasketManager.setBasketInSession(basket);
        } else if ("comment".equals(action)) {
            Document sub = (Document) (job.getRequest().getAttribute("MCRXEditorSubmission"));
            String comment = sub.getRootElement().getChildTextTrim("comment");
            for (String id : ids) {
                basket.get(id).setComment(comment);
            }
        } else if ("show".equals(action)) {
            req.setAttribute("XSL.Style", type);
            Document xml = new MCRBasketXMLBuilder(true).buildXML(basket);
            getLayoutService().doLayout(req, res, new MCRJDOMContent(xml));
            return;
        }
        if (referer != null && "referer".equals(redirect)) {
            res.sendRedirect(res.encodeRedirectURL(referer.toExternalForm()));
        } else if (redirect != null) {
            res.sendRedirect(res.encodeRedirectURL(redirect));
        } else {
            res.sendRedirect(res.encodeRedirectURL(getServletBaseURL() + "MCRBasketServlet?action=show&type=" + type));
        }
    }
}
