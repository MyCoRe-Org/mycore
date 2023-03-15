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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.exception.MCRORCIDException;

/**
 * Factory for various ORCID clients.
 */
public class MCRORCIDClientFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "Client.";

    private static final String READ_PUBLIC_TOKEN
        = MCRConfiguration2.getString(CONFIG_PREFIX + "ReadPublicToken").orElse(null);

    private static Map<String, MCRORCIDClientFactory> factories = new HashMap<>();

    private final String publicAPI;

    private final String memberAPI;

    private final ReadClientMode mode;

    private MCRORCIDReadClient readClient = null;

    private MCRORCIDClientFactory(String version) throws MCRConfigurationException {
        final String prefix = CONFIG_PREFIX + version;
        publicAPI = MCRConfiguration2.getStringOrThrow(prefix + ".PublicAPI");
        memberAPI = MCRConfiguration2.getStringOrThrow(prefix + ".MemberAPI");
        final String modeString = MCRConfiguration2.getStringOrThrow(prefix + ".APIMode");
        try {
            mode = ReadClientMode.valueOf(modeString.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new MCRConfigurationException("Unknown APIMode: " + modeString);
        }
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
     * Creates a MCRORCIDReadClient for ORCID Member API.
     * Member API does not limit the number of results
     * 24 requests per second; 60 burst
     * Public API is limited to 10,000 results
     * 24 requests per second; 40 burst
     *
     * @throws MCRORCIDException if MCR.ORCID2.Client.ReadPublicToken is required but not set
     * @return MCRORCIDReadClient
     */
    public MCRORCIDReadClient createReadClient() throws MCRORCIDException {
        if (readClient == null) {
            readClient = initReadClient();
        }
        return readClient;
    }

    /**
     * Creates a MCRORCIDUserClient for user with MCRORCIDCredential.
     *
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @return MCRORCIDClient
     * @throws MCRORCIDException if client is not in member mode
     */
    public MCRORCIDUserClient createUserClient(String orcid, MCRORCIDCredential credential)
        throws MCRORCIDException {
        if (checkMemberMode()) {
            return new MCRORCIDUserClientImpl(memberAPI, orcid, credential);
        }
        throw new MCRORCIDException("Client is not in member mode");
    }

    /**
     * Checks if api is in member mode.
     * 
     * @return true if member mode is enabled
     */
    public boolean checkMemberMode() {
        return Objects.equals(mode, ReadClientMode.MEMBER);
    }

    private MCRORCIDReadClient initReadClient() throws MCRORCIDException {
        if (checkMemberMode()) {
            if (READ_PUBLIC_TOKEN == null) {
                throw new MCRORCIDException("MCR.ORCID2.ReadPublicToken is not set");
            }
            return new MCRORCIDReadClientImpl(memberAPI, READ_PUBLIC_TOKEN);
        } else {
            if (READ_PUBLIC_TOKEN == null) {
                LOGGER.info("MCR.ORCID2.ReadPublicToken is not set.");
            }
            return new MCRORCIDReadClientImpl(publicAPI, READ_PUBLIC_TOKEN);
        }
    }

    enum ReadClientMode {
        PUBLIC, MEMBER
    } 
}
