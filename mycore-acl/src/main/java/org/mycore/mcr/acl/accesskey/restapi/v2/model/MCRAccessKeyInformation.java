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

package org.mycore.mcr.acl.accesskey.restapi.v2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;

/**
 * REST Model for {@link MCRAccessKey}.
 */
public class MCRAccessKeyInformation {

    private List<MCRAccessKey> accessKeys;

    private int totalAccessKeyCount;

    /**
     * Creates MCRAccessKeyInformation with List of {@link MCRAccessKey}s and total count.
     * 
     * @param accessKeys List of MCRAccessKey
     * @param totalAccessKeyCount total count
     */
    public MCRAccessKeyInformation(List<MCRAccessKey> accessKeys, int totalAccessKeyCount) {
        setAccessKeys(accessKeys);
        setTotalAccessKeyCount(totalAccessKeyCount);
    }

    /**
     * Sets {@link MCRAccessKey}s.
     * 
     * @param accessKeys List of MCRAccessKey
     */
    public void setAccessKeys(List<MCRAccessKey> accessKeys) {
        this.accessKeys = accessKeys;
    }

    /**
     * Returns List of {@link MCRAccessKey}.
     * 
     * @return List of MCRAccessKey
     */
    @JsonProperty("items")
    public List<MCRAccessKey> getAccessKeys() {
        return this.accessKeys;
    }

    /**
     * Sets total count.
     * 
     * @param totalAccessKeyCount the total count
     */
    public void setTotalAccessKeyCount(int totalAccessKeyCount) {
        this.totalAccessKeyCount = totalAccessKeyCount;
    }

    /**
     * Returns total count.
     * 
     * @return total count
     */
    @JsonProperty("totalResults")
    public int getTotalAccessKeyCount() {
        return this.totalAccessKeyCount;
    }
}
