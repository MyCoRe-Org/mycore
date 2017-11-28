/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.mycore.common.config.MCRConfiguration;

/**
 * Utility class to work with the REST API of orcid.org.
 * By setting MCR.ORCID.BaseURL, application can choose to work
 * against the production registry or the sandbox of orcid.org.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDClient {

    private final static MCRORCIDClient SINGLETON = new MCRORCIDClient();

    private WebTarget baseTarget;

    public static MCRORCIDClient instance() {
        return SINGLETON;
    }

    private MCRORCIDClient() {
        String baseURL = MCRConfiguration.instance().getString("MCR.ORCID.BaseURL");
        Client client = ClientBuilder.newClient();
        baseTarget = client.target(baseURL);
    }

    public WebTarget getBaseTarget() {
        return baseTarget;
    }
}
