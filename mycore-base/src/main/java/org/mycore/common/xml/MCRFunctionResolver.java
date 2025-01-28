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

package org.mycore.common.xml;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRClassTools;

/**
 * Resolves arbitrary static methods of arbitrary classes. Parameters are considerd to be of type
 * {@link java.lang.String}. Encoding parameter values is recommended.
 * <br/><br/>
 * <strong>Invocation</strong>
 * <pre><code>function:&lt;class name&gt;:&lt;method name&gt;:&lt;param1&gt;:&lt;param2&gt;</code></pre>
 * <br/>
 * <strong>Example</strong>
 * <pre><code>function:de.uni_jena.thunibib.user.ThUniBibUtils:getLeadId:id_connection:foobar;</code></pre>
 *
 * @author shermann (Silvio Hermann)
 * */
public class MCRFunctionResolver implements URIResolver {
    private static final Logger LOGGER = LogManager.getLogger(MCRFunctionResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug("Resolving {}", href);

        String[] parts = href.split(":");
        String className = parts[1];
        String methodName = parts[2];

        Object[] params = Arrays.stream(parts)
            .skip(3)
            .map(p -> URLDecoder.decode(p, StandardCharsets.UTF_8))
            .toArray(String[]::new);

        try {
            Class[] types = new Class[params.length];
            Arrays.fill(types, String.class);

            Method method = MCRClassTools.forName(className).getMethod(methodName, types);
            Object result = method.invoke(null, params);

            Element string = new Element("string");
            string.setText(result == null ? "" : String.valueOf(result));

            return new JDOMSource(string);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }
}
