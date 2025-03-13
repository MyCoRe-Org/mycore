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

package org.mycore.solr.auth;

import org.apache.solr.client.solrj.SolrRequest;

import java.net.http.HttpRequest;

/**
 * Authentication implementation that doesn't add any authentication to Solr requests.
 */
public class MCRSolrNoOpAuthenticator implements MCRSolrAuthenticator {

    @Override
    public final void applyAuthentication(SolrRequest<?> request) {
    }

    @Override
    public final void applyAuthentication(HttpRequest.Builder request) {
    }

}
