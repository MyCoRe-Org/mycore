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

/**
 * Interface for ORCID API Definition.
 */
public interface MCRORCIDAPIDefinition {

    /**
     * Returns ORCID Member API URL.
     * 
     * @return ORCID Member API URL
     */
    String getMemberURL();

    /**
     * Returns ORCID Member Sandbox API URL.
     * 
     * @return ORCID Member Sandbox API URL
     */
    String getMemberSandboxURL();

    /**
     * Returns ORCID Public API URL.
     * 
     * @return ORCID Public API URL
     */
    String getPublicURL();

    /**
     * Returns ORCID Public API Sandbox URL.
     * 
     * @return ORCID Public API Sanbox URL
     */
    String getPublicSandboxURL();
}
