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

package org.mycore.frontend.indexbrowser;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This class builds a google sitemap containing links to all documents. The
 * web.xml file should contain a mapping to /sitemap.xml See
 * http://www.google.com/webmasters/sitemaps/docs/en/protocol.html
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public final class MCRGoogleSitemapServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRGoogleSitemapServlet.class.getName());

    /**
     * This method implement the doGetPost method of MCRServlet. It build a XML
     * file for the Google search engine.
     * 
     * @param job
     *            a MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        File baseDir = MCRFrontendUtil
            .getWebAppBaseDir(getServletContext())
            .orElseGet(() -> new File(MCRConfiguration.instance().getString("MCR.WebApplication.basedir")));
        MCRGoogleSitemapCommon common = new MCRGoogleSitemapCommon(MCRFrontendUtil.getBaseURL(job.getRequest()),
            baseDir);
        int number = common.checkSitemapFile();
        LOGGER.debug("Build Google number of URL files {}.", Integer.toString(number));
        Document jdom = null;
        // check if sitemap_google.xml exist
        String fnsm = common.getFileName(1, true);
        LOGGER.debug("Build Google check file {}", fnsm);
        File fi = new File(fnsm);
        if (fi.isFile()) {
            jdom = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(fi));
            if (jdom == null) {
                if (number == 1) {
                    jdom = common.buildSingleSitemap();
                } else {
                    jdom = common.buildSitemapIndex(number);
                }
            }
            //send XML output
            getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
            return;
        }
        // remove old files
        common.removeSitemapFiles();
        // build new return and URL files
        if (number == 1) {
            jdom = common.buildSingleSitemap();
        } else {
            for (int i = 0; i < number; i++) {
                String fn = common.getFileName(i + 2, true);
                File xml = new File(fn);
                jdom = common.buildPartSitemap(i);
                LOGGER.info("Write Google sitemap file {}.", fn);
                new MCRJDOMContent(jdom).sendTo(xml);
            }
            jdom = common.buildSitemapIndex(number);
        }
        // send XML output
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
    }
}
