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
import org.mycore.frontend.MCRFrontendUtil;

public class MCRViewerLogoConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        String logoURL = MCRConfiguration.instance().getString("MCR.Viewer.logo.URL", null);
        if (logoURL != null) {
            String framedParameter = request.getParameter("frame");
            if (framedParameter == null || !Boolean.parseBoolean(framedParameter)) {
                this.addLocalScript("iview-client-logo.js", isDebugParameterSet(request));
                this.setProperty("logoURL", MCRFrontendUtil.getBaseURL() + logoURL);

                String logoCssProperty = MCRConfiguration.instance().getString("MCR.Viewer.logo.css", null);
                if (logoCssProperty != null) {
                    this.addCSS(MCRFrontendUtil.getBaseURL() + logoCssProperty);
                }
            }
        }

        return this;
    }

}
