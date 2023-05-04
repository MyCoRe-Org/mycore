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

package org.mycore.services.queuedjob.rest;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFilter;
import org.mycore.restapi.MCRCORSResponseFilter;
import org.mycore.restapi.MCRIgnoreClientAbortInterceptor;

import jakarta.ws.rs.ApplicationPath;
import org.mycore.restapi.MCRSessionFilter;
import org.mycore.restapi.MCRTransactionFilter;


/**
 * @author Sebastian Hofmann
 */
@ApplicationPath("/api/jobqueue")
public class MCRJobQueueApp extends ResourceConfig {

    /**
     * Creates the Jersey application for the MyCoRe JobQueue-API
     */
    public MCRJobQueueApp() {
        super();
        initAppName();
        property(ServerProperties.APPLICATION_NAME, getApplicationName());
        packages(getRestPackages());
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        register(MCRSessionFilter.class);
        register(MCRTransactionFilter.class);
        register(MCRJobQueueFeature.class);
        register(MCRCORSResponseFilter.class);
        register(MCRRequestScopeACLFilter.class);
        register(MCRIgnoreClientAbortInterceptor.class);
    }

    /**
     * Sets the application name for the Jersey application
     */
    protected void initAppName() {
        setApplicationName("MyCoRe JobQueue-API " + getVersion());
        LogManager.getLogger().info("Initiialize {}", getApplicationName());
    }

    /**
     * @return the version of the JobQueue-API
     */
    protected String getVersion() {
        return "1.0";
    }

    /**
     * @return the packages to scan for REST resources
     */
    protected String[] getRestPackages() {
        return MCRConfiguration2.getOrThrow("MCR.JobQueue.API.Resource.Packages", MCRConfiguration2::splitValue)
            .toArray(String[]::new);
    }
}
