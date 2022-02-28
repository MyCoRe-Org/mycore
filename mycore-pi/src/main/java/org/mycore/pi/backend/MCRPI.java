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

package org.mycore.pi.backend;

import java.util.Date;

import org.mycore.common.MCRCoreVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@NamedQueries({
    @NamedQuery(name = "Count.PI.Created",
        query = "select count(u) from MCRPI u "
            + "where u.mycoreID = :mcrId "
            + "and u.type = :type "
            + "and (u.additional = :additional OR (u.additional IS NULL AND :additional IS NULL))"
            + "and u.service = :service"),
    @NamedQuery(name = "Count.PI.Registered",
        query = "select count(u) from MCRPI u "
            + "where u.mycoreID = :mcrId "
            + "and u.type = :type "
            + "and (u.additional = :additional OR (u.additional IS NULL AND :additional IS NULL))"
            + "and u.service = :service "
            + "and u.registered is not null"),
    @NamedQuery(name = "Count.PI.RegistrationStarted",
        query = "select count(u) from MCRPI u "
            + "where u.mycoreID = :mcrId "
            + "and u.type = :type "
            + "and u.additional = :additional "
            + "and u.service = :service "
            + "and u.registrationStarted is not null"),
    @NamedQuery(name = "Get.PI.Created",
        query = "select u from MCRPI u "
            + "where u.mycoreID = :mcrId "
            + "and u.type = :type "
            + "and (u.additional != '' OR u.additional IS NOT NULL)"
            + "and u.service = :service"),
    @NamedQuery(name = "Get.PI.Additional",
        query = "select u from MCRPI u "
            + "where u.mycoreID = :mcrId "
            + "and (u.additional = :additional OR (u.additional IS NULL AND :additional IS NULL))"
            + "and u.service = :service"),
    @NamedQuery(name = "Get.PI.Unregistered",
        query = "select u from MCRPI u "
            + "where u.type = :type "
            + "and u.registered is null"),
    @NamedQuery(name = "Update.PI.Registered.Date",
        query = "update from MCRPI u "
            + "set u.registered = :date "
            + "where u.id = :id")
})
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "identifier", "type" }),
        @UniqueConstraint(columnNames = { "mycoreid", "service", "additional" })
    },
    indexes = {
        @Index(name = "Identifier", columnList = "identifier"),
        @Index(name = "MCRIdentifierService", columnList = "mycoreid, service")
    })
public class MCRPI implements org.mycore.pi.MCRPIRegistrationInfo {

    private static final long serialVersionUID = 234168232792525611L;

    // unique constraint für identifier type
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String mycoreID;

    @Column(length = 4096)
    private String additional;

    @Column(length = 128)
    private String service;

    @Column(nullable = false)
    private Date created;

    @Column()
    private Date registrationStarted;

    @Column()
    private Date registered;

    @Column(nullable = false)
    private String mcrVersion;

    //TODO: nullable since MCR-1393
    @Column
    private int mcrRevision;

    private MCRPI() {
    }

    public MCRPI(String identifier, String type, String mycoreID, String additional, String service, Date registered) {
        this();
        this.identifier = identifier;
        this.type = type;
        this.mycoreID = mycoreID;
        this.additional = additional;
        this.service = service;
        this.registered = registered;
        //TODO: disabled by MCR-1393
        //        this.mcrRevision = MCRCoreVersion.getRevision();
        this.mcrVersion = MCRCoreVersion.getVersion();
        this.created = new Date();
    }

    public MCRPI(String identifier, String type, String mycoreID, String additional, String service, Date registered,
        Date registrationStarted) {
        this();
        this.identifier = identifier;
        this.type = type;
        this.mycoreID = mycoreID;
        this.additional = additional;
        this.service = service;
        this.registered = registered;
        this.registrationStarted = registrationStarted;
        //TODO: disabled by MCR-1393
        //        this.mcrRevision = MCRCoreVersion.getRevision();
        this.mcrVersion = MCRCoreVersion.getVersion();
        this.created = new Date();
    }

    public Date getRegistrationStarted() {
        return registrationStarted;
    }

    public void setRegistrationStarted(Date registrationStarted) {
        this.registrationStarted = registrationStarted;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getMycoreID() {
        return mycoreID;
    }

    public void setMycoreID(String mycoreID) {
        this.mycoreID = mycoreID;
    }

    @Override
    public String getAdditional() {
        return additional;
    }

    public void setAdditional(String additional) {
        this.additional = additional;
    }

    @Override
    public String getMcrVersion() {
        return mcrVersion;
    }

    public void setMcrVersion(String mcrVersion) {
        this.mcrVersion = mcrVersion;
    }

    @Override
    public int getMcrRevision() {
        return mcrRevision;
    }

    public void setMcrRevision(int mcrRevision) {
        this.mcrRevision = mcrRevision;
    }

    @Override
    public Date getRegistered() {
        return registered;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getId() {
        return id;
    }
}
