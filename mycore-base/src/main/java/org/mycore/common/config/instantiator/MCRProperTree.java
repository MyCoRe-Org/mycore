package org.mycore.common.config.instantiator;

import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.CLASS_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.log.MCRTreeMessage;

/**
 * A {@link MCRProperTree} is an immutable tree of configuration properties with dot-separated keys
 * that is used by {@link MCRInstantiator} during instantiation of configured classes.
 * <p>
 * The value of a tree node can be obtained with {@link MCRProperTree#value()}. Nested tree nodes
 * can be obtained with {@link MCRProperTree#nested(String)} and {@link MCRProperTree#deeplyNested(String)}.
 * The first method expects a single-level-key like <code>Foo</code>, while the latter method supports nested
 * keys like <code>Foo.Bar.Baz</code>.
 * <p>
 * The special key {@link MCRInstanceConfiguration#CLASS_KEY} is treated special: Entries for this key
 * are ignored in {@link MCRProperTree#keys()}, {@link MCRProperTree#nested(String)} and
 * {@link MCRProperTree#deeplyNested(String)}. A single string value for this key, if present,
 * can be obtained with {@link MCRProperTree#classValue()}.
 */
public final class MCRProperTree {

    private static final MCRProperTree EMPTY = new MCRProperTree(null, Map.of());

    public static final Predicate<String> CLASS_KEY_FILTER = key -> !key.equals(CLASS_KEY);

    private static final Function<String, Object> HASH_MAP = _ -> new HashMap<>();

    private final String value;

    private final Map<String, MCRProperTree> nested;

    private MCRProperTree(String value, Map<String, MCRProperTree> nested) {
        this.value = value;
        this.nested = nested;
    }

    public String value() {
        return value;
    }

    public String classValue() {
        return nested.getOrDefault(CLASS_KEY, EMPTY).value;
    }

    public Stream<String> keys() {
        if (nested.containsKey(CLASS_KEY)) {
            return nested.keySet().stream().filter(CLASS_KEY_FILTER);
        } else {
            return nested.keySet().stream();
        }
    }

    public MCRProperTree nested(String key) {
        return key.equals(CLASS_KEY) ? EMPTY : nested.getOrDefault(key, EMPTY);
    }

    public MCRProperTree deeplyNested(String key) {
        MCRProperTree tree = this;
        int startIndex = 0;
        int nextDotIndex = key.indexOf('.', startIndex);
        while (nextDotIndex != -1) {
            tree = tree.nested(key.substring(startIndex, nextDotIndex));
            startIndex = nextDotIndex + 1;
            nextDotIndex = key.indexOf('.', startIndex);
        }
        return tree.nested(key.substring(startIndex));
    }

    public Map<String, String> toProperties() {
        Map<String, String> properties = new HashMap<>();
        if (value != null) {
            properties.put("", value);
        }
        for (Map.Entry<String, MCRProperTree> nestedEntry : nested.entrySet()) {
            nestedEntry.getValue().putProperties(properties, nestedEntry.getKey());
        }
        return properties;
    }

    private void putProperties(Map<String, String> properties, String prefix) {
        if (value != null) {
            properties.put(prefix, value);
        }
        for (Map.Entry<String, MCRProperTree> nestedEntry : nested.entrySet()) {
            nestedEntry.getValue().putProperties(properties, prefix + "." + nestedEntry.getKey());
        }
    }

    public MCRTreeMessage toTreeMessage() {
        MCRTreeMessage treeMessage = new MCRTreeMessage();
        nested.keySet().stream().sorted().forEach(key -> {
            MCRProperTree nestedTree = nested.get(key);
            treeMessage.add(key, nestedTree.value, nestedTree.toTreeMessage());
        });
        return treeMessage;
    }

    public static MCRProperTree of(String value) {
        return new MCRProperTree(value, Map.of());
    }

    public static MCRProperTree ofProperties(Map<String, String> properties) {
        return toTree(toNestedMap(properties));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toNestedMap(Map<String, String> properties) {
        Map<String, Object> root = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> current = root;
            if (key.isEmpty()) {
                root.put(null, entry.getValue());
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
                ((Map<String, Object>) current.computeIfAbsent(lastKeyPart, HASH_MAP)).put(null, entry.getValue());
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static MCRProperTree toTree(Map<String, Object> map) {
        Map<String, MCRProperTree> nested = new HashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                nested.put(key, toTree((Map<String, Object>) value));
            }
        });
        return new MCRProperTree((String) map.get(null), nested);
    }

    @Override
    public String toString() {
        return "{ " + value + nested.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", ", " | ", " }"));
    }

}
