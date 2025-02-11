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

package org.mycore.mcr.acl.accesskey;

import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.mcr.acl.accesskey.dto.MCRAccessKeyDto;
import org.mycore.mcr.acl.accesskey.mapper.MCRAccessKeyJsonMapper;
import org.mycore.mcr.acl.accesskey.model.MCRAccessKey;
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyServiceFactory;

/**
 * Returns a JSON string with all {@link MCRAccessKey} for an given reference.
 * <p>Syntax:</p>
 * <ul>
 * <li><code>accesskeys:{reference}</code> to resolve a access keys as JSON string and count as attribute</li>
 * </ul>
 */
public class MCRAccessKeyURIResolver implements URIResolver {

    private static final String ELEMENT_NAME = "accesskeys";

    @Override
    public Source resolve(String href, String base) {
        final String reference = extractReferenceFromHref(href);
        final List<MCRAccessKeyDto> accessKeyDtos
            = MCRAccessKeyServiceFactory.getAccessKeyService().findAccessKeysByReference(reference);
        final String json = MCRAccessKeyJsonMapper.accessKeyDtosToJson(accessKeyDtos);
        return createResultSource(json, accessKeyDtos.size());
    }

    private String extractReferenceFromHref(String href) {
        int colonIndex = href.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid href format. Expected ':' in href: " + href);
        }
        return href.substring(colonIndex + 1);
    }

    private Source createResultSource(String json, int count) {
        final Element element = new Element(ELEMENT_NAME);
        element.setText(json);
        element.setAttribute("count", Integer.toString(count));
        return new JDOMSource(element);
    }
}
