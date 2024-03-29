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

package org.mycore.iiif.image.model;

public enum MCRIIIFFeatures {
    baseUriRedirect("baseUriRedirect"),
    canonicalLinkHeader("canonicalLinkHeader"),
    cors("cors"),
    jsonldMediaType(
        "jsonldMediaType"),
    mirroring("mirroring"),
    profileLinkHeader("profileLinkHeader"),
    regionByPct(
        "regionByPct"),
    regionByPx("regionByPx"),
    rotationArbitrary("rotationArbitrary"),
    rotationBy90s(
        "rotationBy90s"),
    sizeAboveFull("sizeAboveFull"),
    sizeByWhListed("sizeByWhListed"),
    sizeByForcedWh(
        "sizeByForcedWh"),
    sizeByH(
        "sizeByH"),
    sizeByPct("sizeByPct"),
    sizeByW("sizeByW"),
    sizeByWh("sizeByWh");

    private final String featureName;

    MCRIIIFFeatures(String featureName) {
        this.featureName = featureName;
    }

    @Override
    public String toString() {
        return featureName;
    }
}
