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

package org.mycore.viewer.alto.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

@MappedSuperclass
public class MCRStoredChangeSet extends MCRStoredAltoChangeSetMetadata {

    @Transient
    @JsonInclude()
    private MCRAltoChangeSet changeSet;

    public MCRStoredChangeSet() {
    }

    public MCRStoredChangeSet(String sessionID, String derivateID, String objectTitle, Date created,
        Date applied, String user) {
        super(sessionID, derivateID, objectTitle, created, applied, user);
    }

    public MCRAltoChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(MCRAltoChangeSet changeSet) {
        this.changeSet = changeSet;
    }
}
