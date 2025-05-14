package org.mycore.frontend.xeditor.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCRNodes;

public class MCRFieldMapping {

    private MCRFieldCoder coder = new MCRFieldCoder();

    private Map<String, MCRField> name2field = new LinkedHashMap<String, MCRField>();

    private Map<Object, MCRField> node2field = new LinkedHashMap<Object, MCRField>();

    public MCRField getField(MCRBinding binding) {
        Object firstNode = binding.getBoundNode();
        if (!node2field.containsKey(firstNode)) {
            MCRField field = new MCRField();
            binding.getBoundNodes().stream().forEach(node -> field.addNode(node));
            registerInNode2Field(field);
        }
        return node2field.get(firstNode);
    }

    void registerInName2Field(MCRField field) {
        String fieldName = field.getName();
        if ((fieldName != null) && !name2field.containsKey(fieldName)) {
            name2field.put(fieldName, field);
        }
    }

    void registerInNode2Field(MCRField field) {
        field.getNodes().forEach(node -> node2field.put(node, field));
    }

    public boolean hasField(String fieldName) {
        return name2field.containsKey(fieldName);
    }

    public MCRField getField(String fieldName) {
        return name2field.get(fieldName);
    }

    public void encode() {
        node2field.entrySet().forEach(entry -> {
            Object node = entry.getKey();
            MCRField field = entry.getValue();
            coder.encode(field, node);
        });
    }

    public void removeField(MCRField field) {
        name2field.remove(field.getName());
        field.getNodes().forEach(node -> node2field.remove(node));
    }

    public void setFieldsWithoutValues() {
        node2field.keySet().forEach(node -> {
            MCRField field = node2field.get(node);
            new MCRNodes(node).setValue(field.getDefaultValue());
        });
    }
}
