package org.mycore.iiif.model;


import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class MCRIIIFProfile {

    @SerializedName("@context")
    public String context;

    @SerializedName("@type")
    public final String type = "iiif:ImageProfile";

    @SerializedName("@id")
    public String id;

    public Set<MCRIIIFFeatures> supports = new HashSet<>();

    public MCRIIIFProfile(){
        supports.add(MCRIIIFFeatures.baseUriRedirect);
        supports.add(MCRIIIFFeatures.canonicalLinkHeader);
        supports.add(MCRIIIFFeatures.cors);
        supports.add(MCRIIIFFeatures.jsonldMediaType);
        supports.add(MCRIIIFFeatures.profileLinkHeader);
        supports.add(MCRIIIFFeatures.profileLinkHeader);
    }

    public Set<String> formats = new HashSet<>();
    public Set<String> qualities = new HashSet<>();


}
