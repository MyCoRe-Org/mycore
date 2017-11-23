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

package org.mycore.pi.doi.rest;

import java.util.List;

public class MCRDOIRestResponse {
    int responseCode;

    String handle;

    List<MCRDOIRestResponseEntry> values;

    /**
     * 1 : Success. (HTTP 200 OK)
     * 2 : Error. Something unexpected went wrong during handle resolution. (HTTP 500 Internal Server Error)
     * 100 : Handle Not Found. (HTTP 404 Not Found)
     * 200 : Values Not Found. The handle exists but has no values (or no values according to the types and indices specified). (HTTP 200 OK)
     *
     * @return the code
     */
    public int getResponseCode() {
        return responseCode;
    }

    public String getHandle() {
        return handle;
    }

    public List<MCRDOIRestResponseEntry> getValues() {
        return values;
    }
}
