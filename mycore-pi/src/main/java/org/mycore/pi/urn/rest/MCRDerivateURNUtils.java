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

package org.mycore.pi.urn.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.MCRPIRegistrationInfo;

/**
 * Created by chi on 07.02.17.
 *
 * @author Huu Chi Vu
 */
public class MCRDerivateURNUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SUPPORTED_CONTENT_TYPE = MCRConfiguration2
        .getString("MCR.PI.URNGranular.SupportedContentTypes").orElse("");

    public static URL getURL(MCRPIRegistrationInfo piInfo) {
        String derivateID = piInfo.getMycoreID();

        if (piInfo.getService().endsWith("-dfg")) {
            return getDFGViewerURL(piInfo);
        }

        try {
            // the base urn, links to frontpage (metadata + viewer)
            if (piInfo.getAdditional() == null || piInfo.getAdditional().isBlank()) {
                MCRObjectID derID = MCRObjectID.getInstance(derivateID);
                final MCRObjectID objectId = MCRMetadataManager.getObjectId(derID, 0, TimeUnit.SECONDS);
                if (objectId == null) {
                    LOGGER.warn("Object for {} could NOT be found", derivateID);
                    return null;
                }
                return new URI(MCRFrontendUtil.getBaseURL() + "receive/" + objectId + "?derivate=" + derID).toURL();
            } else /* an urn for a certain file, links to iview2 */ {
                MCRPath file = MCRPath.getPath(derivateID, piInfo.getAdditional());

                if (!Files.exists(file)) {
                    LOGGER.warn("File {} in object {} could NOT be found", file::getFileName, () -> derivateID);
                    return null;
                }

                if (!isFileSupported(file)) {
                    if (LOGGER.isInfoEnabled()) { // false-positive on PMD:GuardLogStatement (static method reference)
                        LOGGER.info("File is not displayable within iView2. Use {} as url",
                            MCRFileNodeServlet.class::getSimpleName);
                    }
                    String filePath = "/" + file.getOwner()
                        + MCRXMLFunctions.encodeURIPath(file.getOwnerRelativePath());
                    return new URI(MCRFrontendUtil.getBaseURL() + "servlets/" + MCRFileNodeServlet.class.getSimpleName()
                        + filePath).toURL();
                }

                return new URI(getViewerURL(file)).toURL();
            }
        } catch (MalformedURLException | MCRPersistenceException | URISyntaxException e) {
            LOGGER.error(() -> "Malformed URL for URN " + piInfo.getIdentifier(), e);
        }

        return null;
    }

    private static String getViewerURL(MCRPath file) throws URISyntaxException {
        return new MessageFormat("{0}rsc/viewer/{1}{2}", Locale.ROOT).format(
            new Object[] { MCRFrontendUtil.getBaseURL(), file.getOwner(),
                MCRXMLFunctions.encodeURIPath(file.getOwnerRelativePath()) });
    }

    public static URL getDFGViewerURL(MCRPIRegistrationInfo urn) {
        URL url = null;
        try {
            MCRObjectID derivateId = MCRObjectID.getInstance(urn.getMycoreID());
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = Optional.ofNullable(derivate.getDerivate())
                .map(MCRObjectDerivate::getInternals)
                .map(MCRMetaIFS::getMainDoc)
                .orElseThrow(() -> new RuntimeException(
                    "Could not get main doc for " + derivateId));

            String spec;
            String baseURL = MCRFrontendUtil.getBaseURL();
            String id = URLEncoder.encode(derivateId.toString(), StandardCharsets.UTF_8);
            if (mainDoc != null && mainDoc.length() > 0) {
                String mainDocEnc = URLEncoder.encode(mainDoc, StandardCharsets.UTF_8);
                spec = String.format(Locale.ROOT, "%sservlets/MCRDFGLinkServlet?deriv=%s&file=%s", baseURL, id,
                    mainDocEnc);
            } else {
                spec = baseURL + "servlets/MCRDFGLinkServlet?deriv="
                    + id;
            }

            LOGGER.debug("Generated URL for urn {} is {}", urn::getIdentifier, () -> spec);
            url = new URI(spec).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.error("Could not create dfg viewer url", e);
        }
        return url;
    }

    /**
     * @param file image file
     * @return if content type is in property <code>MCR.PI.URNGranular.SupportedContentTypes</code>
     * @see MCRContentTypes#probeContentType(Path)
     */
    private static boolean isFileSupported(MCRPath file) {
        try {
            return SUPPORTED_CONTENT_TYPE.contains(MCRContentTypes.probeContentType(file));
        } catch (IOException e) {
            LOGGER.error(e::getMessage, e);
        }

        return false;
    }
}
