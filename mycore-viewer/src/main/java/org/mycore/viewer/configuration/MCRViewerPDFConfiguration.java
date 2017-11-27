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
import org.mycore.frontend.servlets.MCRServlet;

public class MCRViewerPDFConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String pdfProviderURL = MCRConfiguration.instance().getString("MCR.Viewer.pdfProviderURL",
            MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/{derivate}/{filePath}");
        String pdfWorkerLocation = MCRFrontendUtil.getBaseURL() + "modules/iview2/js/lib/pdf.min.worker.js";

        setProperty("pdfProviderURL", pdfProviderURL);
        setProperty("pdfWorkerURL", pdfWorkerLocation);
        // script
        addLocalScript("lib/pdf.js", false);
        addLocalScript("iview-client-pdf.js", isDebugParameterSet(request));

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "pdf";
    }

}
