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

package org.mycore.restapi;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.MCRJerseyDefaultConfiguration;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFactory;
import org.mycore.frontend.jersey.resources.MCRJerseyExceptionMapper;
import org.mycore.restapi.v1.errors.MCRForbiddenExceptionMapper;
import org.mycore.restapi.v1.errors.MCRNotAuthorizedExceptionMapper;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;

public abstract class MCRJerseyRestApp extends ResourceConfig {

    protected MCRJerseyRestApp() {
        super();
        setApplicationName(MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe") + " REST-API "
            + getVersion());
        LogManager.getLogger().info("Initiialize {}", getApplicationName());
        property(ServerProperties.APPLICATION_NAME, getApplicationName());
        MCRJerseyDefaultConfiguration.setupGuiceBridge(this);
        String[] restPackages = getRestPackages();
        packages(restPackages);
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        register(MCRSessionFilter.class);
        register(MCRTransactionFilter.class);
        register(MultiPartFeature.class);
        register(MCRRestFeature.class);
        register(MCRJerseyExceptionMapper.class);
        register(MCRRestAPIExceptionMapper.class);
        register(MCRForbiddenExceptionMapper.class);
        register(MCRNotAuthorizedExceptionMapper.class);
        register(MCRCORSResponseFilter.class);
        register(MCRRequestScopeACLFactory.getBinder());
        register(MCRIgnoreClientAbortInterceptor.class);
    }

    protected abstract String getVersion();

    protected abstract String[] getRestPackages();

}
