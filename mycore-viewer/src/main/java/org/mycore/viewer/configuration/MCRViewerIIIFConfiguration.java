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

package org.mycore.viewer.configuration;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;

public class MCRViewerIIIFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerIIIFConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        final String derivate = getDerivate(request);

        setProperty("manifestURL", MCRFrontendUtil.getBaseURL() + "rsc/iiif/presentation/mets/" + derivate + "/manifest");
        setProperty("imageAPIURL", MCRFrontendUtil.getBaseURL() + "rsc/iiif/image/Iview/");
//        String imageXmlPath = MCRConfiguration.instance().getString("MCR.Viewer.BaseURL", null); // Parameter can be used to provide multiple urls
//
//        if (imageXmlPath == null || imageXmlPath.isEmpty()) {
//            imageXmlPath = MCRServlet.getServletBaseURL() + "MCRTileServlet/";
//        }
//        setProperty("tileProviderPath", imageXmlPath);

        // script
        final boolean debugParameterSet = isDebugMode(request);
        addLocalScript("iview-client-iiif.js", true, debugParameterSet);
        addLocalScript("lib/manifesto/manifesto.js", true, isDebugMode(request));

        final MCRPath teiDirectoryPath = MCRPath.getPath(derivate, "/tei");
        if (Files.exists(teiDirectoryPath) && Files.isDirectory(teiDirectoryPath)) {
            addLocalScript("iview-client-tei.js", true, debugParameterSet);
            addLocalCSS("tei.css");
            MCRConfiguration2.getString("MCR.Viewer.TeiStyle")
                .ifPresent((style)-> setProperty("teiStylesheet", style));
        }

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "manifest";
    }

}
