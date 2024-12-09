package org.mycore.frontend.xeditor.mapper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;

public class MCRNameBuilder {

    private Map<String, Integer> baseNames = new HashMap<String, Integer>();

    public String buildNameFor(Object node, String nameInHTML) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.isBlank(nameInHTML)) {
            sb.append(buildFieldNameFromQName(node));
        } else {
            sb.append(nameInHTML);
        }

        String baseName = sb.toString();
        if (baseNames.containsKey(baseName)) {
            int count = baseNames.get(baseName) + 1;
            baseNames.put(baseName, count);
            sb.append('[').append(count).append(']');
        } else {
            baseNames.put(baseName, 1);
        }

        return sb.toString();
    }

    public static String buildFieldNameFromQName(Object node) {
        if (node instanceof Element e) {
            return e.getQualifiedName();
        } else {
            return ((Attribute) node).getQualifiedName();
        }
    }
}
