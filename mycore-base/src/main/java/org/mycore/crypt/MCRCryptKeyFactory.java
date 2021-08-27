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

import org.mycore.common.MCRException;

import java.io.IOException;
//import java.nio.charset.StandardCharsets.UTF8;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.HashMap;


/**
 * 
 * 
 *
 */
public class MCRCryptKeyFactory {
	
	//private static HashMap<String, Optional<String> > cryptKeys = new HashMap<String, Optional<String> >();
	
	private static final Logger LOGGER = LogManager.getLogger(MCRCryptKeyFactory.class);
	
    public static Optional<String> getCryptKey(String keyFilePath) throws MCRException {
    	
        /*if (cryptKeys.containsKey(keyFilePath)) {
        	LOGGER.info("Get key from cache {}.", keyFilePath );
        	return (cryptKeys.get(keyFilePath));
        } else {*/
        	try {
        		LOGGER.info("Get key from file {}.", keyFilePath );
        		Optional <String> cryptKey = Optional.of(
        				Files.readString(FileSystems.getDefault().getPath(keyFilePath))
        			);
        		//cryptKeys.put(keyFilePath,cryptKey);
        		return cryptKey;
        	} catch (NoSuchFileException e) {
        		throw new MCRCryptKeyFileNotFoundException ( 
        				"Keyfile " + keyFilePath 
        				+ " not found. Generate new one with cli command or copy file to path."
        				, e ) ;
        	} catch (IOException e) {
        		throw new MCRException ("Can't read keyFile " + keyFilePath + ".",e) ; 
        	}
        	
        //}
        
    }
}
