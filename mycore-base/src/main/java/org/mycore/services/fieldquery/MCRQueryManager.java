/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import org.mycore.common.config.MCRConfiguration;

/**
 * Executes queries on all configured searchers and returns query results.
 * 
 * @author Frank Lützenkirchen, Huu Chi Vu
 */
public class MCRQueryManager {
    private static MCRQueryEngine queryEngine = (MCRQueryEngine) MCRConfiguration.instance().getInstanceOf("MCR.Query.Engine",
            "org.mycore.services.fieldquery.MCRDefaultQueryEngine");

    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * 
     * @return the query results
     */
    public static MCRResults search(MCRQuery query) {
        return queryEngine.search(query, false);
    }

    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * @param comesFromRemoteHost
     *            if true, this query is originated from a remote host, so no
     *            sorting of results is done for performance reasons
     * 
     * @return the query results
     */
    public static MCRResults search(MCRQuery query, boolean comesFromRemoteHost) {
        return queryEngine.search(query, comesFromRemoteHost);
    }
}
