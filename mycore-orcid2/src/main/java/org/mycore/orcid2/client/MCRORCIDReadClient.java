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

import org.mycore.orcid2.client.exception.MCRORCIDRequestException;

/**
 * Interface for client that is at least compatible with ORCID Public API.
 */
public interface MCRORCIDReadClient {

    /**
     * Fetches section/object of orcid profile and wraps response into type.
     * 
     * @param orcid the orcid
     * @param section the ORCID section
     * @param <T> the result class
     * @param valueType the result class
     * @param putCodes optional put code(s)
     * @return transformed section/object
     * @throws MCRORCIDRequestException if request fails
     */
    <T> T fetch(String orcid, MCRORCIDSection section, Class<T> valueType, long... putCodes)
        throws MCRORCIDRequestException;
}
