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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

/**
 * Default image viewer configuration. Decides if the mets or the pdf configuration is used.
 * Returns the appropriate configuration with all plugins (piwik, metadata, logo).
 * 
 * @author Matthias Eichner
 */
public class MCRViewerDefaultConfigurationStrategy implements MCRViewerConfigurationStrategy {

    @Override
    public MCRViewerConfiguration get(HttpServletRequest request) {
        if (isPDF(request)) {
            return getPDF(request);
        } else {
            return getMETS(request);
        }
    }

    protected boolean isPDF(HttpServletRequest request) {
        // well, this is the best test to check the type, for sure!
        String filePath = MCRViewerConfiguration.getFilePath(request);
        return filePath != null && filePath.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    protected MCRViewerConfiguration getPDF(HttpServletRequest request) {
        return MCRViewerConfigurationBuilder.pdf(request).mixin(MCRViewerConfigurationBuilder.plugins(request).get())
            .get();
    }

    protected MCRViewerConfiguration getMETS(HttpServletRequest request) {
        return MCRViewerConfigurationBuilder.metsAndPlugins(request).get();
    }

}
