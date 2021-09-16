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
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import java.util.Set;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * This class provides an URIResolver for encryption and decryption.
 * 
 * URI Pattern:
 * crypt:{encrypt/decrypt}:{cipherid}:{value}
 * 
 * where 
 * <ul>
 *   <li>encrypt/decrypt - the action to act an value</li>
 *   <li>cipherid - ID of the cipher</li>  
 *   <li>value - string to be encryptet or decypted</li>
 * </ul>
 * @author Paul Borchert
 */

public class MCRCryptResolver implements URIResolver {

    public static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    
    private static final Logger LOGGER = LogManager.getLogger(MCRCryptResolver.class);
    
    private static final Set<String> ACTIONS = Set.of("encrypt","decrypt");
    
    @Override
    public Source resolve(String s, String s1) throws TransformerException {
        String[] parts = s.split(":", 4);
        if (parts.length != 4) {
            throw new TransformerException (
                    "Malformed CrypResolver uri. Must be crypt:{encrypt/decrypt}:{cipherid}:{value}"
                );
        }
        if (! ACTIONS.contains(parts[1]))  {
            throw new TransformerException("The crypt action must be one of encrypt or decrypt.");
        }
        String action = parts[1];
        
        String cipherID = parts[2];
        String value = parts[3];
        
        String returnString = "";
        try {
            MCRCipher cipher = MCRCipherManager.getCipher(cipherID);
            returnString =  action.equals("encrypt") ? cipher.encrypt(value) : cipher.decrypt(value);
        } catch (MCRCryptKeyFileNotFoundException e) {
        	LOGGER.error(MCRException.getStackTraceAsString(e));
        	returnString =  action.equals("encrypt") ? "" : value;
        } catch ( MCRCryptKeyNoPermissionException e) {
        	LOGGER.info("No permission to read cryptkey" + cipherID + ".");
        	returnString =  action.equals("encrypt") ? "" : value;
        } catch ( MCRCryptCipherConfigurationException e) {
        	LOGGER.error(e.getStackTraceAsString());
        	LOGGER.error("Invalid configuration or key.");
        	returnString =  action.equals("encrypt") ? "" : value;
        }
        
        final Element root = new Element("value");
        root.setText(returnString);
        
        return new JDOMSource(root);
    }
    
}
