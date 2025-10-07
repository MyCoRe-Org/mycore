/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import jakarta.servlet.http.HttpServletResponse;

/**
 * This class builds a google sitemap containing links to all documents. The
 * web.xml file should contain a mapping to /sitemap.xml
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 */
public final class MCRGoogleSitemapServlet extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern PARTSITEMAP_FILENAME_PATTERN = Pattern.compile("sitemap_google_([0-9]+)\\.xml");

    /**
     * This method implement the doGetPost method of MCRServlet. It build a XML
     * file for the Google search engine.
     *
     * @param job
     *            a MCRServletJob instance
     */
    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        String uri = job.getRequest().getRequestURI();
        String filename = uri.substring(uri.lastIndexOf('/') + 1);
        if (MCRGoogleSitemapCommon.DEFAULT_FILENAME.equals(filename)) {
            processSingleSitemap(job);
        } else {
            processPartSitemap(filename, job);
        }
    }

    private void processSingleSitemap(MCRServletJob job) throws Exception {
        MCRGoogleSitemapCommon common = new MCRGoogleSitemapCommon(MCRFrontendUtil.getBaseURL(job.getRequest()));
        int number = common.checkSitemapFile();
        LOGGER.debug("Build Google number of URL files {}.", number);
        Document jdom;
        // check if sitemap_google.xml exist
        // the file can be pre-generated with MyCoRe CLI:> build google sitemap 
        // after that it won't be updated anymore until deleted or recreated
        Path fi = common.getFile(0);
        LOGGER.debug("Build Google check file {}", fi);
        if (Files.isRegularFile(fi)) {
            jdom = MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRPathContent(fi));
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
            for (int i = 1; i <= number; i++) {
                Path xml = common.getFile(i);
                jdom = common.buildPartSitemap(i);
                LOGGER.info("Write Google sitemap file {}.", xml);
                new MCRJDOMContent(jdom).sendTo(xml);
            }
            jdom = common.buildSitemapIndex(number);
        }
        // send XML output
        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
    }

    private void processPartSitemap(String filename, MCRServletJob job) throws Exception {
        Matcher m = PARTSITEMAP_FILENAME_PATTERN.matcher(filename);
        if (m.matches()) {
            int idx = Integer.parseInt(m.group(1));
            MCRGoogleSitemapCommon common = new MCRGoogleSitemapCommon(MCRFrontendUtil.getBaseURL(job.getRequest()));

            Path xml = common.getFile(idx);
            if (Files.isRegularFile(xml)) {
                getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRPathContent(xml));
            } else {
                int number = common.checkSitemapFile();
                if (idx <= number) {
                    Document jdom = common.buildPartSitemap(idx);
                    new MCRJDOMContent(jdom).sendTo(xml);
                    getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(jdom));
                } else {
                    job.getResponse().sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Partial Google Sitemap '" + filename + "' not found!");
                }
            }
        }
    }

}
