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

package org.mycore.iview2.frontend;

import java.nio.file.Files;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * Adapter that can be extended to work with different internal files systems.
 * To get the extending class invoked, one need to define a MyCoRe property, which defaults to:
 * <code>MCR.Module-iview2.MCRIView2XSLFunctionsAdapter=org.mycore.iview2.frontend.MCRIView2XSLFunctionsAdapter</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2XSLFunctionsAdapter {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRLinkTableManager LINK_TABLE_MANAGER = MCRLinkTableManager.getInstance();

    public static MCRIView2XSLFunctionsAdapter obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public boolean hasMETSFile(String derivateID) {
        return Files.exists(MCRPath.getPath(derivateID, "/mets.xml"));
    }

    public String getSupportedMainFile(String derivateID) {
        return MCRIView2Tools.getSupportedMainFile(derivateID);
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public String getOptions(String derivateID, String extensions) {
        final Collection<String> sources = LINK_TABLE_MANAGER.getSourceOf(derivateID, "derivate");
        String objectID = (sources != null && !sources.isEmpty()) ? sources.iterator().next() : "";
        StringBuilder options = new StringBuilder();
        options.append('{');
        options.append("\"derivateId\":").append('\"').append(derivateID).append("\",");
        options.append("\"objectId\":").append('\"').append(objectID).append("\",");
        options.append("\"webappBaseUri\":").append('\"').append(MCRFrontendUtil.getBaseURL()).append("\",");
        String baseUris = MCRConfiguration2.getString("MCR.Module-iview2.BaseURL").orElse("");
        if (baseUris.length() < 10) {
            baseUris = MCRServlet.getServletBaseURL() + "MCRTileServlet";
        }
        options.append("\"baseUri\":").append('\"').append(baseUris).append("\".split(\",\")");
        if (MCRAccessManager.checkPermission(derivateID, "create-pdf")) {
            options.append(",\"pdfCreatorURI\":").append('\"')
                .append(MCRConfiguration2.getString("MCR.Module-iview2.PDFCreatorURI").orElse("")).append("\",");
            options.append("\"pdfCreatorStyle\":").append('\"')
                .append(MCRConfiguration2.getString("MCR.Module-iview2.PDFCreatorStyle").orElse("")).append('\"');
        }

        if (extensions != null && !extensions.isEmpty()) {
            options.append(',');
            options.append(extensions);
        }

        options.append('}');
        LOGGER.debug(options);
        return options.toString();
    }

    private static final class LazyInstanceHolder {
        public static final MCRIView2XSLFunctionsAdapter SHARED_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRIView2XSLFunctionsAdapter.class, MCRIView2Tools.CONFIG_PREFIX + "MCRIView2XSLFunctionsAdapter");
    }

}
