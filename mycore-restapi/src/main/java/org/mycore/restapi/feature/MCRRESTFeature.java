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

package org.mycore.restapi.feature;

import java.util.List;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;
import org.mycore.restapi.MCRRESTResourceConfig;

public class MCRRESTFeature extends MCRJerseyDefaultFeature {

    @Override
    protected List<String> getPackages() {
        return MCRConfiguration.instance().getStrings(MCRRESTResourceConfig.REST_API_PACKAGE);
    }
}
