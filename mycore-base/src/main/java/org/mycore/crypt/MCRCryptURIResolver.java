/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that encrypts or decrypts a value using a configured cipher.
 */
public class MCRCryptURIResolver implements URIResolver {

    public static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ENCRYPT_ACTION = "encrypt";

    private static final String DECRYPT_ACTION = "decrypt";

    private static final Set<String> ACTIONS = Set.of(ENCRYPT_ACTION, DECRYPT_ACTION);

    /**
     * Encrypts or decrypts the given value using the specified cipher and returns the result as an XML source.
     * <p>If the cipher key is not found or the current user lacks permission to read it,
     * the result depends on the action: for decryption the original value is returned unchanged,
     * for encryption an empty string is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{encrypt|decrypt}:{cipherId}:{value}
     * </pre>
     * <p>Example request:
     * <pre>
     *   crypt:encrypt:myCipher:plaintext
     *   crypt:decrypt:myCipher:encryptedtext
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <value>encryptedOrDecryptedText</value>
     * }</pre>
     *
     * @param s the URI in the syntax above to resolve
     * @param s1 the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <value>} element containing the result
     * @throws TransformerException if the URI is malformed or the action is not {@code encrypt}
     *                              or {@code decrypt}
     */
    @Override
    public Source resolve(String s, String s1) throws TransformerException {
        String[] parts = s.split(":", 4);
        if (parts.length != 4) {
            throw new TransformerException(
                "Malformed CryptResolver uri. Must be crypt:{encrypt/decrypt}:{cipherid}:{value}");
        }
        if (!ACTIONS.contains(parts[1])) {
            throw new TransformerException("The crypt action must be one of encrypt or decrypt.");
        }
        String action = parts[1];

        String cipherID = parts[2];
        String value = parts[3];

        final Element root = new Element("value");
        root.setText(action.equals(ENCRYPT_ACTION) ? "" : value);
        try {
            MCRCipher cipher = MCRCipherManager.getCipher(cipherID);
            root.setText(action.equals(ENCRYPT_ACTION) ? cipher.encrypt(value) : cipher.decrypt(value));
        } catch (MCRCryptKeyFileNotFoundException e) {
            LOGGER.error(e::getMessage, e);
        } catch (MCRCryptKeyNoPermissionException e) {
            LOGGER.info(() -> "No permission to read cryptkey" + cipherID + ".");
        } catch (MCRCryptCipherConfigurationException e) {
            LOGGER.error("Invalid configuration or key.", e);
        }
        return new JDOMSource(root);
    }

}
