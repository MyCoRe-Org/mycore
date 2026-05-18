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

import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutTransformerFactory;
import org.mycore.common.xsl.MCRParameterCollector;

/**
 * {@link URIResolver} that resolves a URI and transforms its result using an XSL stylesheet.
 */
public class MCRLayoutTransformerResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRLayoutTransformerFactory layoutTransformerFactory;

    public MCRLayoutTransformerResolver() {
        layoutTransformerFactory = MCRConfiguration2.getInstanceOfOrThrow(MCRLayoutTransformerFactory.class,
            "MCR.Layout.Transformer.Factory");
    }

    /**
     * Resolves the target URI and transforms its content using the specified XSL transformer.
     * <p>Optional query parameters are passed to the transformer if it implements
     * {@link MCRParameterizedTransformer}; otherwise they are ignored.
     * If the target URI resolves to {@code null} or the sub-URI is empty, an empty result is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{transformerId}[?param1=value1&amp;param2=value2]:{anyMCRUri}
     * </pre>
     * <p>Example request:
     * <pre>
     *   xslTransform:myStylesheet?lang=de:mcrobject:mcr_document_00000001
     * </pre>
     * <p>Example response: the transformed XML content produced by the stylesheet.
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, passed through to the delegated resolver
     * @return a {@link Source} wrapping the transformed content, or an empty result if the
     *         target URI resolves to {@code null} or the sub-URI is empty
     * @throws TransformerException if the target URI cannot be resolved or the transformation fails
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String help = href.substring(href.indexOf(':') + 1);
        String transformerId = new StringTokenizer(help, ":").nextToken();
        String target = help.substring(help.indexOf(':') + 1);

        String subUri = target.substring(target.indexOf(':') + 1);
        if (subUri.isEmpty()) {
            return MCRURIResolverResponse.ofNull();
        }

        Map<String, String> params;
        StringTokenizer tok = new StringTokenizer(transformerId, "?");
        transformerId = tok.nextToken();

        if (tok.hasMoreTokens()) {
            params = MCRURIResolverHelper.parseQueryParameters(tok.nextToken());
        } else {
            params = Collections.emptyMap();
        }
        Source resolved = MCRURIResolver.obtainInstance().resolve(target, base);

        try {
            if (resolved != null) {
                MCRSourceContent content = new MCRSourceContent(resolved);
                MCRContentTransformer transformer = layoutTransformerFactory.getTransformer(transformerId);
                MCRContent result;
                if (transformer instanceof MCRParameterizedTransformer parameterizedTransformer) {
                    MCRParameterCollector paramcollector = MCRParameterCollector.ofCurrentSession();
                    paramcollector.setParameters(params);
                    result = parameterizedTransformer.transform(content, paramcollector);
                } else {
                    result = transformer.transform(content);
                }
                return result.getSource();
            } else {
                LOGGER.debug("MCRLayoutStyleResolver returning empty xml");
                return MCRURIResolverResponse.ofNull();
            }
        } catch (Exception e) {
            throw MCRURIResolverHelper.asTransformerException(e);
        }
    }

}
