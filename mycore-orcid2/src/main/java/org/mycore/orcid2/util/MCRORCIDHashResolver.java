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

package org.mycore.orcid2.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * Provides URIResolver to hash string.
 */
public class MCRORCIDHashResolver implements URIResolver {

    /**
     * Hashes given input with given algoritm.
     *
     * Syntax: <code>hash:{input}:{algorithm}:{?salt}:{?iterations}</code>
     * 
     * input and salt will be url decoded
     *
     * @param href
     *            URI in the syntax above
     * @param base
     *            not used
     *
     * @return hashed input as hex string
     * @throws MCRException query is invalid or hash algorithm is not supported
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] split = href.split(":", 4);
        if (split.length < 3) {
            throw new IllegalArgumentException("Invalid format of uri for retrieval of hash: " + href);
        }
        final String input = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
        final String algorithm = split[2];
        String result = null;
        try {
            if (split.length == 3) {
                result = MCRUtils.hashString(input, algorithm, null, 1);
            } else {
                final String optional = split[3];
                final int separatorIndex = optional.indexOf(":");
                if (separatorIndex >= 0) {
                    final String salt
                        = URLDecoder.decode(optional.substring(0, separatorIndex), StandardCharsets.UTF_8);
                    final int iterations = Integer.parseInt(optional.substring(separatorIndex + 1));
                    result = MCRUtils.hashString(input, algorithm, salt.getBytes(StandardCharsets.UTF_8), iterations);
                } else {
                    final String salt = URLDecoder.decode(optional, StandardCharsets.UTF_8);
                    result = MCRUtils.hashString(input, algorithm, salt.getBytes(StandardCharsets.UTF_8), 1);
                }
            }
        } catch (NumberFormatException e) {
            throw new MCRException("Invalid format of uri for retrieval of hash: " + href);
        }
        final Element root = new Element("string");
        root.setText(result);
        return new JDOMSource(root);
    }
}
