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

package org.mycore.viewer.alto.model;

import java.io.IOException;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.mycore.common.MCRException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity(name = MCRDBStoredChangeSet.ENTITY_NAME)
@Table(name = MCRDBStoredChangeSet.ENTITY_NAME)
@NamedQueries({
    @NamedQuery(name = "Count.ALTOCS.Unapplied",
        query = "select count(u) from " + MCRDBStoredChangeSet.ENTITY_NAME + " u "
            + "where u.applied IS NULL"),
    @NamedQuery(name = "Get.ALTOCS.ByPID",
        query = "select u from " + MCRDBStoredChangeSet.ENTITY_NAME + " u "
            + "where u.pid = :pid "
            + "and u.applied IS NULL"),
    @NamedQuery(name = "Get.ALTOCS.Unapplied",
        query = "select u from " + MCRDBStoredChangeSet.ENTITY_NAME + " u where u.applied IS NULL"),
    @NamedQuery(name = "Get.ALTOCS.Unapplied.bySID",
        query = "select u from " + MCRDBStoredChangeSet.ENTITY_NAME
            + " u where u.sessionID = :sid and u.applied IS NULL"),
    @NamedQuery(name = "Get.ALTOCS.Unapplied.byDerivate",
        query = "select u from " + MCRDBStoredChangeSet.ENTITY_NAME
            + " u where u.derivateID = :derivateID and u.applied IS NULL"),
    @NamedQuery(name = "Delete.ALTOCS.byPID",
        query = "delete from " + MCRDBStoredChangeSet.ENTITY_NAME + " u where u.pid = :pid") })
public class MCRDBStoredChangeSet extends MCRStoredChangeSet {

    protected static final String ENTITY_NAME = "MCRAltoChangeStore";

    protected static final int MB = 1024 * 1024 * 1024;

    @Column(nullable = false, length = MB)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String altoChangeSet;

    public MCRDBStoredChangeSet() {
    }

    public MCRDBStoredChangeSet(String sessionID, String derivateID, String objectTitle, Date created,
        Date applied, String user, MCRAltoChangeSet altoChangeSet) {
        super(sessionID, derivateID, objectTitle, created, applied, user);
        setChangeSet(altoChangeSet);
    }

    public String getAltoChangeSet() {
        return altoChangeSet;
    }

    public void setAltoChangeSet(String altoChangeSet) {
        super.setChangeSet(stringToChangeSet(altoChangeSet));
        this.altoChangeSet = altoChangeSet;
    }

    @Override
    public MCRAltoChangeSet getChangeSet() {
        return stringToChangeSet(this.altoChangeSet);
    }

    @Override
    public void setChangeSet(MCRAltoChangeSet changeSet) {
        super.setChangeSet(changeSet);
        setAltoChangeSet(changeSetToString(changeSet));
    }

    private MCRAltoChangeSet stringToChangeSet(String changeSet) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(changeSet, MCRAltoChangeSet.class);
        } catch (IOException e) {
            throw new MCRException("Could not create changeSet from json string", e);
        }
    }

    private String changeSetToString(MCRAltoChangeSet changeSet) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(changeSet);
        } catch (JsonProcessingException e) {
            throw new MCRException("Could not create json from changeSet object", e);
        }
    }
}
