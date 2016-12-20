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
