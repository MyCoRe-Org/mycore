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

            LOGGER.debug("Generated URL for urn {} is {}", urn.getURN(), spec);
            url = new URL(spec);
        } catch (UnsupportedEncodingException | MalformedURLException e) {
            LOGGER.error("Could not create dfg viewer url", e);
        }
        return url;
    }
}
