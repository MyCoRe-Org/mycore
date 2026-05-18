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
 * Transforms the result of another resolver using an XSL stylesheet.
 * <p>
 * Usage:
 * <pre>
 * xslTransform:&lt;transformer&gt;&lt;?param1=value1&amp;param2=value2&gt;:&lt;anyMyCoReURI&gt;
 * </pre>
 */
public class MCRLayoutTransformerResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCRLayoutTransformerFactory layoutTransformerFactory;

    public MCRLayoutTransformerResolver() {
        layoutTransformerFactory = MCRConfiguration2.getInstanceOfOrThrow(MCRLayoutTransformerFactory.class,
            "MCR.Layout.Transformer.Factory");
    }

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
