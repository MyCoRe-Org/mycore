/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.orcid.works;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.orcid.oauth.MCROAuthClient;

/**
 * Represents the source application that generated the work entry in the ORCID profile.
 * This may be another ORCID, another client application or THIS, our MyCoRe application.
 *
 * @author Frank Lützenkirchen *
 */
public final class MCRWorkSource {

    private static final Map<String, MCRWorkSource> SOURCES = new ConcurrentHashMap<>();

    private final String sourceID;

    static MCRWorkSource obtainInstance(String sourceID) {
        return SOURCES.computeIfAbsent(sourceID, MCRWorkSource::new);
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
     */
    public boolean isThisApplication() {
        return MCROAuthClient.obtainInstance().getClientID().equals(sourceID);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MCRWorkSource workSource) && sourceID.equals(workSource.sourceID);
    }

    @Override
    public int hashCode() {
        return sourceID.hashCode();
    }
}
