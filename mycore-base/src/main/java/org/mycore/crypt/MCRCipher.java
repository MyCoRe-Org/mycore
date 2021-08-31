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

import org.mycore.access.MCRAccessManager;

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
    abstract public void init(String id) throws MCRCryptKeyFileNotFoundException;
    
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
    abstract public void overwriteKeyFile() ;
    
    
    public String encrypt(String text) throws MCRCryptKeyNoPermission {
        return ((checkPermission( "encrypt" )) ? pencrypt(text) : "");
    }
    
    public byte[] encrypt(byte[] bytes) throws MCRCryptKeyNoPermission {
        return ((checkPermission( "encrypt" )) ? pencrypt(bytes) : "".getBytes());
    }
    
    public String decrypt(String text) throws MCRCryptKeyNoPermission {
        return ((checkPermission( "decrypt" )) ? pdecrypt(text) : text);
    }
    
    public byte[] decrypt(byte[] bytes) throws MCRCryptKeyNoPermission {
        return ((checkPermission( "decrypt" )) ? pencrypt(bytes) : bytes);
    }
    
    private boolean checkPermission ( String action) {
        return (MCRAccessManager.checkPermission("crypt:cipher:" + cipherID , action ));
    }
    
    abstract protected String pencrypt(String text);
    abstract protected String pdecrypt(String text);
    abstract protected byte[] pencrypt(byte[] bytes);
    abstract protected byte[] pdecrypt(byte[] bytes);
    
}
