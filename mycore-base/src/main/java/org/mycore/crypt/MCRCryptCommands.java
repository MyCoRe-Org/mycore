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
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.frontend.cli.MCRAbstractCommands;

import java.nio.file.FileAlreadyExistsException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class provides a set of commands for the org.mycore.crypt management
 * which can be used by the command line interface.
 * 
 * @author Paul Borchert
 */

@MCRCommandGroup(name = "Crypt Commands")
public class MCRCryptCommands extends MCRAbstractCommands {
    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRCryptCommands.class.getName());

    /**
     * list all cipher configuration
     * 
     */
    @MCRCommand(syntax = "show cipher configuration",
        help = "The command list all chipher configured in mycore.properties",
        order = 10)
    public static void showChipherConfig() {
        Map<String, String> subProps = MCRConfiguration2.getSubPropertiesMap("MCR.Crypt.Cipher");
        LOGGER.info("Cipher configuration: \n" 
            + subProps.entrySet()
                .stream()
                .sorted(Map.Entry.<String, String>comparingByKey()) //.reversed())
                .map( entry -> "MCR.Crypt.Cipher" + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n")));
                
    }
    
    /**
     * generate keyfile for cipher {0} 
     * 
     * @param cipherid
     *            String id of cipher configured in properties
     */
    @MCRCommand(syntax = "generate keyfile for cipher {0}",
        help = "The command generate the keyfile for the cipher configured in mycore.properties. "
               + "Fails if key file exists.",
        order = 20)
    public static void generateKeyFile(String cipherid) {
        LOGGER.info("Start generateKeyFile");
        try {
            MCRCipher cipher = MCRCipherManager.getUnIntitialisedCipher(cipherid);
            cipher.generateKeyFile();
            cipher.init(cipherid);
            LOGGER.info("Keyfile generated.");
        } catch (MCRCryptCipherConfigurationException e) {
            LOGGER.info(e.getStackTraceAsString());
            LOGGER.info("Error while initialising cipher. No keyfile generated. ");
        } catch (FileAlreadyExistsException e) {
            LOGGER.info("Keyfile already exists. No keyfile generated.");
        }
    }
    
    /**
     * generate and overwrite of keyfile of cipher {0} 
     * 
     * @param cipherid
     *            String id of cipher configured in properties
     */
    @MCRCommand(syntax = "overwrite keyfile for cipher {0}",
        help = "The command generate on overwrite the keyfile for the cipher configured in mycore.properties."
               + " Fails if key file exists.",
        order = 20)
    public static void overwriteKeyFile(String cipherid) {
        LOGGER.info("Start genrerateKeyFile");
        try {
            MCRCipher cipher = MCRCipherManager.getUnIntitialisedCipher(cipherid);
            cipher.overwriteKeyFile();
            cipher.init(cipherid);
            LOGGER.info("Keyfile overwriten.");
        } catch (MCRCryptCipherConfigurationException e) {
            LOGGER.info(e.getStackTraceAsString());
            LOGGER.info("Error while initialising cipher. No keyfile generated. ");
        } 
        
    }
    
    /**
     * encrypt {0} with cipher {1} 
     * 
     * @param value
     *            The value to be encrypted
     * 
     * @param cipherid
     *            String id of cipher configured in properties
     */
    @MCRCommand(syntax = "encrypt {0} with cipher {1}",
        help = "The command encrypt the value with cipher.",
        order = 30)
    public static void encrypt(String value, String cipherid) {
        try {
            MCRCipher cipher = MCRCipherManager.getCipher(cipherid);
            LOGGER.info("Encrypted Value: " + cipher.encrypt(value));
        } catch (MCRCryptKeyNoPermission e) {
            LOGGER.info("Value not encrypted. No Permission for chipher " + cipherid + ".");
        } catch ( MCRCryptCipherConfigurationException e) {
        	LOGGER.info(e.getStackTraceAsString());
        	LOGGER.info("Value not encrypted. Invalid configuration or key.");
        }
    }
    
    /**
     * decrypt {0} with cipher {1} 
     * 
     * @param value
     *            The value to be decrypted
     * 
     * @param cipherid
     *            String id of cipher configured in properties
     */
    @MCRCommand(syntax = "decrypt {0} with cipher {1}",
        help = "The command encrypt the value with cipher.",
        order = 40)
    public static void decrypt(String value, String cipherid) {
        try {
            MCRCipher cipher = MCRCipherManager.getCipher(cipherid);
            LOGGER.info("Decrypted Value: " + cipher.decrypt(value));
        } catch (MCRCryptKeyNoPermission e) {
            LOGGER.info("Value not decrypted. No Permission for chipher " + cipherid + ".");
        } catch ( MCRCryptCipherConfigurationException e) {
        	LOGGER.info(e.getStackTraceAsString());
        	LOGGER.info("Value not decrypted. Invalid configuration or key.");
        }
    }

}
