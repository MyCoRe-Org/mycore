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

package org.mycore.urn.epicurlite;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.urn.hibernate.MCRURN;

/**
 * @author shermann
 *
 */
@Deprecated
public class BaseEpicurLiteProvider implements IEpicurLiteProvider {

    private static String SUPPORTED_CONTENT_TYPE = MCRConfiguration.instance()
        .getString("MCR.URN.URNGranular.SupportedContentTypes", "");

    static final Logger LOGGER = LogManager.getLogger(BaseEpicurLiteProvider.class);

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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generated Epicur Lite for urn {} is \n{}", urn,
                new XMLOutputter(Format.getPrettyFormat()).outputString(elp.getEpicurLite()));
        }
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
                url = new URL(
                    MCRFrontendUtil.getBaseURL() + "receive/" + derivate.getOwnerID() + "?derivate=" + urn.getId());
            }
            // an urn for a certain file, links to iview2
            else {
                MCRFile file = MCRFile.getMCRFile(MCRObjectID.getInstance(urn.getId()),
                    urn.getPath() + urn.getFilename());
                if (file == null) {
                    LOGGER.warn("File {} in object {} could NOT be found", urn.getFilename(), urn.getId());
                    return null;
                }

                String spec = BaseEpicurLiteProvider.getViewerURL(file.toPath());
                if (spec == null) {
                    LOGGER.info("File is not displayable within iView2. Use {} as url",
                        MCRFileNodeServlet.class.getSimpleName());
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
        if (!isFileSupported(file)) {
            return null;
        }

        boolean useNewViewer = MCRConfiguration.instance().getBoolean("MCR.Module-iview2.useNewViewer", false);

        String url = null;
        if (useNewViewer) {
            url = MessageFormat.format("{0}rsc/viewer/{1}/{2}", MCRFrontendUtil.getBaseURL(), file.getOwner(),
                file.getFileName());
        } else {
            MCRObjectID mcrObjectID = MCRMetadataManager.getObjectId(MCRObjectID.getInstance(file.getOwner()), 10,
                TimeUnit.SECONDS);
            String params = MCRXMLFunctions
                .encodeURIPath(MessageFormat.format("jumpback=true&maximized=true&page={0}&derivate={1}",
                    file.subpathComplete(), file.getOwner()));
            url = MessageFormat.format("{0}receive/{1}?{2}", MCRFrontendUtil.getBaseURL(), mcrObjectID, params);
        }

        return url;
    }

    /**
     * @param file
     *            image file
     * @return if content type is in property <code>MCR.URN.URNGranular.SupportedContentTypes</code>
     * @see MCRContentTypes#probeContentType(Path)
     */
    public static boolean isFileSupported(Path file) throws IOException {
        return file != null && SUPPORTED_CONTENT_TYPE.contains(MCRContentTypes.probeContentType(file));
    }
}
