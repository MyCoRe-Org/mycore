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

package org.mycore.dedup.resources;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.restapi.MCRJerseyRestApp;

import jakarta.ws.rs.ApplicationPath;

/**
 * Standalone JAX-RS application that exposes the deduplication query API under {@code /api/dedup}.
 * The JAX-RS resource packages are configured via {@code MCR.DeDup.API.Resource.Packages}.
 */
@ApplicationPath("/api/dedup")
public class MCRDeDupApp extends MCRJerseyRestApp {

    @Override
    protected void initAppName() {
        setApplicationName("MyCoRe Deduplication-API " + getVersion());
        LogManager.getLogger().info("Initialize {}", this::getApplicationName);
    }

    @Override
    protected String getVersion() {
        return "1.0";
    }

    @Override
    protected String[] getRestPackages() {
        return MCRConfiguration2.getOrThrow("MCR.DeDup.API.Resource.Packages", MCRConfiguration2::splitValue)
            .toArray(String[]::new);
    }
}
