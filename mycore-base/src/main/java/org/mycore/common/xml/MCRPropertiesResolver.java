package org.mycore.common.xml;

import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.config.MCRConfiguration2;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

/**
 * Resolves the Property values for the given key or key prefix.<br><br>
 * <br>
 * Syntax: <code>property:{key-prefix}*</code> or <br>
 *         <code>property:{key}</code>
 * <br>
 * Result for a key prefix: <code> <br>
 *     &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt; <br>
 *     &lt;properties&gt; <br>
 *   &lt;entry key=&quot;key1&quot;&gt;value1&lt;/property&gt; <br>
 *   &lt;entry key=&quot;key2&quot;&gt;value2&lt;/property&gt; <br>
 *   &lt;entry key=&quot;key3&quot;&gt;value3&lt;/property&gt; <br>
 * &lt;/properties&gt; <br>
 * </code>
 * If no entries with the given key prefix exist, a properties element without entry elements is returned.
 * <br>
 * Result for a key: <code> <br>
 *     &lt;entry key=&quot;key&quot;&gt;value&lt;/property&gt;
 * </code>
 * If no entry with the given key exists, an entry element without text content is returned.
 */
public class MCRPropertiesResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) {
        String target = href.substring(href.indexOf(":") + 1);

        if (target.endsWith("*")) {
            return resolveKeyPrefix(target.substring(0, target.length() - 1));
        } else {
            return resolveKey(target);
        }
    }

    private JDOMSource resolveKeyPrefix(String keyPrefix) {
        final Element propertiesElement = new Element("properties");

        MCRConfiguration2.getSubPropertiesMap(keyPrefix).forEach((key, value) -> {
            final Element entryElement = new Element("entry");
            entryElement.setAttribute("key", keyPrefix + key);
            entryElement.setText(value);
            propertiesElement.addContent(entryElement);
        });

        return new JDOMSource(asPropertiesDocument(propertiesElement));
    }

    private Document asPropertiesDocument(Element propertiesElement) {
        final Document document = new Document();
        document.setDocType(getPropertiesDocType());
        document.setContent(propertiesElement);
        return document;
    }

    private DocType getPropertiesDocType() {
        return new DocType("properties", "SYSTEM", "http://java.sun.com/dtd/properties.dtd");
    }

    private JDOMSource resolveKey(String key) {
        final Element entryElement = new Element("entry");

        entryElement.setAttribute("key", key);
        String value = MCRConfiguration2.getPropertiesMap().get(key);
        if (value != null) {
            entryElement.setText(value);
        }

        return new JDOMSource(entryElement);
    }
}
