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

package org.mycore.orcid;

import org.mycore.orcid.works.MCRWorks;

/**
 * Represents the profile of a given ORCID ID.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDProfile {

    private String orcid;

    private MCRWorks works = new MCRWorks(this);

    public MCRORCIDProfile(String orcid) {
        this.orcid = orcid;
    }

    public String getORCID() {
        return orcid;
    }

    /**
     * Returns a representation of the "works" section within the ORCID profile,
     * which contains the publications stored there
     */
    public MCRWorks getWorks() {
        return works;
    }
}
