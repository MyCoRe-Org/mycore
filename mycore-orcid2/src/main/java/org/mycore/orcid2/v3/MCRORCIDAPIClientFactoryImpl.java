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

package org.mycore.orcid2.v3;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.MCRORCIDAPIClientFactory;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.client.MCRORCIDClientImpl;
import org.mycore.orcid2.client.MCRORCIDReadClient;
import org.mycore.orcid2.client.MCRORCIDReadClientImpl;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Provides a factory that creates a specific api client.
 */
public class MCRORCIDAPIClientFactoryImpl implements MCRORCIDAPIClientFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static MCRORCIDAPIClientFactoryImpl instance = null;

    private static final String PUBLIC_API_URL = "https://pub.orcid.org/v3.0";

    private static final String PUBLIC_SANDBOX_API_URL = "https://pub.sandbox.orcid.org/v3.0";

    private static final String MEMBER_API_URL = "https://api.orcid.org/v3.0";

    private static final String MEMBER_SANDBOX_API_URL = "https://api.sandbox.orcid.org/v3.0";

    private static MCRORCIDReadClient publicClient = null;

    private static MCRORCIDReadClient memberClient = null;

    private static final String READ_PUBLIC_TOKEN
        = MCRConfiguration2.getString(MCRORCIDConstants.CONFIG_PREFIX + "v3.ReadPublicToken").orElse(null);

    private static final boolean IS_SANDBOX
        = MCRConfiguration2.getBoolean(MCRORCIDConstants.CONFIG_PREFIX + "v3.IsSandbox").orElse(false);

    private MCRORCIDAPIClientFactoryImpl() {
    }

    /**
     * Returns a MCRORCIDAPIClientFactoryImpl instance.
     *
     * @return MCRORCIDAPIClientFactoryImpl instance
     */
    public static MCRORCIDAPIClientFactoryImpl getInstance() {
        if (instance == null) {
            instance = new MCRORCIDAPIClientFactoryImpl();
        }
        return instance;
    }

    // Public API is limited to 10,000 results
    // 24 requests per second; 40 burst
    @Override
    public MCRORCIDReadClient createPublicClient() {
        if (publicClient == null) {
            if (READ_PUBLIC_TOKEN == null) {
                LOGGER.info("Read public token is not set.");
            }
            if (IS_SANDBOX) {
                publicClient = new MCRORCIDReadClientImpl(PUBLIC_SANDBOX_API_URL, READ_PUBLIC_TOKEN);
            } else {
                publicClient = new MCRORCIDReadClientImpl(PUBLIC_API_URL, READ_PUBLIC_TOKEN);
            }
        }
        return publicClient;
    }

    // Member API does not limit the number of results
    // 24 requests per second; 60 burst
    @Override
    public MCRORCIDReadClient createMemberClient() throws MCRORCIDException {
        if (memberClient == null) {
            if (READ_PUBLIC_TOKEN == null) {
                throw new MCRORCIDException("Read public token is not set.");
            }
            if (IS_SANDBOX) {
                memberClient = new MCRORCIDReadClientImpl(MEMBER_SANDBOX_API_URL, READ_PUBLIC_TOKEN);
            } else {
                memberClient = new MCRORCIDReadClientImpl(MEMBER_API_URL, READ_PUBLIC_TOKEN);
            }
        }
        return memberClient;
    }

    @Override
    public MCRORCIDClient createMemberClient(MCRORCIDCredentials credentials) {
        if (IS_SANDBOX) {
            return new MCRORCIDClientImpl(MEMBER_SANDBOX_API_URL, credentials);
        }
        return new MCRORCIDClientImpl(MEMBER_API_URL, credentials);
    }
}
