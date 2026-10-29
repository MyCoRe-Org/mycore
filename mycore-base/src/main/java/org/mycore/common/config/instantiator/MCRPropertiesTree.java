/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.config.instantiator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.mycore.common.log.MCRTreeMessage;

/**
 * A {@link MCRPropertiesTree} is an immutable tree of configuration properties with dot-separated keys
 * that is used by {@link MCRInstantiator} during instantiation of configured classes.
 * <p>
 * The value of a tree node can be obtained with {@link MCRPropertiesTree#value()}. Nested tree nodes
 * can be obtained with {@link MCRPropertiesTree#nested(String)} and {@link MCRPropertiesTree#deeplyNested(String)}.
 * The first method expects a single-level-key like <code>Foo</code>, while the latter method supports nested
 * keys like <code>Foo.Bar.Baz</code>.
 */
public final class MCRPropertiesTree implements MCRProperTree {

    public static final MCRPropertiesTree EMPTY = new MCRPropertiesTree(null, Map.of());

    private static final Function<String, Object> HASH_MAP = _ -> new HashMap<>();

    private final String value;

    private final Map<String, MCRPropertiesTree> nested;

    private MCRPropertiesTree(String value, Map<String, MCRPropertiesTree> nested) {
        this.value = value;
        this.nested = nested;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Stream<String> keys() {
        return nested.keySet().stream();
    }

    @Override
    public MCRPropertiesTree nested(String key) {
        return nested.getOrDefault(key, EMPTY);
    }

    @Override
    public MCRPropertiesTree deeplyNested(String key) {
        MCRPropertiesTree tree = this;
        int startIndex = 0;
        int nextDotIndex = key.indexOf('.', startIndex);
        while (nextDotIndex != -1) {
            tree = tree.nested(key.substring(startIndex, nextDotIndex));
            startIndex = nextDotIndex + 1;
            nextDotIndex = key.indexOf('.', startIndex);
        }
        return tree.nested(key.substring(startIndex));
    }

    @Override
    public MCRProperTree excludeKey(String excludedKey) {
        if (this.nested.containsKey(excludedKey)) {
            Map<String, MCRPropertiesTree> nestedWithoutExcludedKey = new HashMap<>(this.nested);
            nestedWithoutExcludedKey.remove(excludedKey);
            return new MCRPropertiesTree(value, nestedWithoutExcludedKey);
        } else {
            return this;
        }
    }

    @Override
    public Map<String, String> toProperties() {
        Map<String, String> properties = new HashMap<>();
        if (value != null) {
            properties.put("", value);
        }
        for (Map.Entry<String, MCRPropertiesTree> nestedEntry : nested.entrySet()) {
            nestedEntry.getValue().putProperties(properties, nestedEntry.getKey());
        }
        return properties;
    }

    private void putProperties(Map<String, String> properties, String prefix) {
        if (value != null) {
            properties.put(prefix, value);
        }
        for (Map.Entry<String, MCRPropertiesTree> nestedEntry : nested.entrySet()) {
            nestedEntry.getValue().putProperties(properties, prefix + "." + nestedEntry.getKey());
        }
    }

    @Override
    public MCRTreeMessage toTreeMessage() {
        MCRTreeMessage treeMessage = new MCRTreeMessage();
        sortedEntries().forEach(entry -> {
            MCRPropertiesTree nestedTree = entry.getValue();
            treeMessage.add(entry.getKey(), nestedTree.value, nestedTree.toTreeMessage());
        });
        return treeMessage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder);
        return builder.toString();
    }

    private void appendTo(StringBuilder builder) {
        builder.append('{');
        if (value != null) {
            builder.append(' ').append(value);
        }
        builder.append(' ');
        if (!nested.isEmpty()) {
            builder.append('|');
            sortedEntries().forEach(entry -> {
                builder.append(' ').append(entry.getKey()).append('=');
                entry.getValue().appendTo(builder);
            });
            builder.append(' ');
        }
        builder.append('}');
    }

    private Stream<Map.Entry<String, MCRPropertiesTree>> sortedEntries() {
        return nested.entrySet().stream().sorted(Map.Entry.comparingByKey());
    }

    public static MCRPropertiesTree of(String value) {
        return new MCRPropertiesTree(value, Map.of());
    }

    public static MCRPropertiesTree ofProperties(Map<String, String> properties) {
        return toTree(toNestedMap(properties));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toNestedMap(Map<String, String> properties) {
        Map<String, Object> root = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> current = root;
            String value = entry.getValue();
            if (!value.isBlank()) {
                String trimmedValue = value.trim();
                if (key.isEmpty()) {
                    root.put(null, trimmedValue);
                } else {
                    int startIndex = 0;
                    int nextDotIndex = key.indexOf('.', startIndex);
                    while (nextDotIndex != -1) {
                        String keyPart = key.substring(startIndex, nextDotIndex);
                        current = (Map<String, Object>) current.computeIfAbsent(keyPart, HASH_MAP);
                        startIndex = nextDotIndex + 1;
                        nextDotIndex = key.indexOf('.', startIndex);
                    }
                    String lastKeyPart = key.substring(startIndex);
                    ((Map<String, Object>) current.computeIfAbsent(lastKeyPart, HASH_MAP)).put(null, trimmedValue);
                }
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static MCRPropertiesTree toTree(Map<String, Object> map) {
        Map<String, MCRPropertiesTree> nested = new HashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                nested.put(key, toTree((Map<String, Object>) value));
            }
        });
        return new MCRPropertiesTree((String) map.get(null), nested);
    }

}
