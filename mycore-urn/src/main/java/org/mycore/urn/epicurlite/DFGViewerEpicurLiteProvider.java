/**
 * 
 */
package org.mycore.urn.epicurlite;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.urn.hibernate.MCRURN;

/**
 * @author shermann
 *
 */
@Deprecated
public class DFGViewerEpicurLiteProvider extends BaseEpicurLiteProvider {

    /* (non-Javadoc)
     * @see org.mycore.urn.epicurlite.BaseEpicurLiteProvider#getURL(org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    public URL getURL(MCRURN urn) {
        URL url = null;
        try {
            MCRObjectID derivateId = MCRObjectID.getInstance(urn.getId());
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = derivate.getDerivate().getInternals().getMainDoc();

            String spec = null;
            if (mainDoc != null && mainDoc.length() > 0) {
                spec = MessageFormat.format(
                    MCRFrontendUtil.getBaseURL() + "servlets/MCRDFGLinkServlet?deriv={0}&file={1}",
                    URLEncoder.encode(derivateId.toString(), "UTF-8"), URLEncoder.encode(mainDoc, "UTF-8"));
            } else {
                spec = MCRFrontendUtil.getBaseURL() + "servlets/MCRDFGLinkServlet?deriv="
                    + URLEncoder.encode(derivateId.toString(), "UTF-8");
            }

            LOGGER.debug("Generated URL for urn " + urn.getURN() + " is " + spec);
            url = new URL(spec);
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            LOGGER.error("Could not create dfg viewer url", e);
        }
        return url;
    }
}
