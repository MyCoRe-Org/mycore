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

/**
 * 
 */
package org.mycore.restapi;

import org.glassfish.jersey.server.ResourceConfig;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.jersey.MCRJerseyDefaultConfiguration;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;
import org.mycore.restapi.v1.feature.MCRRESTFeature;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRestConfig extends MCRJerseyDefaultConfiguration {

    public static final String REST_API_PACKAGE = "MCR.RestAPI.Resource.Packages";


    @Override
    public void configure(ResourceConfig resourceConfig) {
        resourceConfig.register(MCRRestAPIExceptionMapper.class);
        super.configure(resourceConfig);
    }

    @Override
    protected void setupResources(ResourceConfig resourceConfig) {
        resourceConfig.packages(MCRConfiguration.instance().getStrings(REST_API_PACKAGE).toArray(new String[0]));
    }

    @Override
    protected void setupFeatures(ResourceConfig resourceConfig) {
        super.setupFeatures(resourceConfig);
        resourceConfig.register(MCRRESTFeature.class);
    }
}
