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

package org.mycore.solr.auth;

import java.net.http.HttpRequest;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrRequest;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrConstants;

/**
 * Factory for creating {@link MCRSolrAuthentication} instances based on configuration.
 * The factory uses the configuration property {@code Server.Auth.<level>.Class} to determine the implementation
 */
public class MCRSolrPropertyAuthenticationManager implements MCRSolrAuthenticationManager {

    private static final String SOLR_AUTH_PROPERTY_PREFIX = MCRSolrConstants.SOLR_CONFIG_PREFIX + "Server.Auth.";

    protected Optional<MCRSolrAuthentication> loadImplementation(MCRSolrAuthenticationLevel level) {
        Optional<MCRSolrAuthentication> instance = MCRConfiguration2.getInstanceOf(MCRSolrAuthentication.class,
            SOLR_AUTH_PROPERTY_PREFIX + level.getPropertyName() + ".Class");
        instance.ifPresent(i -> i.setLevel(level));
        return instance;
    }

    public void applyAuthentication(SolrRequest<?> request, MCRSolrAuthenticationLevel level) {
        Optional<MCRSolrAuthentication> optImpl = loadImplementation(level);
        optImpl.ifPresent(instance -> {
            instance.applyAuthentication(request);
        });
    }

    public void applyAuthentication(HttpRequest.Builder request, MCRSolrAuthenticationLevel level) {
        Optional<MCRSolrAuthentication> optImpl = loadImplementation(level);
        optImpl.ifPresent(instance -> {
            instance.applyAuthentication(request);
        });
    }

}
