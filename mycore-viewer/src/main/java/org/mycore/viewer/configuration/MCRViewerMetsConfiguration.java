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

import java.nio.file.Files;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerMetsConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        final String derivate = getDerivate(request);

        setProperty("metsURL", MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + derivate);
        // Parameter can be used to provide multiple urls
        String imageXmlPath = MCRConfiguration2.getString("MCR.Viewer.BaseURL").orElse(null);

        if (imageXmlPath == null || imageXmlPath.isEmpty()) {
            imageXmlPath = MCRServlet.getServletBaseURL() + "MCRTileServlet/";
        }
        setProperty("tileProviderPath", imageXmlPath);
        if (imageXmlPath.contains(",")) {
            imageXmlPath = imageXmlPath.split(",")[0];
        }
        setProperty("imageXmlPath", imageXmlPath);

        setProperty("pdfCreatorStyle", MCRConfiguration2.getString("MCR.Viewer.PDFCreatorStyle").orElse(null));
        setProperty("pdfCreatorURI", MCRConfiguration2.getString("MCR.Viewer.PDFCreatorURI").orElse(null));
        setProperty("text.enabled", MCRConfiguration2.getString("MCR.Viewer.text.enabled").orElse("false"));

        setProperty("pdfCreatorFormatString",
            MCRConfiguration2.getString("MCR.Viewer.PDFCreatorFormatString").orElse(null));
        setProperty("pdfCreatorRestrictionFormatString",
            MCRConfiguration2.getString("MCR.Viewer.PDFCreatorRestrictionFormatString").orElse(null));

        // script
        final boolean debugParameterSet = isDebugMode(request);
        addLocalScript("iview-client-mets.js", true, debugParameterSet);

        final MCRPath teiDirectoryPath = MCRPath.getPath(derivate, "/tei");
        if (Files.exists(teiDirectoryPath) && Files.isDirectory(teiDirectoryPath)) {
            addLocalScript("iview-client-tei.js", true, debugParameterSet);
            addLocalCSS("tei.css");
            MCRConfiguration2.getString("MCR.Viewer.TeiStyle")
                .ifPresent((style) -> setProperty("teiStylesheet", style));
        }

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "mets";
    }

}
