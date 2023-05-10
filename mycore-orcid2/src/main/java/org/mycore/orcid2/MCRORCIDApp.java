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

package org.mycore.orcid2;

import jakarta.ws.rs.ApplicationPath;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.restapi.MCRDropSessionFilter;
import org.mycore.restapi.MCRJerseyRestApp;
import org.mycore.restapi.v1.MCRRestAPIAuthentication;
import org.mycore.restapi.v2.MCRExceptionMapper;

/**
 * ORCID API REST app.
 */
@ApplicationPath("/api/orcid")
public class MCRORCIDApp extends MCRJerseyRestApp {

    public MCRORCIDApp() {
        super();
        register(MCRRestAPIAuthentication.class);
        register(MCRDropSessionFilter.class);
        register(MCRExceptionMapper.class);
    }

    @Override
    protected void initAppName() {
        setApplicationName("MyCoRe ORCID-API " + getVersion());
        LogManager.getLogger().info("Initiialize {}", getApplicationName());
    }

    @Override
    protected String getVersion() {
        return "1.0";
    }

    @Override
    protected String[] getRestPackages() {
        return MCRConfiguration2.getOrThrow("MCR.ORCID2.API.Resource.Packages", MCRConfiguration2::splitValue)
            .toArray(String[]::new);
    }
}
