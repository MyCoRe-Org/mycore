package org.mycore.common.config.instantiator;

import java.util.Map;
import java.util.stream.Stream;

import org.mycore.common.log.MCRTreeMessage;

public class MCREmptyTree implements MCRProperTree {

    @Override
    public String value() {
        return "";
    }

    @Override
    public Stream<String> keys() {
        return Stream.empty();
    }

    @Override
    public MCRProperTree nested(String key) {
        return MCRPropertiesTree.EMPTY;
    }

    @Override
    public MCRProperTree deeplyNested(String key) {
        return MCRPropertiesTree.EMPTY;
    }

    @Override
    public MCRProperTree excludeKey(String classKey) {
        return this;
    }

    @Override
    public Map<String, String> toProperties() {
        return Map.of();
    }

    @Override
    public MCRTreeMessage toTreeMessage() {
        return new MCRTreeMessage();
    }

    @Override
    public String toString() {
        return "{ }";
    }

}
