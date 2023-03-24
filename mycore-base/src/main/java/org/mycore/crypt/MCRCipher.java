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

package org.mycore.crypt;

import java.nio.file.FileAlreadyExistsException;
import java.security.InvalidKeyException;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRProperty;

/**
 * Abstract class of a concrete cipherimplementation
 * 
 * After checking the permission call the encrypt an decrypt
 * functionality. The permission is set by acl.
 * 
 * Example for chipher with id abstract:
 * crypt:abstract   {encrypt:decrypt}   "administrators only"
 *  
 * @author Paul Borchert
 *
 */

public abstract class MCRCipher {

    protected String cipherID;

    private boolean aclEnabled = true;

    /**
     * Initialize the chipher by reading the key from file. If the cipher can't initialized an exception
     * will thrown. Common issue is an missing key. In this case the methods throws 
     * a MCRCryptKeyFileNotFoundException.
     * 
     * Needs the id of cipher as parameter, because the id can't be set during 
     * instanciating by getSingleInstanceOf.  
     * 
     * @param id ID of cipher as configured
     */
    abstract public void init(String id) throws MCRCryptKeyFileNotFoundException, InvalidKeyException;

    /**
     * Return whether cipher has been initialized. 
     */
    abstract public boolean isInitialised();

    /**
     * Revert init process. 
     */
    abstract public void reset();

    /**
     * If no keyfile exsits, generate the secret key an write it 
     * to the keyfile.  
     */
    abstract public void generateKeyFile() throws FileAlreadyExistsException;

    /**
     * Generate the secret key an write it to the keyfile. Overwrites
     * exsisting keyfile.   
     */
    abstract public void overwriteKeyFile();

    public boolean getAclEnabled() {
        return aclEnabled;
    }

    @MCRProperty(name = "EnableACL", required = false)
    public void setAclEnabled(final String enabled) {
        if ("true".equalsIgnoreCase(enabled) || "false".equalsIgnoreCase(enabled)) {
            aclEnabled = Boolean.parseBoolean(enabled);
        } else {
            throw new MCRConfigurationException("MCRCrypt: " + enabled + " is not a valid boolean.");
        }
    }

    public String encrypt(String text) throws MCRCryptKeyNoPermissionException {
        if (checkPermission("encrypt")) {
            return encryptImpl(text);
        } else {
            throw new MCRCryptKeyNoPermissionException("No Permission to encrypt with the chiper " + cipherID + ".");
        }
    }

    public byte[] encrypt(byte[] bytes) throws MCRCryptKeyNoPermissionException {
        if (checkPermission("encrypt")) {
            return encryptImpl(bytes);
        } else {
            throw new MCRCryptKeyNoPermissionException("No Permission to encrypt with the chiper " + cipherID + ".");
        }
    }

    public String decrypt(String text) throws MCRCryptKeyNoPermissionException {
        if (checkPermission("decrypt")) {
            return decryptImpl(text);
        } else {
            throw new MCRCryptKeyNoPermissionException("No Permission to decrypt with the chiper " + cipherID + ".");
        }
    }

    public byte[] decrypt(byte[] bytes) throws MCRCryptKeyNoPermissionException {
        if (checkPermission("decrypt")) {
            return decryptImpl(bytes);
        } else {
            throw new MCRCryptKeyNoPermissionException("No Permission to decrypt with the chiper " + cipherID + ".");
        }
    }

    private boolean checkPermission(String action) {
        if (aclEnabled) {
            return MCRAccessManager.checkPermission("crypt:cipher:" + cipherID, action);
        }
        return true;
    }

    abstract protected String encryptImpl(String text);

    abstract protected String decryptImpl(String text);

    abstract protected byte[] encryptImpl(byte[] bytes);

    abstract protected byte[] decryptImpl(byte[] bytes);

}
