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

package org.mycore.orcid2.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Factory for various ORCID clients.
 */
public class MCRORCIDClientFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "Client.";

    private static final String READ_PUBLIC_TOKEN
        = MCRConfiguration2.getString(CONFIG_PREFIX + "ReadPublicToken").orElse(null);

    private static Map<String, MCRORCIDClientFactory> factories = new HashMap<>();

    private final String memberAPI;

    private final String publicAPI;

    private MCRORCIDReadClient publicClient = null;

    private MCRORCIDReadClient memberClient = null;

    private MCRORCIDClientFactory(String version) throws MCRConfigurationException {
        this.publicAPI = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + version + ".PublicAPI");
        this.memberAPI = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + version + ".MemberAPI");
    }

    /**
     * Returns an instance of a factory for a version.
     * 
     * @param version the version
     * @return MCRORCIDClientFactory
     * @throws MCRConfigurationException if factory cannot be initialized
     */
    public static MCRORCIDClientFactory getInstance(String version) throws MCRConfigurationException {
        MCRORCIDClientFactory factory = null;
        if (factories.containsKey(version)) {
            factory = factories.get(version);
        } else {
            factory = new MCRORCIDClientFactory(version);
            factories.put(version, factory);
        }
        return factory;
    }

    /**
     * Creates a MCRORCIDReadClient for ORCID Public API.
     * Public API is limited to 10,000 results
     * 24 requests per second; 40 burst
     * 
     * @return MCRORCIDReadClient
     */
    public MCRORCIDReadClient createPublicClient() {
        if (publicClient == null) {
            if (READ_PUBLIC_TOKEN == null) {
                LOGGER.info("MCR.ORCID2.ReadPublicToken is not set.");
            }
            publicClient = new MCRORCIDReadClientImpl(publicAPI);
        }
        return publicClient;
    }

    /**
     * Creates a MCRORCIDReadClient for ORCID Member API.
     * Member API does not limit the number of results
     * 24 requests per second; 60 burst
     *
     * @throws MCRORCIDException if MCR.ORCID2.Client.ReadPublicToken is not set
     * @return MCRORCIDReadClient
     */
    public MCRORCIDReadClient createMemberClient() throws MCRORCIDException {
        if (memberClient == null) {
            if (READ_PUBLIC_TOKEN == null) {
                throw new MCRORCIDException("MCR.ORCID2.ReadPublicToken is not set.");
            }
            memberClient = new MCRORCIDReadClientImpl(memberAPI, READ_PUBLIC_TOKEN);
        }
        return memberClient;
    }

    /**
     * Creates a MCRORCIDClient for ORCID Member API with MCRORCIDCredentials.
     *
     * @param credentials the MCRORCIDCredentials
     * @return MCRORCIDClient
     */
    public MCRORCIDClient createMemberClient(MCRORCIDCredentials credentials) {
        return new MCRORCIDClientImpl(memberAPI, credentials);
    }
}
