package org.mycore.frontend.xeditor.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;

public class MCRFieldCoder {

    private static final String PI_TARGET = "xed_field";

    public void encode(MCRField field, Object node) {
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("name", field.getName());

        if (node instanceof Attribute) {
            data.put("xpath", "@" + MCRNameBuilder.buildFieldNameFromQName(node));
        }

        if (!field.getDefaultValue().isEmpty()) {
            data.put("default", field.getDefaultValue());
        }

        ProcessingInstruction pi = new ProcessingInstruction(PI_TARGET, data);
        Element parent = node instanceof Attribute a ? a.getParent() : (Element) node;
        parent.addContent(pi);
    }

    public MCRField decode(ProcessingInstruction pi) {
        String fieldName = pi.getPseudoAttributeValue("name");

        MCRField field = new MCRField();
        field.setName(fieldName);

        String defaultValue = pi.getPseudoAttributeValue("default");
        if (defaultValue != null) {
            field.setDefaultValue(defaultValue);
        }

        String xPath = pi.getPseudoAttributeValue("xpath");
        Element parent = pi.getParentElement();
        if (xPath == null) {
            field.addNode(parent);
        } else {
            Attribute a = XPathFactory.instance()
                .compile(xPath, Filters.attribute(), null, MCRConstants.getStandardNamespaces())
                .evaluateFirst(parent);
            field.addNode(a);
        }

        return field;
    }
}
