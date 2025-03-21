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

package org.mycore.viewer.configuration;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerIIIFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerIIIFConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        // properties
        final String derivate = getDerivate(request);

        setProperty("manifestURL", MCRFrontendUtil.getBaseURL()
            + MCRConfiguration2.getStringOrThrow("MCR.Viewer.IIIF.URL.Presentation")
            + derivate + "/manifest");
        setProperty("imageAPIURL", MCRFrontendUtil.getBaseURL()
            + MCRConfiguration2.getStringOrThrow("MCR.Viewer.IIIF.URL.Image"));
        setProperty("filePath", getDerivate(request) + getFilePath(request));

        // script
        final boolean debugParameterSet = isDebugMode(request);
        addLocalScript("iview-client-iiif.es.js", false, true, debugParameterSet);
        // addLocalScript("lib/manifesto/manifesto.js", true, false, debugParameterSet);

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "manifest";
    }

}
