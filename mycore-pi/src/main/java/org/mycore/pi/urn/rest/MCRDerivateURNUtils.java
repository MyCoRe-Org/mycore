package org.mycore.pi.urn.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.MCRPIRegistrationInfo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

/**
 * Created by chi on 07.02.17.
 * @author Huu Chi Vu
 */
public class MCRDerivateURNUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SUPPORTED_CONTENT_TYPE = MCRConfiguration.instance().getString(
            "MCR.URN.URNGranular.SupportedContentTypes", "");

    public static URL getURL(MCRPIRegistrationInfo piInfo) {
        String derivateID = piInfo.getMycoreID();
        try {
            // the base urn, links to frontpage (metadata + viewer)
            if (piInfo.getAdditional() == null || piInfo.getAdditional().trim().length() == 0) {
                MCRDerivate derivate = (MCRDerivate) MCRMetadataManager.retrieve(MCRObjectID.getInstance(
                        derivateID));
                return new URL(
                        MCRFrontendUtil.getBaseURL() + "receive/" + derivate.getOwnerID() + "?derivate=" + derivateID);
            }
            // an urn for a certain file, links to iview2
            else {
                MCRPath file = MCRPath.getPath(derivateID, piInfo.getAdditional());

                if (!Files.exists(file)) {
                    LOGGER.warn("File {} in object {} could NOT be found", file.getFileName().toString(), derivateID);
                    return null;
                }

                if (!isFileSupported(file)) {
                    LOGGER.info("File is not displayable within iView2. Use " + MCRFileNodeServlet.class.getSimpleName()
                                        + " as url");
                    String filePath = "/" + file.getOwner() + file.toString();
                    return new URL(
                            MCRFrontendUtil.getBaseURL()+ "servlets/" + MCRFileNodeServlet.class.getSimpleName() + filePath);
                }

                return new URL(getViewerURL(file));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param file
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private static String getViewerURL(MCRPath file) {
        return MessageFormat.format("{0}rsc/viewer/{1}/{2}", MCRFrontendUtil.getBaseURL(), file.getOwner(),
                                    file.getFileName().toString());
    }

    /**
     * @param file
     *            image file
     * @return if content type is in property <code>MCR.URN.URNGranular.SupportedContentTypes</code>
     * @see MCRContentTypes#probeContentType(Path)
     */
    private static boolean isFileSupported(MCRPath file) {
        try {
            return SUPPORTED_CONTENT_TYPE.contains(MCRContentTypes.probeContentType(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
