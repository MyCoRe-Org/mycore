/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.iiif.image.model;

public enum MCRIIIFFeatures {
    BASE_URI_REDIRECT("baseUriRedirect"),
    CANONICAL_LINK_HEADER("canonicalLinkHeader"),
    CORS("cors"),
    JSONLD_MEDIA_TYPE(
        "jsonldMediaType"),
    MIRRORING("mirroring"),
    PROFILE_LINK_HEADER("profileLinkHeader"),
    REGION_BY_PCT(
        "regionByPct"),
    REGION_BY_PX("regionByPx"),
    ROTATION_ARBITRARY("rotationArbitrary"),
    ROTATION_BY_90_S(
        "rotationBy90s"),
    SIZE_ABOVE_FULL("sizeAboveFull"),
    SIZE_BY_WH_LISTED("sizeByWhListed"),
    SIZE_BY_FORCED_WH(
        "sizeByForcedWh"),
    SIZE_BY_H(
        "sizeByH"),
    SIZE_BY_PCT("sizeByPct"),
    SIZE_BY_W("sizeByW"),
    SIZE_BY_WH("sizeByWh");

    private final String featureName;

    MCRIIIFFeatures(String featureName) {
        this.featureName = featureName;
    }

    @Override
    public String toString() {
        return featureName;
    }
}
