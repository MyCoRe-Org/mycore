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

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * This class builds a google sitemap containing links to all documents and
 * store them to the webapps directory. This can be configured with property
 * variable MCR.GoogleSitemap.Directory. The web.xml file should contain a
 * mapping to /sitemap.xml
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 */
@MCRCommandGroup(name = "Google Sitemap Commands")
public final class MCRGoogleSitemapCommands extends MCRAbstractCommands {

    /** The logger */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The build and store method.
     */
    @MCRCommand(syntax = "build google sitemap",
        help = "Creates the google sitemap(s) in webapps directory.",
        order = 10)
    public static void buildSitemap() throws Exception {
        // check time
        LOGGER.debug("Build Google sitemap start.");
        final long start = System.currentTimeMillis();
        // init
        File webappBaseDir = new File(MCRConfiguration2.getStringOrThrow("MCR.WebApplication.basedir"));
        MCRGoogleSitemapCommon common = new MCRGoogleSitemapCommon(webappBaseDir);
        // remove old files
        common.removeSitemapFiles();
        // compute number of files
        int number = common.checkSitemapFile();
        LOGGER.debug("Build Google number of URL files {}.", number);
        if (number == 1) {
            String fn = common.getFileName(1, true);
            File xml = new File(fn);
            Document jdom = common.buildSingleSitemap();
            LOGGER.info("Write Google sitemap file {}.", fn);
            new MCRJDOMContent(jdom).sendTo(xml);
        } else {
            String fn = common.getFileName(1, true);
            File xml = new File(fn);
            Document jdom = common.buildSitemapIndex(number);
            LOGGER.info("Write Google sitemap file {}.", fn);
            new MCRJDOMContent(jdom).sendTo(xml);
            for (int i = 0; i < number; i++) {
                fn = common.getFileName(i + 2, true);
                xml = new File(fn);
                jdom = common.buildPartSitemap(i);
                LOGGER.info("Write Google sitemap file {}.", fn);
                new MCRJDOMContent(jdom).sendTo(xml);
            }
        }
        // check time
        LOGGER.debug("Google sitemap request took {}ms.", () -> System.currentTimeMillis() - start);
    }

}
