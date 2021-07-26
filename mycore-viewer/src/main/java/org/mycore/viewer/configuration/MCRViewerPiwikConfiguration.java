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

import org.mycore.common.config.MCRConfiguration2;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerPiwikConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        if (MCRConfiguration2.getBoolean("MCR.Piwik.enable").orElse(false)) {
            this.addLocalScript("iview-client-piwik.js", true, isDebugMode(request));
            this.setProperty("MCR.Piwik.baseurl", MCRConfiguration2.getStringOrThrow("MCR.Piwik.baseurl"));
            this.setProperty("MCR.Piwik.id", MCRConfiguration2.getStringOrThrow("MCR.Piwik.id"));
        }
        return this;
    }

}
