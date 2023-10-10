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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents model for ORCID flag content.
 */
public class MCRORCIDFlagContent {

    private List<MCRORCIDUserInfo> userInfos = new ArrayList<>();

    /**
     * Initializes new MCRORCIDFlagContent object with List of MCRORCIDUserInfo.
     * 
     * @param userInfos List of MCRORCIDUserInfo
     */
    public MCRORCIDFlagContent(List<MCRORCIDUserInfo> userInfos) {
        this.userInfos = userInfos;
    }

    /**
     * Initializes new MCRORCIDFlagContent object.
     */
    public MCRORCIDFlagContent() {
    }

    /**
     * Returns List of MCRORCIDUserInfo.
     * 
     * @return List of MCRORCIDUserInfo
     */
    public List<MCRORCIDUserInfo> getUserInfos() {
        return userInfos;
    }

    /**
     * Sets MCRORCIDUserInfos.
     * 
     * @param userInfos List of MCRORCIDUserInfo
     */
    public void setUserInfos(List<MCRORCIDUserInfo> userInfos) {
        this.userInfos = userInfos;
    }

    /**
     * Returns MCRORCIDUserInfo by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return MCRORCIDUserInfo or null
     */
    public MCRORCIDUserInfo getUserInfoByORCID(String orcid) {
        return userInfos.stream().filter(c -> c.getORCID().equals(orcid)).findFirst().orElse(null);
    }

    /**
     * Updates MCRORCIDUserInfo by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @param userInfo the MCRORCIDUserInfo
     */
    public void updateUserInfoByORCID(String orcid, MCRORCIDUserInfo userInfo) {
        removeUserInfoByORCID(orcid);
        userInfos.add(userInfo);
    }

    /**
     * Removes MCRORCIDUserInfo by ORCID iD.
     * 
     * @param orcid the ORCID iD
     */
    public void removeUserInfoByORCID(String orcid) {
        userInfos = userInfos.stream().filter(c -> !c.getORCID().equals(orcid)).collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfos);
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
        MCRORCIDFlagContent other = (MCRORCIDFlagContent) obj;
        return Objects.equals(userInfos, other.userInfos);
    }
}
