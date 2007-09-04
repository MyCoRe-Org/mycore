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

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

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

    /** The google namespace */
    private static final Namespace ns = Namespace.getNamespace("http://www.google.com/schemas/sitemap/0.84");

    /** The application basedir */
    private static final String basedir = MCRConfiguration.instance().getString("MCR.basedir", "");

    /** The base URL */
    private static final String baseurl = MCRConfiguration.instance().getString("MCR.baseurl", "");

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

    /** Number of URLs in one sitemap */
    private static final int numberOfURLs = MCRConfiguration.instance().getInt("MCR.GoogleSitemap.NumberOfURLs", 30000);

    /** The XML table API */
    private static final MCRXMLTableManager tm = MCRXMLTableManager.instance();

    /** number format for parts */
    private static DecimalFormat number_format = new DecimalFormat("0000");

    /** the slash */
    private static final String SLASH = System.getProperty("file.separator");

    /** local data */
    private ArrayList<String> objidlist = null;

    /** The constructor */
    public MCRGoogleSitemapCommon() {
        objidlist = new ArrayList<String>();
    }

    /**
     * The method compute the number of sitemap files. If we have less than
     * <em>numberOfURLs</em> URLs and only one MyCoRe type the sitemap_google.xml
     * containted all URSs. Otherwise it split the sitemap in an sitemap_google.xml
     * index file and a lot of sitemap_google_xxxx.xml URL files.
     * 
     * @return the number of files, one for a singel sitemap_google.xml file, more than
     *         one for the index and all parts.
     */
    protected final int checkSitemapFile() {
        int number = 0;
        for (String type : types) {
            for (Object objID : tm.retrieveAllIDs(type)) {
                objidlist.add(objID.toString());
            }
        }
        number = objidlist.size() / numberOfURLs;
        if (objidlist.size() % numberOfURLs != 0)
            number++;
        return number;
    }

    /**
     * The method return the path to the sitemap_google.xml file.
     * 
     * @param number
     *            number of this file - 1 = sitemap_google.xml - >1 sitemap_google_xxx.xml
     * @param withpath
     *            true for the full path, false for the file name
     * @return a path to sitemap_google.xml
     */
    protected final String getFileName(int number, boolean withpath) {
        StringBuffer sb = new StringBuffer(128);
        String fn = "sitemap_google.xml";
        if (number > 1) {
            fn = "sitemap_google_" + number_format.format(number - 1) + ".xml";
        }
        if (!withpath)
            return fn;
        if ((cdir == null) || (cdir.trim().length() == 0)) {
            sb.append(basedir).append(SLASH).append("build").append(SLASH).append("webapps").append(SLASH).append(fn);
        } else {
            sb.append(cdir).append(SLASH).append(fn);
        }
        return sb.toString();
    }

    /**
     * The method build the sitemap_google.xml JDOM document over all items.
     * 
     * @return The sitemap_google.xml as JDOM document
     */
    protected final Document buildSitemap() throws Exception {
        LOGGER.debug("Build Google URL sitemap_google.xml for whole items.");
        // build document frame
        Element urlset = new Element("urlset", ns);
        Document jdom = new Document(urlset);

        // build over all types
        Document doc = null;
        XPath xpath = XPath.newInstance("/mycoreobject/service/servdates/servdate[@type='modifydate']");
        for (int i = 0; i < objidlist.size(); i++) {
            String mcrID = objidlist.get(i);
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
        return jdom;
    }

    /**
     * The method call the database and build the sitemap_google.xml JDOM document.
     * 
     * @param number
     *            number of this file - 1 = sitemap_google.xml - > 1 sitemap_google_xxx.xml
     * @return The sitemap.xml as JDOM document
     */
    protected final Document buildSitemap(int number) throws Exception {
        LOGGER.debug("Build Google URL sitemap list number " + Integer.toString(number));
        // build document frame
        Element urlset = new Element("urlset", ns);
        Document jdom = new Document(urlset);

        // build over all types
        Document doc = null;
        XPath xpath = XPath.newInstance("/mycoreobject/service/servdates/servdate[@type='modifydate']");
        int start = numberOfURLs * (number);
        int stop = numberOfURLs * (number + 1);
        if (stop > objidlist.size())
            stop = objidlist.size();
        LOGGER.debug("Build Google URL in range from " + Integer.toString(start) + " to " + Integer.toString(stop - 1) + ".");
        for (int i = start; i < stop; i++) {
            String mcrID = objidlist.get(i);
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
        return jdom;
    }

    /**
     * The method build the index sitemap_google.xml JDOM document.
     * 
     * @param number
     *            number of indexed files (must greater than 1
     * @return The index sitemap_google.xml as JDOM document
     */
    protected final Document buildSitemapIndex(int number) throws Exception {
        LOGGER.debug("Build Google sitemap number " + Integer.toString(number));
        // build document frame
        Element index = new Element("sitemapindex", ns);
        Document jdom = new Document(index);
        // build over all files
        for (int i = 0; i < number; i++) {
            Element sitemap = new Element("sitemap", ns);
            index.addContent(sitemap);
            StringBuffer sb = new StringBuffer(128);
            sb.append(baseurl).append(getFileName(i + 2, false));
            sitemap.addContent(new Element("loc", ns).addContent(sb.toString()));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String datestr = formatter.format((new GregorianCalendar()).getTime());
            sitemap.addContent(new Element("lastmod", ns).addContent(datestr));
        }
        return jdom;
    }

    /**
     * This method remove all sitemap files from the webapps directory.
     */
    protected final void removeSitemapFiles() {
        StringBuffer sb = new StringBuffer(128);
        if ((cdir == null) || (cdir.trim().length() == 0)) {
            sb.append(basedir).append(SLASH).append("build").append(SLASH).append("webapps");
        } else {
            sb.append(cdir);
        }
        File dir = new File(sb.toString());
        File[] li = dir.listFiles();
        for (File fi : li) {
            if (fi.getName().startsWith("sitemap_google")) {
                LOGGER.debug("Remove file " + fi.getName());
                fi.delete();
            }
        }
    }
}