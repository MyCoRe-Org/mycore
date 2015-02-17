/**
 * 
 */
package org.mycore.urn.epicurlite;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.urn.hibernate.MCRURN;

/**
 * @author shermann
 *
 */
public class BaseEpicurLiteProvider implements IEpicurLiteProvider {

    static final Logger LOGGER = Logger.getLogger(BaseEpicurLiteProvider.class);

    /* (non-Javadoc)
     * @see org.mycore.urn.epicurlite.IEpicurLiteProvider#getEpicurLiteWrapper(org.mycore.backend.hibernate.tables.MCRURN)
     */
    @Override
    public EpicurLite getEpicurLite(MCRURN urn) {
        EpicurLite elp = new EpicurLite(urn);
        URL url = null;
        // the base urn
        if (urn.getPath() == null || urn.getPath().trim().length() == 0) {
            elp.setFrontpage(true);
        }
        url = getURL(urn);
        elp.setUrl(url);
        LOGGER.debug("Generated Epicur Lite for urn " + urn + " is \n" + MCRUtils.asString(elp.getEpicurLite()));
        return elp;
    }

    /* (non-Javadoc)
     * @see org.mycore.urn.epicurlite.IEpicurLiteProvider#getURL(org.mycore.datamodel.ifs.MCRFile)
     */
    public URL getURL(MCRURN urn) {
        URL url = null;

        try {
            // the base urn, links to frontpage (metadata + viewer)
            if (urn.getPath() == null || urn.getPath().trim().length() == 0) {
                MCRDerivate derivate = (MCRDerivate) MCRMetadataManager.retrieve(MCRObjectID.getInstance(urn.getId()));
                url = new URL(MCRFrontendUtil.getBaseURL() + "receive/" + derivate.getOwnerID() + "?derivate=" + urn.getId());
            }
            // an urn for a certain file, links to iview2
            else {
                MCRFile file = MCRFile.getMCRFile(MCRObjectID.getInstance(urn.getId()), urn.getPath() + urn.getFilename());
                if (file == null) {
                    LOGGER.warn("File " + urn.getFilename() + " in object " + urn.getId() + " could NOT be found");
                    return null;
                }

                String spec = BaseEpicurLiteProvider.getViewerURL(file.toPath());
                if (spec == null) {
                    LOGGER.info("File is not displayable within iView2. Use " + MCRFileNodeServlet.class.getSimpleName() + " as url");
                    String derivateId = file.getOwnerID();
                    String filePath = "/" + derivateId + file.getAbsolutePath();
                    spec = MCRServlet.getServletBaseURL() + MCRFileNodeServlet.class.getSimpleName() + filePath;
                }
                return new URL(spec);
            }
        } catch (Exception e) {
            LOGGER.error("Could not create url ", e);
        }
        return url;
    }

    /**
     * @param file
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private static String getViewerURL(MCRPath file) throws URISyntaxException, IOException {
        if (!MCRIView2Tools.isFileSupported(file)) {
            return null;
        }

        boolean useNewViewer = MCRConfiguration.instance().getBoolean("MCR.Module-iview2.useNewViewer", false);

        String url = null;
        if (useNewViewer) {
            url = MessageFormat.format("{0}rsc/viewer/{1}/{2}", MCRFrontendUtil.getBaseURL(), file.getOwner(), file.getFileName());
        } else {
            MCRObjectID mcrObjectID = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(file.getOwner()), 10, TimeUnit.SECONDS);
            String params = MCRXMLFunctions.encodeURIPath(MessageFormat.format("jumpback=true&maximized=true&page={0}&derivate={1}",
                    file.subpathComplete(), file.getOwner()));
            url = MessageFormat.format("{0}receive/{1}?{2}", MCRFrontendUtil.getBaseURL(), mcrObjectID, params);
        }

        return url;
    }
}
