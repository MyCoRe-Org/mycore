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
 * Interface for ORCID client which bound to specific MCRCredentials.
 */
public interface MCRORCIDUserClient {

    /**
     * Fetches section/object and wraps response into type.
     * 
     * @param section the ORCID section
     * @param <T> the result class
     * @param type the result class
     * @param putCodes optional put code(s)
     * @return transformed section/object
     * @throws MCRORCIDRequestException if request fails
     */
    <T> T fetch(MCRORCIDSection section, Class<T> type, long... putCodes) throws MCRORCIDRequestException;

    /**
     * Creates object in section.
     * 
     * @param section the ORCID section
     * @param object the element
     * @return put code of created object
     * @throws MCRORCIDRequestException if request fails
     */
    long create(MCRORCIDSection section, Object object) throws MCRORCIDRequestException;

    /**
     * Updates object in section by put code.
     * 
     * @param section the ORCID section
     * @param putCode the put code
     * @param object the object
     * @throws MCRORCIDRequestException if request fails
     */
    void update(MCRORCIDSection section, long putCode, Object object) throws MCRORCIDRequestException;

    /**
     * Deletes object in section by put code.
     * 
     * @param section the ORCID section
     * @param putCode the put code
     * @throws MCRORCIDRequestException if request fails
     */
    void delete(MCRORCIDSection section, long putCode) throws MCRORCIDRequestException;
}
