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

package org.mycore.frontend.export;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.frontend.basket.MCRBasket;
import org.mycore.frontend.basket.MCRBasketManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * Provides functionality to export content. 
 * The content to export can be selected by specifying one or more 
 * URIs to read from, or by giving the ID of a basket to export. 
 * The selected content is collected as MCRExportCollection thats
 * root element name can be specified. 
 * The content is then transformed using an MCRContentTransformer instance
 * and forwarded to the requesting client.
 * 
 * Request Parameters:
 *   uri=... 
 *     can be repeated to include content from one or more URIs to read XML from
 *   basket=...
 *     the ID of a basket to read XML from  
 *   root=...
 *     optional, name of the root element that wraps the selected content
 *   ns=...
 *     optional, URI of the namespace of the root element
 *   transformer=...
 *     the ID of the transformer to use to export the selected content.
 *          
 * @see MCRExportCollection
 * @see MCRContentTransformer
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRExportServlet extends MCRServlet {

    private static final Logger LOGGER = LogManager.getLogger(MCRExportServlet.class);

    /** URIs beginning with these prefixes are forbidden for security reasons */
    private static final String[] forbiddenURIs = { "file", "webapp", "resource" };

    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        MCRExportCollection collection = createCollection(job.getRequest());
        fillCollection(job.getRequest(), collection);
        MCRContent content2export = collection.getContent();

        String filename = getProperty(job.getRequest(), "filename");
        if (filename == null) {
            filename = "export-" + System.currentTimeMillis();
        }
        job.getResponse().setHeader("Content-Disposition", "inline;filename=\"" + filename + "\"");

        String transformerID = job.getRequest().getParameter("transformer");
        job.getRequest().setAttribute("XSL.Transformer", transformerID);
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), content2export);
    }

    /**
     * Fills the collection with the XML data requested by URIs or basket ID.
     */
    private void fillCollection(HttpServletRequest req, MCRExportCollection collection) throws Exception {
        String basketID = req.getParameter("basket");
        if (basketID != null) {
            MCRBasket basket = MCRBasketManager.getOrCreateBasketInSession(basketID);
            collection.add(basket);
            LOGGER.info("exporting basket {} via {}", basketID, req.getParameter("transformer"));
        }

        if (req.getParameter("uri") != null)
            for (String uri : req.getParameterValues("uri")) {
                if (isAllowed(uri)) {
                    collection.add(uri);
                    LOGGER.info("exporting {} via {}", uri, req.getParameter("transformer"));
                }
            }
    }

    private boolean isAllowed(String uri) {
        for (String prefix : forbiddenURIs)
            if (uri.startsWith(prefix)) {
                LOGGER.warn("URI {} is not allowed for security reasons", uri);
                return false;
            }
        return true;
    }

    /**
     * Creates a new, empty MCRExportCollection, optionally with the requested root element name and namespace.
     */
    private MCRExportCollection createCollection(HttpServletRequest req) {
        MCRExportCollection collection = new MCRExportCollection();
        String root = req.getParameter("root");
        String ns = req.getParameter("ns");
        if (!((root == null) || root.isEmpty()))
            collection.setRootElement(root, ns);
        return collection;
    }
}
