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
import org.mycore.mcr.acl.accesskey.service.MCRAccessKeyService;

/**
 * {@link URIResolver} that returns all access keys for a given reference as an XML source.
 */
public class MCRAccessKeyURIResolver implements URIResolver {

    private static final String ELEMENT_NAME = "accesskeys";

    private final MCRAccessKeyService accessKeyService;

    /**
     * Creates a new {@link MCRAccessKeyURIResolver} using the default
     * {@link MCRAccessKeyService} instance.
     */
    public MCRAccessKeyURIResolver() {
        this(MCRAccessKeyService.obtainInstance());
    }

    /**
     * Creates a new {@link MCRAccessKeyURIResolver} with the specified access key service.
     *
     * @param accessKeyService the {@link MCRAccessKeyService} used to manage access key events
     */
    public MCRAccessKeyURIResolver(MCRAccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    /**
     * Resolves all access keys for the given reference and returns them as an XML source.
     * <p>The access keys are serialized as a JSON string inside the result element, with
     * the total count exposed as an attribute.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{reference}
     * </pre>
     * <p>Example request:
     * <pre>
     *   accesskeys:mcr_document_00000001
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <accesskeys count="2">[{"secret":"key1",...},{"secret":"key2",...}]</accesskeys>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <accesskeys>} element containing the
     *         JSON string and a {@code count} attribute
     * @throws IllegalArgumentException if the URI does not contain a {@code :}
     */
    @Override
    public Source resolve(String href, String base) {
        final String reference = extractReferenceFromHref(href);
        final List<MCRAccessKeyDto> accessKeyDtos = accessKeyService.findAccessKeysByReference(reference);
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
