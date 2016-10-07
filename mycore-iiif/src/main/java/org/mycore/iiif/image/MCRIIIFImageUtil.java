package org.mycore.iiif.image;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;

import static org.mycore.iiif.image.resources.MCRIIIFImageResource.IIIF_IMAGE_API_2_LEVEL2;

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
