package org.mycore.frontend.xeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
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

    private Map<String, List<Object>> field2node = new HashMap<String, List<Object>>();

    public MCRFieldMapper() {
    }

    public MCRFieldMapper(Document doc) {
        doc.getDescendants(Filters.element()).forEach(e -> {
            e.getAttributes().forEach(attribute -> {
                mapField2Node(attribute, attribute.getValue());
            });
        });
        doc.getDescendants(Filters.textOnly()).forEach(text -> {
            Element element = text.getParent();
            mapField2Node(element, text.getText());
        });
    }

    private void mapField2Node(Object node, String value) {
        if (isMappedField(value)) {
            String fieldName = getFieldName(value);
            addNodeToMap(node, fieldName);
        }
    }

    private void addNodeToMap(Object node, String fieldName) {
        if (!field2node.containsKey(fieldName)) {
            List<Object> nodes = new ArrayList<Object>();
            field2node.put(fieldName, nodes);
        }
        field2node.get(fieldName).add(node);
    }

    private boolean isMappedField(String value) {
        return value.startsWith(PREFIX);
    }

    private String getFieldName(String value) {
        return value.substring(PREFIX.length(), value.indexOf(SUFFIX));
    }

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
            return getFieldName(value);
        } else {
            String fieldName = buildFieldName(binding, nameInHTML);
            binding.getBoundNodes().stream().forEach(node -> {
                encodeValue(binding, node, fieldName);
                addNodeToMap(node, fieldName);
            });
            return fieldName;
        }
    }

    private String buildFieldName(MCRBinding binding, String nameInHTML) {
        String baseFieldName = StringUtils.isBlank(nameInHTML) ? buildFieldNameFromQName(binding) : nameInHTML;
        String indexSuffix = field2node.containsKey(baseFieldName) ? "_" + Integer.toString(fieldCount++) : "";
        return baseFieldName + indexSuffix;
    }

    private String buildFieldNameFromQName(MCRBinding binding) {
        Object n = binding.getBoundNode();
        String qName = n instanceof Element ? ((Element) n).getQualifiedName() : ((Attribute) n).getQualifiedName();
        String fieldName = qName.replace(":", "_");
        return fieldName;
    }

    public void emptyNotResubmittedNodes() {
        getNodesStream().forEach(node -> {
            if (node instanceof Element element) {
                element.setText("");
            } else if (node instanceof Attribute attribute) {
                attribute.setValue("");
            }
        });

        field2node.clear();
    }

    private Stream<Object> getNodesStream() {
        return field2node.values().stream().flatMap(nodes -> nodes.stream());
    }

    public boolean hasField(String fieldName) {
        return field2node.containsKey(fieldName);
    }

    public List<Object> removeResubmittedValueNodes(String fieldName, int numValuesReturned) {
        List<Object> nodesOfField = field2node.get(fieldName);
        List<Object> nodesResubmitted = new ArrayList<Object>();

        int maxNodesReturnable = Math.min(nodesOfField.size(), numValuesReturned);
        nodesResubmitted.addAll(nodesOfField.subList(0, maxNodesReturnable));
        nodesOfField.removeAll(nodesResubmitted);
        
        return nodesResubmitted;
    }
}
