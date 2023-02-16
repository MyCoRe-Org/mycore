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

package org.mycore.orcid2.metadata;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents model for own and other ORCID put codes.
 */
public class MCRORCIDPutCodeInfo {

    private long ownPutCode;

    private long[] otherPutCodes;

    /**
     * Initialises a new MCRORCIDPutCodeInfo with own and other put codes.
     * 
     * @param ownPutCode own put code
     * @param otherPutCodes List of other put codes
     */
    public MCRORCIDPutCodeInfo(long ownPutCode, long[] otherPutCodes) {
        this.ownPutCode = ownPutCode;
        this.otherPutCodes = otherPutCodes;
    }

    /**
     * Initialises a new MCRORCIDPutCodeInfo with own put code.
     * 
     * @param ownPutCode own put code
     */
    public MCRORCIDPutCodeInfo(long ownPutCode) {
        this.ownPutCode = ownPutCode;
    }

    /**
     * Initialises a new MCRORCIDPutCodeInfo.
     */
    public MCRORCIDPutCodeInfo() {
    }

    /**
     * Return own put code.
     * 
     * @return own put code
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonProperty("own")
    public long getOwnPutCode() {
        return ownPutCode;
    }

    /**
     * Sets own put code.
     * 
     * @param putCode own put code
     */
    public void setOwnPutCode(long putCode) {
        ownPutCode = putCode;
    }

    /**
     * Returns other put codes.
     * 
     * @return other put codes
     */
    @JsonProperty("other")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public long[] getOtherPutCodes() {
        return otherPutCodes;
    }

    /**
     * Sets other put codes.
     *
     * @param putCodes other put codes
     */
    public void setOtherPutCodes(long[] putCodes) {
        this.otherPutCodes = putCodes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownPutCode, otherPutCodes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRORCIDPutCodeInfo other = (MCRORCIDPutCodeInfo) obj;
        return Objects.equals(ownPutCode, other.ownPutCode) && Arrays.equals(otherPutCodes, other.otherPutCodes);
    }
}
