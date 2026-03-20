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

package org.mycore.solr;

public class MCRSolrDefaultPropertyConstants {

    public static final String USE_JETTY_HTTP_CLIENT = "MCR.Solr.Default.UseJettyHttpClient";

    public static final String CLIENT_IDLE_TIMEOUT = "MCR.Solr.Default.Client.IdleTimeout";
    public static final String CLIENT_IDLE_TIMEOUT_UNIT = "MCR.Solr.Default.Client.IdleTimeout.Unit";

    public static final String CLIENT_CONNECTION_TIMEOUT = "MCR.Solr.Default.Client.ConnectionTimeout";
    public static final String CLIENT_CONNECTION_TIMEOUT_UNIT = "MCR.Solr.Default.Client.ConnectionTimeout.Unit";

    public static final String CLIENT_REQUEST_TIMEOUT = "MCR.Solr.Default.Client.RequestTimeout";
    public static final String CLIENT_REQUEST_TIMEOUT_UNIT = "MCR.Solr.Default.Client.RequestTimeout.Unit";

    public static final String DEFAULT_SHARD_COUNT = "MCR.Solr.Default.ShardCount";
    public static final String DEFAULT_REPLICA_COUNT = "MCR.Solr.Default.ReplicaCount";

}
