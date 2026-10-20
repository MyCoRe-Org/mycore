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

package org.mycore.solr.proxy;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.MCRSolrConstants;

/**
 * Modifies the parameters of a user supplied Solr query on the server side, right before it is sent to Solr.
 * <p>
 * Implementations can remove, overwrite or add parameters, e.g. to restrict the result set according to the
 * current user. Since the parameters are changed after all user input has been collected, values set here
 * cannot be overridden by the client.
 *
 * @see MCRCombinedSolrQueryParameterFilter
 */
public interface MCRSolrQueryParameterFilter {

    String FILTER_PROPERTY = MCRSolrConstants.SOLR_CONFIG_PREFIX + "QueryParameterFilter";

    /**
     * Filters the parameters of a Solr query.
     *
     * @param queryHandlerPath the path of the Solr request handler the query is sent to, e.g. <code>/select</code>
     * @param params the query parameters, which may be modified
     */
    void filter(String queryHandlerPath, ModifiableSolrParams params);

    static MCRSolrQueryParameterFilter obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRSolrQueryParameterFilter.class, FILTER_PROPERTY);
    }

}
