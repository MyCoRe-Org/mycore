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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Create Instances of MCRCipher as configured in mycore.properties.
 * The cipher is defined by the java class. 
 * The secret key is saved in an keyfile. 
 *  
 * MCR.Crypt.Cipher.abstract.Class=org.mycore.crypt.MCRAESCipher
 * MCR.Crypt.Cipher.id1.KeyFile=example_1.secret
 *
 */
public class MCRCipherFactory {
    private static Logger LOGGER = LogManager.getLogger(MCRCipherFactory.class.getName());
    
    /**
     * Create a single instance of a MCRCipher and initialize it. If the cipher can't initialized an exception
     * will thrown. Common issue is an missing key. In this case the methods throws 
     * a MCRCryptKeyFileNotFoundException. 
     * 
     * @param id ID of cipher as configured
     * @return instance of cipher
     */
    
    public static MCRCipher getCipher(String id) throws MCRCryptKeyFileNotFoundException {
        LOGGER.debug("getCipher for id {} .", id );
        String propertiy = "MCR.Crypt.Cipher." + id + ".class";
        MCRCipher cipher = MCRConfiguration2.<MCRCipher>getSingleInstanceOf(propertiy)
                .orElseThrow( 
                    () -> new MCRCryptCipherConfigurationException (
                        "Property " + propertiy + "not configured or class not found."
                        )
                );
        if (! cipher.isInitialised()) { 
            LOGGER.debug("init Cipher for id {} .", id );
            cipher.init(id);
        }
        return cipher;
    }
    
    /**
     * Create a single instance of a MCRCipher without initialising it.
     * The resulting instance can used to generate an new key.
     * 
     * @param id ID of cipher as configured
     * @return instance of cipher
     */
    
    
    public static MCRCipher getUnIntitialisedCipher(String id) {
        LOGGER.debug("getCipher for id {} .", id );
        String propertiy = "MCR.Crypt.Cipher." + id + ".class";
        MCRCipher cipher = MCRConfiguration2.<MCRCipher>getSingleInstanceOf(propertiy)
                .orElseThrow( 
                    () -> new MCRCryptCipherConfigurationException (
                        "Property " + propertiy + "not configured or class not found."
                        )
                );
        cipher.reset();
        return cipher;
    }
}
