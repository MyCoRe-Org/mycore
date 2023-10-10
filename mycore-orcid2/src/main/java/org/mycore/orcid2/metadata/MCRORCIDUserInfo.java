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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents model for an ORCID user info.
 */
public class MCRORCIDUserInfo {

    private String orcid;

    private MCRORCIDPutCodeInfo workInfo;

    /**
     * Initialises new MCRORCIDUserInfo with ORCID iD.
     * 
     * @param orcid the ORCID iD
     */
    public MCRORCIDUserInfo(String orcid) {
        this.orcid = orcid;
    }

    /**
     * Initialises new MCRORCIDUserInfo.
     */
    public MCRORCIDUserInfo() {
    }

    /**
     * Returns the ORCID iD.
     * 
     * @return the ORCID iD
     */
    @JsonProperty("orcid")
    public String getORCID() {
        return orcid;
    }

    /**
     * Sets ORCID iD.
     * 
     * @param orcid the ORCID iD
     */
    public void setORCID(String orcid) {
        this.orcid = orcid;
    }

    /**
     * Returns MCRORCIDPutCodeInfo for works.
     * 
     * @return MCRORCIDPutCodeInfo for works
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("works")
    public MCRORCIDPutCodeInfo getWorkInfo() {
        return workInfo;
    }

    /**
     * Sets works MCRORCIDPutCodeInfo.
     * 
     * @param workInfo the work info
     */
    public void setWorkInfo(MCRORCIDPutCodeInfo workInfo) {
        this.workInfo = workInfo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(orcid, workInfo);
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
        MCRORCIDUserInfo other = (MCRORCIDUserInfo) obj;
        return Objects.equals(orcid, other.orcid) && Objects.equals(workInfo, other.workInfo);
    }
}
