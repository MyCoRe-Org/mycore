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
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * This class implements all common methods th create the Google sitemap data.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * 
 */
public final class MCRGoogleSitemapCommon {

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRGoogleSitemapCommon.class.getName());

    /** The application basedir */
    private static final String basedir = MCRConfiguration.instance().getString("MCR.basedir", "");

    /** The base URL */
    private static final String baseurl  = MCRConfiguration.instance().getString("MCR.baseurl","");
    
    /** The webapps directory path from configuration */
    private static final String cdir = MCRConfiguration.instance().getString("MCR.GoogleSitemap.Directory", "");

    /** The types to build sitemaps */
    private static final String[] types = MCRConfiguration.instance().getString("MCR.GoogleSitemap.Types", "document").split(",");

    /** The frequence of crawle by Google */
    private static final String freq = MCRConfiguration.instance().getString("MCR.GoogleSitemap.Freq", "monthly");

    /** The style for by Google link */
    private static final String style = MCRConfiguration.instance().getString("MCR.GoogleSitemap.Style", "");

    /** The url path for retrieving object metadata */
    private static final String objectPath = MCRConfiguration.instance().getString("MCR.GoogleSitemap.ObjectPath", "receive/");
    
    /** The XML table API */
    private static final MCRXMLTableManager tm = MCRXMLTableManager.instance();

    /**
     * The method return the complete path to the sitemap.xml file.
     * 
     * @return a path to sitemap.xml
     */
    protected static final String getFileName() {
        String file = cdir;
        if ((file == null) || (file.trim().length() == 0)) {
            file = basedir + "/build/webapps/sitemap.xml";
        } else {
            file = file + "/sitemap.xml";
        }
        return file;
    }

    /**
     * The method call the database and build the sitemap.xml JDOM document.
     * 
     * @return The sitemap.xml as JDOM document
     */
    protected static final Document buildSitemap() throws Exception {
        LOGGER.debug("Build Google sitemap start.");
        final long start = System.currentTimeMillis();
        // build document frame
        Namespace ns = Namespace.getNamespace("http://www.google.com/schemas/sitemap/0.84");
        Element urlset = new Element("urlset", ns);
        Document jdom = new Document(urlset);

        // build over all types
        Document doc = null;
        XPath xpath = XPath.newInstance("/mycoreobject/service/servdates/servdate[@type='modifydate']");
        for (String type : types) {
            for (Object objID : tm.retrieveAllIDs(type)) {
                String mcrID = objID.toString();
                doc = tm.readDocument(new MCRObjectID(mcrID));
                Element servDate = (Element) xpath.selectSingleNode(doc);
                StringBuffer sb = new StringBuffer(1024);
                sb.append(baseurl).append(objectPath).append(mcrID);
                if ((style != null) && (style.trim().length() > 0)) {
                    sb.append("?XSL.Style=").append(style);
                }
                // build entry
                Element url = new Element("url", ns);
                url.addContent(new Element("loc", ns).addContent(sb.toString()));
                url.addContent(new Element("changefreq", ns).addContent(freq));
                url.addContent(new Element("lastmod", ns).addContent(servDate.getText()));
                urlset.addContent(url);
            }
        }
        LOGGER.debug("Google sitemap request took " + (System.currentTimeMillis() - start) + "ms.");
        return jdom;
    }

}