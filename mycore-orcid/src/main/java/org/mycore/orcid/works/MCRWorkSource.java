/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid.works;

import java.util.HashMap;
import java.util.Map;

import org.mycore.orcid.oauth.MCROAuthClient;

/**
 * Represents the source application that generated the work entry in the ORCID profile.
 * This may be another ORCID, another client application or THIS, our MyCoRe application.
 *
 * @author Frank L\u00FCtzenkirchen *
 */
public class MCRWorkSource {

    private static Map<String, MCRWorkSource> sources = new HashMap<String, MCRWorkSource>();

    private String sourceID;

    static MCRWorkSource getInstance(String sourceID) {
        MCRWorkSource source = sources.get(sourceID);
        if (source == null) {
            source = new MCRWorkSource(sourceID);
            sources.put(sourceID, source);
        }
        return source;
    }

    private MCRWorkSource(String sourceID) {
        this.sourceID = sourceID;
    }

    public String getID() {
        return sourceID;
    }

    /**
     * Returns true, if this is our client application, that means
     * the source's ID is our MCR.ORCID.OAuth.ClientID
     *
     * @return
     */
    public boolean isThisApplication() {
        return MCROAuthClient.instance().getClientID().equals(sourceID);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MCRWorkSource) && sourceID.equals(((MCRWorkSource) obj).sourceID);
    }

    @Override
    public int hashCode() {
        return sourceID.hashCode();
    }
}
