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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
 *   to add a new entry with ID DocPortal_document_00774301 to the basket,
 *   reading its contents from URI mcrobject:DocPortal_document_00774301
 * BasketServlet?type=objects&amp;action=add&amp;id=DocPortal_document_00774301&amp;uri=mcrobject:DocPortal_document_00774301&amp;resolve=true
 *   to add a new entry with ID DocPortal_document_00774301 to the basket,
 *   immediately resolving content from the given URI.
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
 *    to store a basket in a new file "basket" in a new derivate that is owned by the metadata object
 *    with the given ownerID.
 * BasketServlet?type=objects&amp;action=update
 *   to update a persistent basket in its derivate - that is the derivate the basket was loaded from.
 * BasketServlet?action=retrieve&amp;derivateID=DocPortal_derivate_12345678
 *   to retrieve a basket's data from a file "basket.xml" in the given derivate into the current user's session.
 * 
 * @author Frank L\u00fctzenkirchen
 **/
public class MCRBasketServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRBasketServlet.class);

    public static final String ALLOW_LIST_PROPERTY_NAME = "MCR.Basket.Resolver.AllowList";

    private static final List<String> URI_ALLOW_LIST = MCRConfiguration2
        .getOrThrow(ALLOW_LIST_PROPERTY_NAME, MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest req = job.getRequest();
        String type = req.getParameter("type");
        String action = req.getParameter("action");
        String[] uris = req.getParameterValues("uri");
        String[] ids = req.getParameterValues("id");
        LOGGER.info("{} {} {}", action, type, ids == null ? "" : ids);
        MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(type);
        switch (action) {
            case "add":
                if (uris.length != ids.length) {
                    throw new MCRException("Amount of URIs must match amount of IDs");
                }
                for (int i = 0; i < uris.length; i++) {
                    if (URI_ALLOW_LIST.stream().noneMatch(uris[i]::startsWith)) {
                        throw new MCRException("The URI \"" + uris[i] + "\" is forbidden ");
                    }
                    MCRBasketEntry entry = new MCRBasketEntry(ids[i], uris[i]);
                    basket.add(entry);
                    if ("true".equals(req.getParameter("resolve"))) {
                        entry.resolveContent();
                    }
                }
                break;
            case "remove":
                for (String id : ids) {
                    basket.removeEntry(id);
                }
                break;
            case "up":
                for (String id : ids) {
                    basket.up(basket.get(id));
                }
                break;
            case "down":
                for (String id : ids) {
                    basket.down(basket.get(id));
                }
                break;
            case "clear":
                basket.clear();
                break;
            case "create":
                MCRBasketPersistence.createDerivateWithBasket(basket,
                    MCRObjectID.getInstance(req.getParameter("ownerID")));
                break;
            case "update":
                MCRBasketPersistence.updateBasket(basket);
                break;
            case "retrieve":
                basket = MCRBasketPersistence.retrieveBasket(req.getParameter("derivateID"));
                type = basket.getType();
                MCRBasketManager.setBasketInSession(basket);
                break;
            case "comment":
                String comment = ((Document) job.getRequest().getAttribute("MCRXEditorSubmission"))
                    .getRootElement().getChildTextTrim("comment");
                for (String id : ids) {
                    basket.get(id).setComment(comment);
                }
                break;
            case "show":
                req.setAttribute("XSL.Style", type);
                Document xml = new MCRBasketXMLBuilder(true).buildXML(basket);
                getLayoutService().doLayout(req, job.getResponse(), new MCRJDOMContent(xml));
                return;
            default:
                throw new MCRException("Invalid action: " + action);
        }
        extracted(req, job.getResponse(), type);
    }

    private void extracted(HttpServletRequest req, HttpServletResponse res, String type) throws IOException {
        String redirect = getProperty(req, "redirect");
        URI referer = getReferer(req);
        if (referer != null && Objects.equals(redirect, "referer")) {
            res.sendRedirect(res.encodeRedirectURL(referer.toString()));
        } else {
            res.sendRedirect(res.encodeRedirectURL(Objects.requireNonNullElseGet(redirect, () -> getServletBaseURL() +
                "MCRBasketServlet?action=show&type=" + type)));
        }
    }

}
