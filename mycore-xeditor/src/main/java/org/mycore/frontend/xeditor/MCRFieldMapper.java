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
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

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

    private Map<Object, String> node2field = new HashMap<Object, String>();

    public MCRFieldMapper() {
    }

    public static MCRFieldMapper decodeFromXML(Document doc) {
        MCRFieldMapper mapper = new MCRFieldMapper();
        
        doc.getDescendants(Filters.element()).forEach(e -> {
            e.getAttributes().forEach(attribute -> {
                mapper.mapField2Node(attribute, attribute.getValue());
            });
        });
        doc.getDescendants(Filters.textOnly()).forEach(text -> {
            Element element = text.getParent();
            mapper.mapField2Node(element, text.getText());
        });
        
        return mapper;
    }
    
    private void mapField2Node(Object node, String value) {
        if (isMappedField(value)) {
            String fieldName = getFieldName(value);
            addNodeToMap(node, fieldName);
        }
    }

    private boolean isMappedField(String value) {
        return value.startsWith(PREFIX);
    }

    private String getFieldName(String value) {
        return value.substring(PREFIX.length(), value.indexOf(SUFFIX));
    }

    public void encodeIntoXML(MCRChangeTracker tracker) {
      node2field.keySet().forEach( node -> {
          String fieldName = node2field.get(node);
          encodeValue(tracker, node, fieldName);  
      } );
    }
    
    public String getDecodedValue(Object node) {
        String value = MCRBinding.getValue(node);
        return getDecodedValue(value);
    }
    
    private static String getDecodedValue(String value) {
        return value.contains(SUFFIX) ? value.substring(value.indexOf(SUFFIX) + SUFFIX.length()) : value;
    }
    
    public void encodeValue(MCRChangeTracker tracker, Object node, String fieldName) {
        String valuePrefix = PREFIX + fieldName + SUFFIX;
        String givenValue = getDecodedValue(node);
        MCRBinding binding = new MCRBinding(node);
        binding.setChangeTracker(tracker);
        binding.setValue(valuePrefix + givenValue);
    }
    
    public String getNameFor(MCRBinding binding, String nameInHTML) {
        Object firstNode = binding.getBoundNode();
        if (!node2field.containsKey(firstNode)) {
            String fieldName = buildFieldName(binding, nameInHTML);
            binding.getBoundNodes().stream().forEach(boundNode -> addNodeToMap(boundNode, fieldName));
        }
        return node2field.get(firstNode);
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

    private void addNodeToMap(Object node, String fieldName) {
        if (!field2node.containsKey(fieldName)) {
            List<Object> nodes = new ArrayList<Object>();
            field2node.put(fieldName, nodes);
        }
        field2node.get(fieldName).add(node);
        node2field.put(node, fieldName);
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
