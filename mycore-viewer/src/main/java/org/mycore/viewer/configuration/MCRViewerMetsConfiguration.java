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

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRViewerMetsConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        setProperty("metsURL", MCRServlet.getServletBaseURL() + "MCRMETSServlet/" + getDerivate(request));
        String imageXmlPath = MCRConfiguration.instance().getString("MCR.Viewer.BaseURL", null); // Parameter can be used to provide multiple urls

        if (imageXmlPath == null || imageXmlPath.isEmpty()) {
            imageXmlPath = MCRServlet.getServletBaseURL() + "MCRTileServlet/";
        }
        setProperty("tileProviderPath", imageXmlPath);
        if (imageXmlPath.contains(",")) {
            imageXmlPath = imageXmlPath.split(",")[0];
        }
        setProperty("imageXmlPath", imageXmlPath);

        setProperty("pdfCreatorStyle", MCRConfiguration.instance().getString("MCR.Viewer.PDFCreatorStyle", null));
        setProperty("pdfCreatorURI", MCRConfiguration.instance().getString("MCR.Viewer.PDFCreatorURI", null));
        setProperty("text.enabled", MCRConfiguration.instance().getString("MCR.Viewer.text.enabled", "false"));

        MCRConfiguration configuration = MCRConfiguration.instance();
        setProperty("pdfCreatorFormatString",
            configuration.getString("MCR.Viewer.PDFCreatorFormatString", null));
        setProperty("pdfCreatorRestrictionFormatString", configuration
            .getString("MCR.Viewer.PDFCreatorRestrictionFormatString", null));

        // script
        addLocalScript("iview-client-mets.js", isDebugParameterSet(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "mets";
    }

}
