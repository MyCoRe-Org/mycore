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

import org.apache.log4j.Logger;
import org.jdom.Document;

import org.mycore.common.MCRUtils;
import org.mycore.common.xml.MCRXMLHelper;
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

    /** The logger */
    private static Logger LOGGER = Logger.getLogger(MCRGoogleSitemapServlet.class.getName());

    /**
     * This method implement the doGetPost method of MCRServlet. It build a XML
     * file for the Google search engine.
     * 
     * @param job
     *            a MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        MCRGoogleSitemapCommon common = new MCRGoogleSitemapCommon();
        int number = common.checkSitemapFile();
        LOGGER.debug("Build Google number of URL files " + Integer.toString(number) + ".");
        Document jdom = null;
        // check if sitemap_google.xml exist
        String fnsm = common.getFileName(1, true);
        LOGGER.debug("Build Google check file " + fnsm);
        File fi = new File(fnsm);
        if (fi.isFile()) {
            jdom = MCRXMLHelper.parseURI(fnsm, false);
            if (jdom == null) {
                if (number == 1) {
                    jdom = common.buildSitemap();
                } else {
                    jdom = common.buildSitemapIndex(number);
                }
            }
            // redirect Layout Servlet
            getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdom);
            return;
        }
        // remove old files
        common.removeSitemapFiles();
        // build new return and URL files
        if (number == 1) {
            jdom = common.buildSitemap();
        } else {
            for (int i = 0; i < number; i++) {
                String fn = common.getFileName(i + 2, true);
                File xml = new File(fn);
                jdom = common.buildSitemap(i);
                LOGGER.info("Write Google sitemap file " + fn + ".");
                MCRUtils.writeJDOMToFile(jdom, xml);
            }
            jdom = common.buildSitemapIndex(number);
        }
        // redirect Layout Servlet
        getLayoutService().sendXML(job.getRequest(), job.getResponse(), jdom);
    }
}
