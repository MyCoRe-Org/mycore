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
