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

package org.mycore.frontend.indexbrowser;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This class builds a google sitemap containing links to all documents. The
 * web.xml file should contain a mapping to /sitemap.xml See
 * http://www.google.com/webmasters/sitemaps/docs/en/protocol.html
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public final class MCRGoogleSitemapServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRGoogleSitemapServlet.class);

    private static final String[] types = MCRConfiguration.instance().getString("MCR.googlesitemap.types", "document").split(",");

    private static final String freq = MCRConfiguration.instance().getString("MCR.googlesitemap.freq", "monthly");

    private static final MCRXMLTableManager tm = MCRXMLTableManager.instance();

    /**
     * This method implement the doGetPost method of MCRServlet. It build a XML
     * file for the Google search engine.
     * 
     * @param job
     *            a MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // set baseurl
        final long start = System.currentTimeMillis();
        String addr = getBaseURL() + "receive/";
        // build ducment frame
        Namespace ns = Namespace.getNamespace("http://www.google.com/schemas/sitemap/0.84");
        Element urlset = new Element("urlset", ns);
        Document jdom = new Document(urlset);

        // build over all types
        MCRObjectID mid = new MCRObjectID();
        Document doc = null;
        XPath xpath = XPath.newInstance("/mycoreobject/service/servdates/servdate[@type='modifydate']");
        for (String type : types) {
            for (Object objID : tm.retrieveAllIDs(type)) {
                String mcrID = objID.toString();
                mid.setID(mcrID);
                doc = tm.readDocument(mid);
                Element servDate = (Element) xpath.selectSingleNode(doc);
                // build entry
                Element url = new Element("url", ns);
                url.addContent(new Element("loc", ns).addContent(addr + mcrID));
                url.addContent(new Element("changefreq", ns).addContent(freq));
                url.addContent(new Element("lastmod", ns).addContent(servDate.getText()));
                urlset.addContent(url);
            }
        }
        // redirect Layout Servlet
        LOGGER.debug("Google sitemap request took " + (System.currentTimeMillis() - start) + "ms.");
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdom);
    }
}
