/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 23, 2013 $
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

package org.mycore.solr;

import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrConstants {

    public static final String CONFIG_PREFIX = "MCR.Module-solr.";

    public static final String SERVER_BASE_URL;

    public static final String SERVER_URL;

    public static final String CORE = MCRConfiguration.instance().getString(CONFIG_PREFIX + "Core", null);

    public static final String QUERY_XML_PROTOCOL_VERSION = MCRConfiguration.instance().getString(
        CONFIG_PREFIX + "XMLProtocolVersion");

    public static final String QUERY_PATH = MCRConfiguration.instance().getString(CONFIG_PREFIX + "SelectPath");

    public static final String EXTRACT_PATH = MCRConfiguration.instance().getString(CONFIG_PREFIX + "ExtractPath");

    public static final String UPDATE_PATH = MCRConfiguration.instance().getString(CONFIG_PREFIX + "UpdatePath");

    public static final String JOIN_PATTERN = "{!join from=returnId to=id}";

    static {
        String serverURL = MCRConfiguration.instance().getString(CONFIG_PREFIX + "ServerURL");
        if (!serverURL.endsWith("/")) {
            serverURL = serverURL += "/";
        }
        SERVER_BASE_URL = serverURL;
        serverURL = CORE != null ? SERVER_BASE_URL + CORE : SERVER_BASE_URL;
        if (serverURL.endsWith("/")) {
            serverURL = serverURL.substring(0, serverURL.length() - 1);
        }
        SERVER_URL = serverURL;
    }

}
