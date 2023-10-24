package org.mycore.common.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRClassTools;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Resolves arbitrary static methods of arbitrary classes. Parameters are considerd to be of type
 * {@link java.lang.String}.
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
    private Logger LOGGER = LogManager.getLogger(MCRFunctionResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        LOGGER.debug("Resolving {}", href);

        String[] parts = href.split(":");
        String className = parts[1];
        String methodName = parts[2];

        Object[] params = Arrays.stream(parts).skip(3).toArray(String[]::new);

        try {
            Class[] types = new Class[params.length];
            Arrays.fill(types, String.class);

            Object result = null;
            Method method = MCRClassTools.forName(className).getMethod(methodName, types);
            result = method.invoke(null, params);

            Element string = new Element("string");
            string.setText(result == null ? "" : String.valueOf(result));

            return new JDOMSource(string);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }
}
