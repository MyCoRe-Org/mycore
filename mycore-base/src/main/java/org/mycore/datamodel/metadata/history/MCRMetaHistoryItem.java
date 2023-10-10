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

package org.mycore.datamodel.metadata.history;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.Basic;
import org.mycore.backend.jpa.MCRObjectIDConverter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Entity implementation class for Entity: MCRMetaHistoryItem
 *
 */
@Entity
@Table(name = "MCRMetaHistory",
    indexes = {
        @Index(name = "IDX_ID_TIME", columnList = "id,time"),
        @Index(name = "IDX_TIME", columnList = "time")
    })
@NamedQueries({
    @NamedQuery(name = "MCRMetaHistory.getLastOfType",
        query = "SELECT MAX(time) FROM MCRMetaHistoryItem i WHERE i.id=:id and i.eventTypeChar=:type"),
    @NamedQuery(name = "MCRMetaHistory.getLastEventByID",
        query = "SELECT a FROM MCRMetaHistoryItem a "
            + "WHERE a.time BETWEEN :from AND :until "
            + "AND a.eventTypeChar=:eventType "
            + "ORDER BY a.time DESC "
            + "FETCH FIRST 1 ROWS ONLY"),
    @NamedQuery(name = "MCRMetaHistory.getFirstDate", query = "SELECT MIN(time) from MCRMetaHistoryItem"),
    @NamedQuery(name = "MCRMetaHistory.getHighestID",
        query = "SELECT MAX(history.id) from MCRMetaHistoryItem history"
            + " WHERE CAST(history.id as string) like :looksLike escape '\\'"),
    @NamedQuery(name = "MCRMetaHistory.getNextActiveIDs",
        query = "SELECT c"
            + " FROM MCRMetaHistoryItem c"
            + " WHERE (:afterID is null or c.id >:afterID)"
            + "   AND c.eventTypeChar='Created'"
            + "   AND (:kind!='object' OR CAST(c.id as string) NOT LIKE '%\\_derivate\\_%')"
            + "   AND (:kind!='derivate' OR CAST(c.id as string) LIKE '%\\_derivate\\_%')"
            + "   AND (NOT EXISTS (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventTypeChar ='d' AND c.id=d.id)"
            + "        OR c.time > ALL (SELECT d.time FROM MCRMetaHistoryItem d "
            + "                         WHERE d.eventTypeChar ='d' AND c.id=d.id))"
            + " ORDER by c.id"),
    @NamedQuery(name = "MCRMetaHistory.countActiveIDs",
        query = "SELECT count(c)"
            + " FROM MCRMetaHistoryItem c"
            + " WHERE c.eventTypeChar='Created'"
            + "   AND (:kind!='object' OR CAST(c.id as string) NOT LIKE '%\\_derivate\\_%')"
            + "   AND (:kind!='derivate' OR CAST(c.id as string) LIKE '%\\_derivate\\_%')"
            + "   AND (NOT EXISTS (SELECT d.time FROM MCRMetaHistoryItem d WHERE d.eventTypeChar ='d' AND c.id=d.id)"
            + "        OR c.time > ALL (SELECT d.time FROM MCRMetaHistoryItem d "
            + "                         WHERE d.eventTypeChar ='d' AND c.id=d.id))")
})
public class MCRMetaHistoryItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long internalid;

    @Column(length = MCRObjectID.MAX_LENGTH)
    @Convert(converter = MCRObjectIDConverter.class)
    @Basic
    private MCRObjectID id;

    private Instant time;

    @Column(length = 1, name = "eventType")
    private char eventTypeChar;

    private String userID;

    private String userIP;

    private static final long serialVersionUID = 1L;

    static MCRMetaHistoryItem createdNow(MCRObjectID id) {
        return now(id, MCRMetadataHistoryEventType.Create);
    }

    static MCRMetaHistoryItem deletedNow(MCRObjectID id) {
        return now(id, MCRMetadataHistoryEventType.Delete);
    }

    static MCRMetaHistoryItem now(MCRObjectID id, MCRMetadataHistoryEventType type) {
        MCRMetaHistoryItem historyItem = new MCRMetaHistoryItem();
        historyItem.setId(id);
        historyItem.setTime(Instant.now());
        historyItem.setEventType(type);
        if (MCRSessionMgr.hasCurrentSession()) {
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            historyItem.setUserID(currentSession.getUserInformation().getUserID());
            historyItem.setUserIP(currentSession.getCurrentIP());
        }
        return historyItem;
    }

    public long getInternalid() {
        return this.internalid;
    }

    public void setInternalid(long internalid) {
        this.internalid = internalid;
    }

    public MCRObjectID getId() {
        return this.id;
    }

    public void setId(MCRObjectID id) {
        this.id = id;
    }

    public Instant getTime() {
        return this.time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public MCRMetadataHistoryEventType getEventType() {
        return MCRMetadataHistoryEventType.fromAbbr(getEventTypeChar());
    }

    public void setEventType(MCRMetadataHistoryEventType eventType) {
        setEventTypeChar(eventType.getAbbr());
    }

    public String getUserID() {
        return this.userID;
    }

    public void setUserID(String userId) {
        this.userID = userId;
    }

    public String getUserIP() {
        return this.userIP;
    }

    public void setUserIP(String ip) {
        this.userIP = ip;
    }

    public char getEventTypeChar() {
        return eventTypeChar;
    }

    public void setEventTypeChar(char eventTypeChar) {
        this.eventTypeChar = eventTypeChar;
    }

    @Override
    public String toString() {
        return "MCRMetaHistoryItem [eventType=" + getEventType() + ", id=" + id + ", time=" + time + ", userID="
            + userID + ", userIP=" + userIP + "]";
    }

}
