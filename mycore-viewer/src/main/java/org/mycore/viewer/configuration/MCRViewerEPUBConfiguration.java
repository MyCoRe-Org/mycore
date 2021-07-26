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

import org.mycore.frontend.MCRFrontendUtil;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerEPUBConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        final boolean debugMode = isDebugMode(request);
        addLocalScript("lib/epubjs/epub.js", true, debugMode);
        addLocalScript("lib/jszip/jszip.js", true, debugMode);
        addLocalScript("iview-client-epub.js", true, debugMode);
        addLocalScript("lib/es6-promise/es6-promise.auto.js", true, debugMode);

        final String derivate = getDerivate(request);
        final String filePath = getFilePath(request);

        // put / at the end to let epubjs know its extracted
        setProperty("epubPath", MCRFrontendUtil.getBaseURL(request) + "rsc/epub/" + derivate + filePath + "/");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "epub";
    }
}
