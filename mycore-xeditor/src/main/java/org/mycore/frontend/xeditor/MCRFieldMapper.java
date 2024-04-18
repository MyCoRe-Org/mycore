package org.mycore.frontend.xeditor;

import java.util.HashSet;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;

/**
 * Builds HTML field names for xml nodes. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRFieldMapper {

    private static final String PREFIX = "@@_";

    private static final String SUFFIX = "_@@=";

    private int fieldCount = 1;

    private Set<String> usedFields = new HashSet<String>();

    public String getDecodedValue(Object node) {
        String value = MCRBinding.getValue(node);
        return getDecodedValue(value);
    }

    private static String getDecodedValue(String value) {
        return value.contains(SUFFIX) ? value.substring(value.indexOf(SUFFIX) + SUFFIX.length()) : value;
    }

    public void encodeValue(MCRBinding binding, Object node, String fieldName) {
        String valuePrefix = PREFIX + fieldName + SUFFIX;
        String givenValue = getDecodedValue(node);
        binding.setValue(node, valuePrefix + givenValue);
    }

    public boolean hasValue(MCRBinding binding, String value) {
        return binding.getBoundNodes().stream().anyMatch(n -> value.equals(getDecodedValue(n)));
    }

    public String getNameFor(MCRBinding binding, String nameInHTML) {
        String value = binding.getValue();
        if (value.contains(SUFFIX)) {
            return value.substring(PREFIX.length(), value.indexOf(SUFFIX));
        } else {
            String fieldName = buildFieldName(binding, nameInHTML);
            binding.getBoundNodes().stream().forEach(x -> encodeValue(binding, x, fieldName));
            return fieldName;
        }
    }

    private String buildFieldName(MCRBinding binding, String nameInHTML) {
        String baseFieldName
            = nameInHTML == null || nameInHTML.isBlank() ? buildFieldNameFromQName(binding) : nameInHTML;
        String fieldName = baseFieldName
            + (usedFields.contains(baseFieldName) ? "_" + Integer.toString(fieldCount++) : "");
        usedFields.add(fieldName);
        return fieldName;
    }

    private String buildFieldNameFromQName(MCRBinding binding) {
        Object n = binding.getBoundNode();
        String qName = n instanceof Element ? ((Element) n).getQualifiedName() : ((Attribute) n).getQualifiedName();
        String fieldName = qName.replace(":", "_");
        return fieldName;
    }

    public static void emptyNotResubmittedNodes(Document doc) {
        doc.getDescendants(Filters.element()).forEach(e -> {
            e.getAttributes().forEach(a -> a.setValue(getDecodedValue(a.getValue())));
        });
        doc.getDescendants(Filters.textOnly()).forEach(t -> t.setText(getDecodedValue(t.getText())));
    }
}
