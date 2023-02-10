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

import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Interface for ORCID client factory which contains which all relevant options.
 */
public interface MCRORCIDAPIClientFactory {

    /**
     * Creates MCRORCIDReadClient.
     *
     * @return MCRORCIDReadClient
     */
    MCRORCIDReadClient createPublicClient();

    /**
     * Creates MCRORCIDReadClient.
     * 
     * @return MCRORCIDReadClient
     * @throws MCRORCIDException if there is no token
     */
    MCRORCIDReadClient createMemberClient() throws MCRORCIDException;

    /**
     * Creates an MCRORCIDClient with MCRORCIDCredentials.
     *
     * @param credentials the MCRORCIDCredentials
     * @return MCRORCIDClient
     */
    MCRORCIDClient createMemberClient(MCRORCIDCredentials credentials);
}
