package org.mycore.frontend.xeditor.mapper;

import java.util.ArrayList;
import java.util.List;

public class MCRField {

    private String name;

    private String defaultValue = "";

    private List<Object> nodes = new ArrayList<Object>();

    public MCRField() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void addNode(Object node) {
        nodes.add(node);
    }

    public List<Object> getNodes() {
        return nodes;
    }
}
