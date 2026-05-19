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

package org.mycore.common.xsl.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;

import org.jdom2.JDOMException;
import org.mycore.frontend.MCRWebsiteWriteProtection;

/**
 * {@link URIResolver} that returns the current write protection status and message as XML.
 */
public class MCRWebsiteWriteProtectionURIResolver implements URIResolver {

    /**
     * Resolves the current write protection state and returns it as an XML source.
     * <p>If no message is configured, a default empty {@code <message>} element is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;
     * </pre>
     * <p>Example request:
     * <pre>
     *   websiteWriteProtection:
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <message active="true">Maintenance mode active</message>
     * }</pre>
     *
     * @param href the URI to resolve (unused)
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link DOMSource} wrapping the {@code <message>} element with an {@code active} attribute
     * @throws TransformerException if the configured message cannot be parsed
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        boolean active = MCRWebsiteWriteProtection.isActive();
        org.w3c.dom.Document message;
        try {
            message = MCRWebsiteWriteProtection.getMessage();
        } catch (JDOMException e) {
            throw new TransformerException(e);
        }
        if (message.getDocumentElement() == null) {
            //fallback to default message if no message is set
            message.appendChild(message.createElement("message"));
        }
        message.getDocumentElement().setAttribute("active", String.valueOf(active));
        return new DOMSource(message);
    }

}
