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


public abstract class MCRCipher {
    
    protected String cipherID;
    
    abstract public void init(String id);
    abstract public boolean isInitialised();
    abstract public void reset();
    abstract public void generateKeyFile() throws FileAlreadyExistsException;
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
