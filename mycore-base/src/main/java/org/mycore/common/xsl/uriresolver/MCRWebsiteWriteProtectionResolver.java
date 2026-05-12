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
 * Resolver for MCRWebsiteWriteProtection. Returns an XML with the following format:
 * <code>
 *   &lt;message  active="true|false"&gt;Message to display when write protection is active&lt;/message&gt;
 * </code>
 */
public class MCRWebsiteWriteProtectionResolver implements URIResolver {

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
