package org.mycore.iiif.image.model;

import java.util.HashSet;
import java.util.Set;

import org.mycore.iiif.model.MCRIIIFBase;

public class MCRIIIFImageProfile extends MCRIIIFBase {

    public static final String IIIF_PROFILE_2_0 = "http://iiif.io/api/image/2/profiles/level2.json";

    public static final String IIIF_IMAGE_PROFILE = "iiif:ImageProfile";

    public Set<MCRIIIFFeatures> supports = new HashSet<>();

    public Set<String> formats = new HashSet<>();

    public Set<String> qualities = new HashSet<>();

    public MCRIIIFImageProfile() {
        super(IIIF_IMAGE_PROFILE, API_IMAGE_2);
        supports.add(MCRIIIFFeatures.baseUriRedirect);
        supports.add(MCRIIIFFeatures.canonicalLinkHeader);
        supports.add(MCRIIIFFeatures.cors);
        supports.add(MCRIIIFFeatures.jsonldMediaType);
        supports.add(MCRIIIFFeatures.profileLinkHeader);
        supports.add(MCRIIIFFeatures.profileLinkHeader);
    }

}
