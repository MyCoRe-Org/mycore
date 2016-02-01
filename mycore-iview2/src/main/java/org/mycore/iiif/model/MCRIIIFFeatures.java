package org.mycore.iiif.model;


public enum MCRIIIFFeatures {
    baseUriRedirect("baseUriRedirect"),
    canonicalLinkHeader("canonicalLinkHeader"),
    cors("cors"),
    jsonldMediaType("jsonldMediaType"),
    mirroring("mirroring"),
    profileLinkHeader("profileLinkHeader"),
    regionByPct("regionByPct"),
    regionByPx("regionByPx"),
    rotationArbitrary("rotationArbitrary"),
    rotationBy90s("rotationBy90s"),
    sizeAboveFull("sizeAboveFull"),
    sizeByWhListed("sizeByWhListed"),
    sizeByForcedWh("sizeByForcedWh"),
    sizeByH("sizeByH"),
    sizeByPct("sizeByPct"),
    sizeByW("sizeByW"),
    sizeByWh("sizeByWh");


    private String featureName;

    MCRIIIFFeatures(String featureName) {
        this.featureName = featureName;
    }

    @Override
    public String toString() {
        return featureName;
    }
}
