package org.mycore.pi.urn.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
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
 * @author Huu Chi Vu
 */
public class MCRDerivateURNUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SUPPORTED_CONTENT_TYPE = MCRConfiguration.instance().getString(
        "MCR.URN.URNGranular.SupportedContentTypes", "");

    public static URL getURL(MCRPIRegistrationInfo piInfo) {
        String derivateID = piInfo.getMycoreID();

        if (piInfo.getService().endsWith("-dfg")) {
            return getDFGViewerURL(piInfo);
        }

        try {
            // the base urn, links to frontpage (metadata + viewer)
            if (piInfo.getAdditional() == null || piInfo.getAdditional().trim().length() == 0) {
                MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivateID));
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
                        MCRFrontendUtil.getBaseURL() + "servlets/" + MCRFileNodeServlet.class.getSimpleName()
                            + filePath);
                }

                return new URL(getViewerURL(file));
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL for URN " + piInfo.getIdentifier(), e);
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

    public static URL getDFGViewerURL(MCRPIRegistrationInfo urn) {
        URL url = null;
        try {
            MCRObjectID derivateId = MCRObjectID.getInstance(urn.getMycoreID());
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = Optional.ofNullable(derivate.getDerivate())
                .map(MCRObjectDerivate::getInternals)
                .map(MCRMetaIFS::getMainDoc)
                .orElseThrow(() -> new RuntimeException(
                    "Could not get main doc for " + derivateId.toString()));

            String spec = null;
            String baseURL = MCRFrontendUtil.getBaseURL();
            String id = URLEncoder.encode(derivateId.toString(), "UTF-8");
            if (mainDoc != null && mainDoc.length() > 0) {
                String mainDocEnc = URLEncoder.encode(mainDoc, "UTF-8");
                spec = MessageFormat
                    .format(baseURL + "servlets/MCRDFGLinkServlet?deriv={0}&file={1}",
                        id, mainDocEnc);
            } else {
                spec = baseURL + "servlets/MCRDFGLinkServlet?deriv="
                    + id;
            }

            LOGGER.debug("Generated URL for urn " + urn.getIdentifier() + " is " + spec);
            url = new URL(spec);
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            LOGGER.error("Could not create dfg viewer url", e);
        }
        return url;
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
        //        return true;
    }
}
