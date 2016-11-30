/**
 * $Revision$ 
 * $Date$
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.annotation.MCRUserAttribute;
import org.mycore.user2.annotation.MCRUserAttributeJavaConverter;
import org.mycore.user2.utils.MCRRolesConverter;
import org.mycore.user2.utils.MCRUserNameConverter;

/**
 * Represents a login user. Each user has a unique numerical ID.
 * Each user belongs to a realm. The user name must be unique within a realm.
 * Any changes made to an instance of this class does not persist automatically.
 * Use {@link MCRUserManager#updateUser(MCRUser)} to achieve this.
 * 
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 * @author Ren\u00E9 Adler (eagle)
 */
@Entity
@Access(AccessType.PROPERTY)
@Table(name = "MCRUser", uniqueConstraints = @UniqueConstraint(columnNames = { "userName", "realmID" }))
// TODO use @Cacheable instead
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "ownerId", "realName", "eMail", "lastLogin", "validUntil", "roles", "attributesMap",
    "password" })
public class MCRUser implements MCRUserInformation, Cloneable, Serializable {
    private static final long serialVersionUID = 3378645055646901800L;

    /** The unique user ID */
    int internalID;

    /** if locked, user may not change this instance */
    @XmlAttribute(name = "locked")
    private boolean locked;

    @XmlAttribute(name = "disabled")
    private boolean disabled;

    /** The login user name */
    @MCRUserAttribute
    @MCRUserAttributeJavaConverter(MCRUserNameConverter.class)
    @XmlAttribute(name = "name")
    private String userName;

    @XmlElement
    private Password password;

    /** The realm the user comes from */
    @XmlAttribute(name = "realm")
    private String realmID;

    /** The ID of the user that owns this user, or 0 */
    private MCRUser owner;

    /** The name of the person that this login user represents */
    @MCRUserAttribute
    @XmlElement
    private String realName;

    /** The E-Mail address of the person that this login user represents */
    @MCRUserAttribute
    @XmlElement
    private String eMail;

    /** The last time the user logged in */
    @XmlElement
    private Date lastLogin;

    @XmlElement
    private Date validUntil;

    /**
     * 
     */
    private Map<String, String> attributes;

    @Transient
    private Collection<String> systemRoles;

    @Transient
    private Collection<String> externalRoles;

    protected MCRUser() {
        this(null);
    }

    /**
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param mcrRealm the realm this user belongs to
     */
    public MCRUser(String userName, MCRRealm mcrRealm) {
        this(userName, mcrRealm.getID());
    }

    /**
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param realmID the ID of the realm this user belongs to
     */
    public MCRUser(String userName, String realmID) {
        this.userName = userName;
        this.realmID = realmID;
        this.systemRoles = new HashSet<String>();
        this.externalRoles = new HashSet<String>();
        this.attributes = new HashMap<String, String>();
        this.password = new Password();
    }

    /**
     * Creates a new user in the default realm.
     * 
     * @param userName the login user name
     */
    public MCRUser(String userName) {
        this(userName, MCRRealmFactory.getLocalRealm().getID());
    }

    /**
     * @return the internalID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    int getInternalID() {
        return internalID;
    }

    /**
     * @param internalID the internalID to set
     */
    void setInternalID(int internalID) {
        this.internalID = internalID;
    }

    @Column(name = "locked", nullable = true)
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked == null ? false : locked;
    }

    /**
     * @return the disabled
     */
    @Column(name = "disabled", nullable = true)
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled the disabled to set
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled == null ? false : disabled;
    }

    /**
     * Returns the login user name. The user name is
     * unique within its realm.
     *  
     * @return the login user name.
     */
    @Column(name = "userName", nullable = false)
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the login user name. The login user name
     * can be changed as long as it is unique within
     * its realm and the user ID is not changed.
     * 
     * @param userName the new login user name
     */
    void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the realm the user belongs to.
     * 
     * @return the realm the user belongs to.
     */
    @Transient
    public MCRRealm getRealm() {
        return MCRRealmFactory.getRealm(realmID);
    }

    /**
     * Returns the ID of the realm the user belongs to.
     * 
     * @return the ID of the realm the user belongs to.
     */
    @Column(name = "realmID", length = 128, nullable = false)
    public String getRealmID() {
        return realmID;
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realmID the ID of the realm the user belongs to.
     */
    void setRealmID(String realmID) {
        if (realmID == null) {
            setRealm(MCRRealmFactory.getLocalRealm());
        } else {
            setRealm(MCRRealmFactory.getRealm(realmID));
        }
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realm the realm the user belongs to.
     */
    void setRealm(MCRRealm realm) {
        this.realmID = realm.getID();
    }

    /**
     * @return the hash
     */
    @Column(name = "password", nullable = true)
    public String getPassword() {
        return password == null ? null : password.hash;
    }

    /**
     * @param password the hash value to set
     */
    public void setPassword(String password) {
        this.password.hash = password;
    }

    /**
     * @return the salt
     */
    @Column(name = "salt", nullable = true)
    public String getSalt() {
        return password == null ? null : password.salt;
    }

    /**
     * @param salt the salt to set
     */
    public void setSalt(String salt) {
        this.password.salt = salt;
    }

    /**
     * @return the hashType
     */
    @Column(name = "hashType", nullable = true)
    @Enumerated(EnumType.STRING)
    public MCRPasswordHashType getHashType() {
        return password == null ? null : password.hashType;
    }

    /**
     * @param hashType the hashType to set
     */
    public void setHashType(MCRPasswordHashType hashType) {
        this.password.hashType = hashType;
    }

    /**
     * Returns the user that owns this user, or null 
     * if the user is independent and has no owner.
     *  
     * @return the user that owns this user.
     */
    @ManyToOne
    @JoinColumn(name = "owner", nullable = true)
    public MCRUser getOwner() {
        return owner;
    }

    /**
     * Returns true if this user has no owner and therefore
     * is independent. Independent users may change their passwords 
     * etc., owned users may not, they are created to limit read access
     * in general.
     * 
     * @return true if this user has no owner
     */
    public boolean hasNoOwner() {
        return owner == null;
    }

    /**
     * Returns the name of the person this login user represents.
     * 
     * @return the name of the person this login user represents.
     */
    @Column(name = "realName", nullable = true)
    public String getRealName() {
        return realName;
    }

    /**
     * Returns the E-Mail address of the person this login user represents.
     * 
     * @return the E-Mail address of the person this login user represents.
     */
    @Transient
    public String getEMailAddress() {
        return eMail;
    }

    @Column(name = "eMail", nullable = true)
    private String getEMail() {
        return eMail;
    }

    /**
     * Returns a hint the user has stored in case of forgotten hash.
     * 
     * @return a hint the user has stored in case of forgotten hash.
     */
    @Column(name = "hint", nullable = true)
    public String getHint() {
        return password == null ? null : password.hint;
    }

    /**
     * Returns the last time the user has logged in.
     * 
     * @return the last time the user has logged in.
     */
    @Column(name = "lastLogin", nullable = true)
    public Date getLastLogin() {
        if (lastLogin == null) {
            return null;
        }
        return new Date(lastLogin.getTime());
    }

    /**
     * Sets the user that owns this user. 
     * Setting this to null makes the user independent.
     * 
     * @param owner the owner of the user.
     */
    public void setOwner(MCRUser owner) {
        this.owner = owner;
    }

    /**
     * Sets the name of the person this login user represents.
     * 
     * @param realName the name of the person this login user represents.
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * Sets a hint to store in case of hash loss.
     * 
     * @param hint a hint for the user in case hash is forgotten.
     */
    public void setHint(String hint) {
        this.password.hint = hint;
    }

    /**
     * Sets the E-Mail address of the person this user represents.
     * 
     * @param eMail the E-Mail address
     */
    public void setEMail(String eMail) {
        this.eMail = eMail;
    }

    /**
     * Sets the time of last login to now.
     */
    public void setLastLogin() {
        this.lastLogin = new Date();
    }

    /**
     * Sets the time of last login.
     * 
     * @param lastLogin the last time the user logged in.
     */
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin == null ? null : new Date(lastLogin.getTime());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRUser)) {
            return false;
        }
        MCRUser other = (MCRUser) obj;
        if (realmID == null) {
            if (other.realmID != null) {
                return false;
            }
        } else if (!realmID.equals(other.realmID)) {
            return false;
        }
        if (userName == null) {
            if (other.userName != null) {
                return false;
            }
        } else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((realmID == null) ? 0 : realmID.hashCode());
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Transient
    @Override
    public String getUserID() {
        String cuid = this.getUserName();
        if (!getRealm().equals(MCRRealmFactory.getLocalRealm()))
            cuid += "@" + getRealmID();

        return cuid;
    }

    /**
     * Returns additional user attributes.
     * This methods handles {@link MCRUserInformation#ATT_REAL_NAME} and
     * all attributes defined in {@link #getAttributes()}.
     */
    @Override
    public String getUserAttribute(String attribute) {
        switch (attribute) {
            case MCRUserInformation.ATT_REAL_NAME:
                return getRealName();
            case MCRUserInformation.ATT_EMAIL:
                return getEMailAddress();
            default:
                return getAttributes().get(attribute);
        }
    }

    @Override
    public boolean isUserInRole(final String role) {
        boolean directMember = getSystemRoleIDs().contains(role) || getExternalRoleIDs().contains(role);
        if (directMember) {
            return true;
        }
        return MCRRoleManager.isAssignedToRole(this, role);
    }

    /**
     * @return the attributes
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MCRUserAttr", joinColumns = @JoinColumn(name = "id"))
    @MapKeyColumn(name = "name", length = 128)
    @Column(name = "value", length = 255)
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns a collection any system role ID this user is member of.
     * @see MCRRole#isSystemRole()
     */
    @Transient
    public Collection<String> getSystemRoleIDs() {
        return systemRoles;
    }

    /**
     * Returns a collection any external role ID this user is member of.
     * @see MCRRole#isSystemRole()
     */
    @Transient
    public Collection<String> getExternalRoleIDs() {
        return externalRoles;
    }

    /**
     * Adds this user to the given role.
     * @param roleName the role the user should be added to (must already exist)
     */
    public void assignRole(String roleName) {
        MCRRole mcrRole = MCRRoleManager.getRole(roleName);
        if (mcrRole == null) {
            throw new MCRException("Could not find role " + roleName);
        }
        assignRole(mcrRole);
    }

    private void assignRole(MCRRole mcrRole) {
        if (mcrRole.isSystemRole()) {
            getSystemRoleIDs().add(mcrRole.getName());
        } else {
            getExternalRoleIDs().add(mcrRole.getName());
        }
    }

    /**
     * Removes this user from the given role.
     * @param roleName the role the user should be removed from (must already exist)
     */
    public void unassignRole(String roleName) {
        MCRRole mcrRole = MCRRoleManager.getRole(roleName);
        if (mcrRole == null) {
            throw new MCRException("Could not find role " + roleName);
        }
        if (mcrRole.isSystemRole()) {
            getSystemRoleIDs().remove(mcrRole.getName());
        } else {
            getExternalRoleIDs().remove(mcrRole.getName());
        }
    }

    /**
     * Enable login for this user.
     */
    public void enableLogin() {
        setDisabled(false);
    }

    /**
     * Disable login for this user.
     */
    public void disableLogin() {
        setDisabled(true);
    }

    /**
     * Returns true if logins are allowed for this user.
     */
    public boolean loginAllowed() {
        return !disabled && (validUntil == null || validUntil.after(new Date()));
    }

    /**
     * Returns a {@link Date} when this user can not login anymore.
     */
    @Column(name = "validUntil", nullable = true)
    public Date getValidUntil() {
        if (validUntil == null) {
            return null;
        }
        return new Date(validUntil.getTime());
    }

    /**
     * Sets a {@link Date} when this user can not login anymore.
     * @param validUntil the validUntil to set
     */
    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil == null ? null : new Date(validUntil.getTime());
    }

    //This is used for MCRUserAttributeMapper

    @Transient
    Collection<String> getRolesCollection() {
        return Arrays.stream(getRoles()).map(MCRRole::getName).collect(Collectors.toSet());
    }

    @MCRUserAttribute(name = "roles", separator = ";")
    @MCRUserAttributeJavaConverter(MCRRolesConverter.class)
    void setRolesCollection(Collection<String> roles) {
        for (String role : roles) {
            assignRole(role);
        }
    }

    //This is code to get JAXB work

    private static class Password {
        @XmlAttribute
        /** The hash hash of the user, for users from local realm */
        private String hash;

        //base64 encoded
        @XmlAttribute
        private String salt;

        @XmlAttribute
        private MCRPasswordHashType hashType;

        /** A hint stored by the user in case hash is forgotten */
        @XmlAttribute
        private String hint;

    }

    private static class MapEntry {
        @XmlAttribute
        public String name;

        @XmlAttribute
        public String value;
    }

    private static class UserIdentifier {
        @XmlAttribute
        public String name;

        @XmlAttribute
        public String realm;
    }

    @Transient
    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "role")
    private MCRRole[] getRoles() {
        if (getSystemRoleIDs().isEmpty() && getExternalRoleIDs().isEmpty()) {
            return null;
        }
        ArrayList<String> roleIds = new ArrayList<String>(getSystemRoleIDs().size() + getExternalRoleIDs().size());
        Collection<MCRRole> roles = new ArrayList<MCRRole>(roleIds.size());
        roleIds.addAll(getSystemRoleIDs());
        roleIds.addAll(getExternalRoleIDs());
        for (String roleName : roleIds) {
            MCRRole role = MCRRoleManager.getRole(roleName);
            if (role == null) {
                throw new MCRException("Could not load role: " + roleName);
            }
            roles.add(role);
        }
        return roles.toArray(new MCRRole[roles.size()]);
    }

    @SuppressWarnings("unused")
    private void setRoles(MCRRole[] roles) {
        for (MCRRole role : roles) {
            //check if role does exist, so we wont lose it on export
            String roleName = role.getName();
            role = MCRRoleManager.getRole(roleName);
            if (role == null) {
                throw new MCRException("Could not load role: " + roleName);
            }
            assignRole(role);
        }
    }

    @Transient
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    private MapEntry[] getAttributesMap() {
        if (attributes == null) {
            return null;
        }
        ArrayList<MapEntry> list = new ArrayList<MapEntry>(attributes.size());
        for (Entry<String, String> entry : attributes.entrySet()) {
            MapEntry mapEntry = new MapEntry();
            mapEntry.name = entry.getKey();
            mapEntry.value = entry.getValue();
            list.add(mapEntry);
        }
        return list.toArray(new MapEntry[list.size()]);
    }

    @SuppressWarnings("unused")
    private void setAttributesMap(MapEntry[] entries) {
        for (MapEntry entry : entries) {
            attributes.put(entry.name, entry.value);
        }
    }

    @Transient
    @XmlElement(name = "owner")
    private UserIdentifier getOwnerId() {
        if (owner == null) {
            return null;
        }
        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.name = owner.getUserName();
        userIdentifier.realm = owner.getRealmID();
        return userIdentifier;
    }

    @SuppressWarnings("unused")
    private void setOwnerId(UserIdentifier userIdentifier) {
        if (userIdentifier.name.equals(this.userName) && userIdentifier.realm.equals(this.realmID)) {
            setOwner(this);
            return;
        }
        MCRUser owner = MCRUserManager.getUser(userIdentifier.name, userIdentifier.realm);
        setOwner(owner);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRUser clone() {
        MCRUser copy = getSafeCopy();
        if (copy.password == null) {
            copy.password = new Password();
        }
        copy.password.hashType = this.password.hashType;
        copy.password.hash = this.password.hash;
        copy.password.salt = this.password.salt;
        return copy;
    }

    /**
     * Returns this MCRUser with basic information.
     * Same as {@link #getSafeCopy()} but without these informations:
     * <ul>
     * <li>real name
     * <li>eMail
     * <li>attributes
     * <li>role information
     * <li>last login
     * <li>valid until
     * <li>password hint
     * </ul>
     * @return a clone copy of this instance
     */
    @Transient
    public MCRUser getBasicCopy() {
        MCRUser copy = new MCRUser(userName, realmID);
        copy.locked = locked;
        copy.disabled = disabled;
        copy.owner = this.equals(this.owner) ? copy : this.owner;
        copy.setAttributes(null);
        copy.password = null;
        return copy;
    }

    /**
     * Returns this MCRUser with safe information.
     * Same as {@link #clone()} but without these informations:
     * <ul>
     * <li>password hash type
     * <li>password hash value
     * <li>password salt
     * </ul>
     * @return a clone copy of this instance
     */
    @Transient
    public MCRUser getSafeCopy() {
        MCRUser copy = getBasicCopy();
        if (getHint() != null) {
            copy.password = new Password();
            copy.password.hint = getHint();
        }
        copy.setAttributes(new HashMap<String, String>());
        copy.eMail = this.eMail;
        copy.lastLogin = this.lastLogin;
        copy.validUntil = this.validUntil;
        copy.realName = this.realName;
        copy.systemRoles.addAll(this.systemRoles);
        copy.externalRoles.addAll(this.externalRoles);
        copy.attributes.putAll(this.attributes);
        return copy;
    }
}
