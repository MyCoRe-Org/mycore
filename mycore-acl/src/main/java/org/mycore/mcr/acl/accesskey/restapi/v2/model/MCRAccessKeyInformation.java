/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey.restapi.v2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.mycore.mcr.acl.accesskey.backend.MCRAccessKey;

public class MCRAccessKeyInformation {

    private List<MCRAccessKey> accessKeys;

    private int totalAccessKeyCount;

    private MCRAccessKeyInformation() {

    }

    public MCRAccessKeyInformation(List<MCRAccessKey> accessKeys, int totalAccessKeyCount) {
        setAccessKeys(accessKeys);
        setTotalAccessKeyCount(totalAccessKeyCount);
    }

    public void setAccessKeys(List<MCRAccessKey> accessKeys) {
        this.accessKeys = accessKeys;
    }

    @JsonProperty("items")
    public List<MCRAccessKey> getAccessKeys() {
        return this.accessKeys;
    }

    public void setTotalAccessKeyCount(int totalAccessKeyCount) {
        this.totalAccessKeyCount = totalAccessKeyCount;
    }

    @JsonProperty("totalResults")
    public int getTotalAccessKeyCount() {
        return this.totalAccessKeyCount;
    }
}
