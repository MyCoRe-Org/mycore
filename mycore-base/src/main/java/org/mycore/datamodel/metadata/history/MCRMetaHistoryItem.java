package org.mycore.datamodel.metadata.history;

import java.io.Serializable;
import java.time.Instant;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.mycore.backend.jpa.MCRObjectIDConverter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Entity implementation class for Entity: MCRMetaHistoryItem
 *
 */
@Entity
@Table(name = "MCRMetaHistory", indexes = {
    @Index(name = "IDX_ID_TIME", columnList = "id,time"),
    @Index(name = "IDX_TIME", columnList = "time")
})
@NamedQueries({
    @NamedQuery(name = "MCRMetaHistory.getLastOfType",
        query = "SELECT MAX(time) FROM MCRMetaHistoryItem i WHERE i.id=:id and i.eventType=:type"),
    @NamedQuery(name = "MCRMetaHistory.getLastEventByID", query = "SELECT a FROM MCRMetaHistoryItem a "
        + "WHERE a.time in (SELECT max(time) as time FROM MCRMetaHistoryItem b "
        + "WHERE a.id=b.id AND time BETWEEN :from AND :until) "
        + "AND a.eventType=:eventType"),
    @NamedQuery(name = "MCRMetaHistory.getFirstDate", query = "SELECT MIN(time) from MCRMetaHistoryItem"),
    @NamedQuery(name = "MCRMetaHistory.getHighestID",
        query = "SELECT MAX(id) from MCRMetaHistoryItem WHERE ID like :looksLike")
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

    @Convert(converter = MCRMetadataHistoryEventTypeConverter.class)
    @Column(length = 1)
    private MCRMetadataHistoryEventType eventType;

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
        return this.eventType;
    }

    public void setEventType(MCRMetadataHistoryEventType eventType) {
        this.eventType = eventType;
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

    @Override
    public String toString() {
        return "MCRMetaHistoryItem [eventType=" + eventType + ", id=" + id + ", time=" + time + ", userID=" + userID
            + ", userIP=" + userIP + "]";
    }

}
