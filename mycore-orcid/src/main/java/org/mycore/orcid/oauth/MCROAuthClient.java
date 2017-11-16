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

package org.mycore.orcid.oauth;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.mycore.common.config.MCRConfiguration;

/**
 * Utility class working as a client for the OAuth2 API of orcid.org.
 * Used to get access tokens. Minimum configuration requires to set
 *
 * MCR.ORCID.OAuth.ClientID
 * MCR.ORCID.OAuth.ClientSecret
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCROAuthClient {

    private static MCROAuthClient SINGLETON = new MCROAuthClient();

    public static MCROAuthClient instance() {
        return SINGLETON;
    }

    private String baseURL;

    private String clientID;

    private String clientSecret;

    private Client client;

    private MCROAuthClient() {
        String prefix = "MCR.ORCID.OAuth.";
        MCRConfiguration config = MCRConfiguration.instance();

        baseURL = config.getString(prefix + "BaseURL");
        clientID = config.getString(prefix + "ClientID");
        clientSecret = config.getString(prefix + "ClientSecret");

        client = ClientBuilder.newClient();
    }

    /**
     * Builds am OAuth2 token request.
     */
    public MCRTokenRequest getTokenRequest() {
        MCRTokenRequest req = new MCRTokenRequest(client.target(baseURL));
        req.set("client_id", clientID);
        req.set("client_secret", clientSecret);
        return req;
    }
}
