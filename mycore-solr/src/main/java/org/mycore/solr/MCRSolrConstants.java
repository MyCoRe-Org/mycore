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

package org.mycore.solr;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrConstants {

    public static final String SOLR_CONFIG_PREFIX = "MCR.Solr.";

    public static final String SOLR_SERVER_BASE_URL;

    public static final String SOLR_SERVER_URL;

    public static final String SOLR_CORE_MAIN = MCRConfiguration.instance().getString(SOLR_CONFIG_PREFIX + "Core.Main", null);

    public static final String SOLR_QUERY_XML_PROTOCOL_VERSION = MCRConfiguration.instance().getString(
        SOLR_CONFIG_PREFIX + "XMLProtocolVersion");

    public static final String SOLR_QUERY_PATH = MCRConfiguration.instance().getString(SOLR_CONFIG_PREFIX + "SelectPath");

    public static final String SOLR_EXTRACT_PATH = MCRConfiguration.instance().getString(SOLR_CONFIG_PREFIX + "ExtractPath");

    public static final String SOLR_UPDATE_PATH = MCRConfiguration.instance().getString(SOLR_CONFIG_PREFIX + "UpdatePath");

    public static final String SOLR_JOIN_PATTERN = "{!join from=returnId to=id}";

    static {
        String serverURL = MCRConfiguration.instance().getString(SOLR_CONFIG_PREFIX + "ServerURL");
        if (!serverURL.endsWith("/")) {
            serverURL = serverURL += "/";
        }
        SOLR_SERVER_BASE_URL = serverURL;
        serverURL = SOLR_CORE_MAIN != null ? SOLR_SERVER_BASE_URL + SOLR_CORE_MAIN : SOLR_SERVER_BASE_URL;
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        SOLR_SERVER_URL = serverURL;
    }

}
