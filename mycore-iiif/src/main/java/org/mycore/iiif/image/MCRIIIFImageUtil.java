/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.iiif.image;

import static org.mycore.iiif.image.resources.MCRIIIFImageResource.IIIF_IMAGE_API_2_LEVEL2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;

public class MCRIIIFImageUtil {
    public static void completeProfile(MCRIIIFImageImpl impl, MCRIIIFImageProfile profile) {
        profile.setId(getProfileLink(impl));
    }

    public static String buildCanonicalURL(MCRIIIFImageImpl impl, String identifier)
        throws UnsupportedEncodingException {
        return "<" + getIIIFURL(impl) + URLEncoder.encode(identifier, "UTF-8")
            + "/full/full/0/color.jpg>;rel=\"canonical\"";
    }

    public static String buildProfileURL() throws UnsupportedEncodingException {
        return String.format(Locale.ROOT, "<" + IIIF_IMAGE_API_2_LEVEL2 + ">;rel=\"profile\"");
    }

    public static String getIIIFURL(MCRIIIFImageImpl impl) {
        return MCRFrontendUtil.getBaseURL() + "rsc/iiif/image/" + impl.getImplName() + "/";
    }

    public static String getProfileLink(MCRIIIFImageImpl impl) {
        return getIIIFURL(impl) + "profile.json";
    }

    public static MCRIIIFImageImpl getImpl(String impl) {
        return MCRIIIFImageImpl.getInstance(impl);
    }
}
