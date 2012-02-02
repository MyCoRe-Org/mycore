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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mycore.common.MCRUserInformation;

/**
 * Represents a login user. Each user has a unique numerical ID.
 * Each user belongs to a realm. The user name must be unique within a realm.
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCRUser implements MCRUserInformation {
    /** The unique user ID */
    int internalID;

    /** The login user name */
    private String userName;

    /** The realm the user comes from */
    private MCRRealm realm;

    /** The password hash of the user, for users from local realm */
    private String password;

    //base64 encoded
    private String salt;

    private MCRPasswordHashType hashType;

    /** The ID of the user that owns this user, or 0 */
    private MCRUser owner;

    /** The name of the person that this login user represents */
    private String realName;

    /** The E-Mail address of the person that this login user represents */
    private String eMail;

    /** A hint stored by the user in case password is forgotten */
    private String hint;

    /** The last time the user logged in */
    private Date lastLogin;

    private Map<String, String> attributes;

    /**
     * @return the attributes
     */
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
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param realm the realm this user belongs to
     */
    public MCRUser(String userName, MCRRealm mCRRealm) {
        this.userName = userName;
        this.realm = mCRRealm;
    }

    /**
     * Creates a new user.
     * 
     * @param userName the login user name
     * @param realmID the ID of the realm this user belongs to
     */
    public MCRUser(String userName, String realmID) {
        this.userName = userName;
        this.realm = MCRRealm.getRealm(realmID);
    }

    /**
     * Creates a new user in the default realm.
     * 
     * @param userName the login user name
     */
    public MCRUser(String userName) {
        this.userName = userName;
        this.realm = MCRRealm.getLocalRealm();
    }

    /**
     * Returns the login user name. The user name is
     * unique within its realm.
     *  
     * @return the login user name.
     */
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
    public MCRRealm getRealm() {
        return realm;
    }

    /**
     * Returns the ID of the realm the user belongs to.
     * 
     * @return the ID of the realm the user belongs to.
     */
    public String getRealmID() {
        return realm.getID();
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realmID the ID of the realm the user belongs to.
     */
    void setRealmID(String realmID) {
        setRealm(MCRRealm.getRealm(realmID));
    }

    /**
     * Sets the realm this user belongs to. 
     * The realm can be changed as long as the login user name
     * is unique within the new realm.
     * 
     * @param realm the realm the user belongs to.
     */
    void setRealm(MCRRealm realm) {
        this.realm = realm;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the salt
     */
    public String getSalt() {
        return salt;
    }

    /**
     * @param salt the salt to set
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * @return the hashType
     */
    public MCRPasswordHashType getHashType() {
        return hashType;
    }

    /**
     * @param hashType the hashType to set
     */
    public void setHashType(MCRPasswordHashType hashType) {
        this.hashType = hashType;
    }

    /**
     * Returns the user that owns this user, or null 
     * if the user is independent and has no owner.
     *  
     * @return the user that owns this user.
     */
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
     * Returns a list of users this user owns.
     * 
     * @return a list of users this user owns.
     */
    public List<MCRUser> getOwnedUsers() {
        return MCRUserManager.listUsers(this);
    }

    /**
     * Returns the name of the person this login user represents.
     * 
     * @return the name of the person this login user represents.
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Returns the E-Mail address of the person this login user represents.
     * 
     * @return the E-Mail address of the person this login user represents.
     */
    public String getEMailAddress() {
        return eMail;
    }

    /**
     * Returns a hint the user has stored in case of forgotten password.
     * 
     * @return a hint the user has stored in case of forgotten password.
     */
    public String getHint() {
        return hint;
    }

    /**
     * Returns the last time the user has logged in.
     * 
     * @return the last time the user has logged in.
     */
    public Date getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets the user that owns this user. 
     * Setting this to null makes the user independent.
     * 
     * @param ownerID the ID of the owner of the user.
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
     * Sets a hint to store in case of password loss.
     * 
     * @param hint a hint for the user in case password is forgotten.
     */
    public void setHint(String hint) {
        this.hint = hint;
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
        this.lastLogin = lastLogin;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MCRUser))
            return false;
        MCRUser other = (MCRUser) obj;
        return (other.userName.equals(this.userName)) && (other.realm.equals(this.realm));
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }

    @Override
    public String getUserID() {
        String cuid = this.getUserName();
        if (!getRealm().equals(MCRRealm.getLocalRealm()))
            cuid += "@" + getRealmID();

        return cuid;
    }

    @Override
    public String getUserAttribute(String attribute) {
        if (MCRUserInformation.ATT_REAL_NAME.equals(attribute)) {
            return getRealName();
        }
        return getAttributes().get(attribute);
    }

    @Override
    public boolean isUserInRole(final String role) {
        return getSystemGroupIDs().contains(role);
    }

    public Collection<String> getSystemGroupIDs() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> getExternalGroupIDs() {
        // TODO Auto-generated method stub
        return null;
    }
}
