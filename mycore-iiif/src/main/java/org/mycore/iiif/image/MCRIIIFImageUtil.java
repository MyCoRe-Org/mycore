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

package org.mycore.iiif.image;

import static org.mycore.iiif.image.resources.MCRIIIFImageResource.IIIF_IMAGE_API_2_LEVEL2;

import java.net.URI;
import java.net.URISyntaxException;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;

public class MCRIIIFImageUtil {
    public static void completeProfile(MCRIIIFImageImpl impl, MCRIIIFImageProfile profile) {
        profile.setId(getProfileLink(impl));
    }

    /**
     * Encodes image identidier so it can be used in an URI
     * @param imageIdentifier
     * @see <a href="https://iiif.io/api/image/2.1/#uri-encoding-and-decoding">URI encoding of image identifier</a>
     */
    public static String encodeImageIdentifier(String imageIdentifier) {

        try {
            final URI uri = new URI(null, null, imageIdentifier, null, null);
            return uri.toASCIIString().replace("/", "%2F");
        } catch (URISyntaxException e) {
            //99.99% this won't happen
            throw new MCRException("Could not encode image identifier: " + imageIdentifier, e);
        }
    }

    public static String buildCanonicalURL(MCRIIIFImageImpl impl, String identifier) {
        return "<" + getIIIFURL(impl) + encodeImageIdentifier(identifier)
            + "/full/full/0/color.jpg>;rel=\"canonical\"";
    }

    public static String buildProfileURL() {
        return "<" + IIIF_IMAGE_API_2_LEVEL2 + ">;rel=\"profile\"";
    }

    public static String getIIIFURL(MCRIIIFImageImpl impl) {
        StringBuffer sb = new StringBuffer(MCRFrontendUtil.getBaseURL());
        sb.append("api/iiif/image/v2/");
        String defaultImpl = MCRConfiguration2.getString("MCR.IIIFImage.Default").orElse("");
        if (!defaultImpl.equals(impl.getImplName())) {
            sb.append(impl.getImplName()).append('/');
        }
        return sb.toString();
    }

    public static String getProfileLink(MCRIIIFImageImpl impl) {
        return getIIIFURL(impl) + "profile.json";
    }

    public static MCRIIIFImageImpl getImpl(String impl) {
        return MCRIIIFImageImpl.getInstance(impl);
    }
}
