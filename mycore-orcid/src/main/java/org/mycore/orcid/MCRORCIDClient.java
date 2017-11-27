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

package org.mycore.orcid;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.orcid.oauth.MCRReadPublicTokenFactory;

/**
 * Utility class working as a client for the REST API of orcid.org.
 * By setting MCR.ORCID.BaseURL, application can choose to work
 * against the production registry or the sandbox of orcid.org.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDClient {

    private static final Logger LOGGER = LogManager.getLogger(MCRORCIDProfile.class);

    public static final MediaType ORCID_XML_MEDIA_TYPE = MediaType.valueOf("application/vnd.orcid+xml");

    private static final MCRORCIDClient SINGLETON = new MCRORCIDClient();

    private WebTarget baseTarget;

    public static MCRORCIDClient instance() {
        return SINGLETON;
    }

    private MCRORCIDClient() {
        String baseURL = MCRConfiguration.instance().getString("MCR.ORCID.BaseURL");
        Client client = ClientBuilder.newClient();
        baseTarget = client.target(baseURL);
    }

    /**
     * Does a GET request against the REST API of orcid.org and returns the response as MCRContent.
     *
     * @param path the path of the resource to get, relative to the API's base URL.
     */
    public MCRContent get(String path) {
        WebTarget target = baseTarget.path(path);
        LOGGER.info("get {}", target.getUri());

        Builder b = target.request().accept(ORCID_XML_MEDIA_TYPE);
        b = b.header("Authorization", "Bearer " + MCRReadPublicTokenFactory.getToken());

        InputStream in = b.get(InputStream.class);
        return new MCRStreamContent(in);
    }
}
