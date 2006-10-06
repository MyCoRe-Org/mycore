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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This class builds a google sitemap containing links to all documents. The
 * web.xml file should contain a mapping to /sitemap.xml See
 * http://www.google.com/webmasters/sitemaps/docs/en/protocol.html
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
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
        String addr = getBaseURL() + "receive/";
        // build ducment frame
        Namespace ns = Namespace.getNamespace("http://www.google.com/schemas/sitemap/0.84");
        Element urlset = new Element("urlset", ns);
        Document jdom = new Document(urlset);

        // set current date
        GregorianCalendar calcurrent = new GregorianCalendar();
        String pattern = "yyyy-MM-dd'T'hh:mm'+01:00'";
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        String timecurrent = df.format(calcurrent.getTime());
        String time = null;
        LOGGER.info("Start Google access on " + timecurrent);

        // build over all types
        String id = null;
        MCRObjectID mid = new MCRObjectID();
        byte[] xml = null;
        Document doc = null;
        Element mycore = null;
        Element service = null;
        Element servdates = null;
        List servdatelist = null;
        Element servdate = null;
        for (int i = 0; i < types.length; i++) {
            ArrayList al = tm.retrieveAllIDs(types[i]);
            for (int j = 0; j < al.size(); j++) {
                id = (String) al.get(j);
                LOGGER.debug("Read object with ID " + id);
                mid.setID(id);
                xml = tm.retrieve(mid);
                doc = MCRXMLHelper.parseXML(xml, false);
                mycore = doc.getRootElement();
                if (mycore == null) {
                    LOGGER.warn("No root element for object ID " + id);
                    continue;
                }
                service = mycore.getChild("service");
                if (service == null) {
                    LOGGER.warn("No service element for object ID " + id);
                    continue;
                }
                servdates = service.getChild("servdates");
                if (servdates == null) {
                    LOGGER.warn("No servdates element for object ID " + id);
                    continue;
                }
                servdatelist = servdates.getChildren("servdate");
                if ((servdatelist == null) || (servdatelist.size() == 0)) {
                    LOGGER.warn("No servdate element for object ID " + id);
                    continue;
                }
                for (int k = 0; k < servdatelist.size(); k++) {
                    servdate = (Element) servdatelist.get(k);
                    if (servdate.getAttributeValue("type").equals("modifydate")) {
                        time = servdate.getText();
                        break;
                    }
                }
                // build entry
                Element url = new Element("url", ns);
                Element loc = new Element("loc", ns);
                Element lastmod = new Element("lastmod", ns);
                Element changefreq = new Element("changefreq", ns);
                changefreq.addContent(freq);
                loc.addContent(addr + id);
                try {
                    time = time.substring(0, 16) + "+01:00";
                    lastmod.addContent(time);
                } catch (Exception e) {
                    lastmod.addContent(timecurrent);
                }
                url.addContent(loc);
                url.addContent(changefreq);
                url.addContent(lastmod);
                urlset.addContent(url);
            }
        }

        // redirect Layout Servlet
        calcurrent = new GregorianCalendar();
        timecurrent = df.format(calcurrent.getTime());
        LOGGER.info("Stop Google access on " + timecurrent);
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", "xml");

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
