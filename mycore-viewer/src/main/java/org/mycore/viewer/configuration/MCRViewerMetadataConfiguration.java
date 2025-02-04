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

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.services.i18n.MCRTranslation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class MCRViewerMetadataConfiguration extends MCRViewerConfiguration {

    private static final int EXPIRE_METADATA_CACHE_TIME = 10; // in seconds

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        String derivate = getDerivate(request);
        MCRObjectID derivateID = MCRObjectID.getInstance(derivate);
        final MCRObjectID objectID = MCRMetadataManager.getObjectId(derivateID, EXPIRE_METADATA_CACHE_TIME,
            TimeUnit.SECONDS);
        if (objectID == null) {
            String errorMessage = MCRTranslation.translate("component.viewer.MCRIViewClientServlet.object.not.found");
            // TODO: we should not throw an webapplication exc. here -> instead throw something líke ConfigException
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(errorMessage).build());
        }

        // properties
        setProperty("objId", objectID.toString());
        String urlFormat = "%sreceive/%s?XSL.Transformer=%s";
        String transformer = MCRConfiguration2.getString("MCR.Viewer.metadata.transformer").orElse(null);
        if (transformer != null) {
            setProperty(
                "metadataURL",
                String.format(Locale.ROOT, urlFormat, MCRFrontendUtil.getBaseURL(), objectID, transformer));
        }

        // script
        addLocalScript("iview-client-metadata.es.js", false, true, isDebugMode(request));

        return this;
    }

}
