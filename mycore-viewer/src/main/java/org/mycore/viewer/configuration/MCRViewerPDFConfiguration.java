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
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpServletRequest;

public class MCRViewerPDFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String pdfProviderURL = MCRConfiguration2.getString("MCR.Viewer.pdfProviderURL")
            .orElse(MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/{derivate}/{filePath}");
        String pdfWorkerLocation = MCRFrontendUtil.getBaseURL() + "modules/iview2/js/lib/pdf.worker.min.js";

        setProperty("pdfProviderURL", pdfProviderURL);
        setProperty("pdfWorkerURL", pdfWorkerLocation);
        // script
        addLocalScript("iview-client-pdf.es.js", false, true, isDebugMode(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
