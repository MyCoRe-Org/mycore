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

package org.mycore.orcid;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Utility class to work with the REST API of orcid.org.
 * By setting MCR.ORCID.BaseURL, application can choose to work
 * against the production registry or the sandbox of orcid.org.
 *
 * @author Frank Lützenkirchen
 */
public final class MCRORCIDClient {

    private final WebTarget baseTarget;

    private MCRORCIDClient() {
        String baseURL = MCRConfiguration2.getStringOrThrow("MCR.ORCID.BaseURL");
        Client client = ClientBuilder.newClient();
        baseTarget = client.target(baseURL);
    }

    /**
     * @deprecated Use {@link #getInstance()} instead
     */
    @Deprecated
    public static MCRORCIDClient instance() {
        return getInstance();
    }

    public static MCRORCIDClient getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    public WebTarget getBaseTarget() {
        return baseTarget;
    }

    private static final class LazyInstanceHolder {
        public static final MCRORCIDClient SINGLETON_INSTANCE = new MCRORCIDClient();
    }

}
