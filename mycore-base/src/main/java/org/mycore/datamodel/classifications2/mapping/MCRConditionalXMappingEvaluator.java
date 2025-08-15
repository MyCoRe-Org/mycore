/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
package org.mycore.datamodel.classifications2.mapping;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.mapping.MCRXMappingClassificationGeneratorBase.Evaluator;
import org.mycore.datamodel.metadata.MCRObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A {@link MCRConditionalXMappingEvaluator} is a {@link Evaluator} that provides category IDs based
 * on certain conditions of an {@link MCRObject}. To do so, it uses a map of {@link Condition} instances
 * that provide information about the condition of a MyCoRe object and a JSON-encoded mapping (the value
 * of the <code>x-mapping</code>-label) that contains the category IDs to be returned based on that condition.
 * It uses another {@link Evaluator} instance to evaluate string vales in that JSON-encoded mapping, usually a
 * {@link MCRSimpleXMappingEvaluator}.
 * <p>
 * The returned classification IDs are dependent on the MyCoRe objects.
 * <div style="border: 1px solid; padding: 5px;">
 * If The content of the <code>x-mapping</code>-label starts with <code>"</code>, <code>[</code> or <code>{</code>
 * it is parsed as JSON and interpreted as described below. Otherwise, it is evaluated by
 * the string evaluator. This allows easy integration for mappings that are not conditional
 * (as such mappings do not need to be encoded as a JSON string).
 * <p>
 * If the JSON-content of the <code>x-mapping</code>-label is …
 * <ul>
 * <li> … a JSON-string, it is evaluated by the string evaluator,
 * <li> … a JSON-array, each element is processed recursively,
 * <li> … a JSON-object, it is interpreted as a compact "choose"-"when …"-"otherwise"-expression as described below,
 * <li> … a JSON-number, JSON-boolean or JSON-null, an exception is thrown.
 * </ul>
 * For a JSON-object, the following steps are performed:
 * <ol>
 *  <li> The field with name <code>@type</code> is used to look up the name of the condition
 *  <li> The condition with that name is looked up in the map of conditions.
 *  <li> The <em>condition values</em> for the MyCoRe object are looked up using that condition.
 *  <li> If no <em>condition value</em> is returned:
 *  The content of the field with name <code>@none</code>, if present, is processes recursively.
 *  <li> For each field (except fields with names starting with <code>@</code>):
 *  The field name is parsed as a space separated list of <em>field values</em>.
 *  <li> For each <em>condition value</em>:
 *   <ol>
 *    <li> For all <em>field values</em> equal to the <em>condition value</em>:
 *    The corresponding field is chosen.
 *    <li> If no <em>field value</em> equals the <em>condition value</em>:
 *    The field with name <code>@other</code>, if present, is chosen.
 *   </ol>
 *   <li> If no <em>condition value</em> matched any <em>field value</em> (including <code>@other</code>):
 *   The field with name <code>@otherwise</code> is chosen.
 *   <li> The content of all chosen fields is processed recursively.
 * </ol>
 * <p>
 * Example-JSON:
 * <pre><code>
 * {
 *  "@type": "foobar",
 *  "foo": "result-for-foo-1 result-for-foo-2",
 *  "bar": "result-for-bar-only",
 *  "bar baz": ["result-for-bar-or-baz-1", "result-for-bar-or-baz-2"],
 *  "@other" : "result-for-anything-else",
 *  "@none" : "result-for-nothing"
 * }
 * </code></pre>
 * Results based on <em>condition values</em> for condition <code>foobar</code>:
 * <ul>
 *  <li>
 *   <code>foo</code>:
 *    <code>result-for-foo-1</code>,
 *    <code>result-for-foo-2</code>
 *  <li>
 *   <code>bar</code>:
 *    <code>result-for-bar-only</code>,
 *    <code>result-for-bar-or-baz-1</code>,
 *    <code>result-for-bar-or-baz-2</code>
 *  <li>
 *   <code>baz</code>:
 *    <code>result-for-bar-or-baz-1</code>,
 *    <code>result-for-bar-or-baz-2</code>
 *  <li>
 *   <code>xxx</code>:
 *    <code>result-for-anything-else</code>
 *  <li>
 *   <code>foo</code>, <code>bar</code>:
 *    <code>result-for-foo-1</code>,
 *    <code>result-for-foo-2</code>,
 *    <code>result-for-bar-only</code>,
 *    <code>result-for-bar-or-baz-1</code>,
 *    <code>result-for-bar-or-baz-2</code>
 *  <li>
 *   <code>foo</code>, <code>baz</code>:
 *    <code>result-for-foo-1</code>,
 *    <code>result-for-foo-2</code>,
 *    <code>result-for-bar-or-baz-1</code>,
 *    <code>result-for-bar-or-baz-2</code>
 *  <li>
 *   <code>foo</code>, <code>xxx</code>:
 *    <code>result-for-foo-1</code>,
 *    <code>result-for-foo-2</code>,
 *    <code>result-for-anything-else</code>
 *  <li>
 *   none:
 *   <code>result-for-nothing</code>
 * </ul>
 * If the above example had used <code>@otherwise</code> instead of <code>@other</code>,
 * the result for <em>condition values</em> <code>foo</code>,<code>xxx</code> would only be
 * <code>result-for-foo-1</code>, <code>result-for-foo-2</code>. The result wouldn't contain
 * <code>result-for-anything-else</code> anymore, because the field with name <code>@otherwise</code>
 * is only used, when no other <em>condition value</em> matched any <em>field value</em>, but
 * there were matches for <em>condition value</em> <code>foo</code>.
 * </div>
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRConditionalXMappingEvaluator#STRING_EVALUATOR_KEY} can be used to
 * specify the string evaluator used to obtain category IDs from strings.
 * <li> For the string evaluator, the property suffix {@link MCRSentinel#DEFAULT_KEY} can be used to
 * exclude the string evaluator from the configuration and use a default {@link MCRSimpleXMappingEvaluator} instead.
 * <li> The property suffix {@link MCRConditionalXMappingEvaluator#CONDITIONS_KEY} can be used to
 * specify the map of conditions to be used.
 * <li> For each condition, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that condition from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.datamodel.classifications2.mapping.MCRConditionalXMappingEvaluator
 * [...].StringEvaluator.Class=foo.bar.FooStringEvaluator
 * [...].StringEvaluator.Default=false
 * [...].StringEvaluator.Key1=Value1
 * [...].StringEvaluator.Key2=Value2
 * [...].Conditions.foo.Class=foo.bar.FooCondition
 * [...].Conditions.foo.Enabled=true
 * [...].Conditions.foo.Key1=Value1
 * [...].Conditions.foo.Key2=Value2
 * [...].Conditions.bar.Class=foo.bar.BarCondition
 * [...].Conditions.bar.Enabled=false
 * [...].Conditions.bar.Key1=Value1
 * [...].Conditions.bar.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRConditionalXMappingEvaluator.Factory.class)
public final class MCRConditionalXMappingEvaluator implements Evaluator {

    public static final String STRING_EVALUATOR_KEY = "StringEvaluator";

    public static final String CONDITIONS_KEY = "Conditions";

    private final Evaluator stringEvaluator;

    private final Map<String, Condition> conditions;

    public MCRConditionalXMappingEvaluator(Map<String, Condition> conditions) {
        this(new MCRSimpleXMappingEvaluator(), conditions);
    }

    public MCRConditionalXMappingEvaluator(Evaluator stringEvaluator, Map<String, Condition> conditions) {
        this.stringEvaluator = Objects
            .requireNonNull(stringEvaluator, "String Evaluator must not be null");
        this.conditions = Objects
            .requireNonNull(conditions, "Conditions must not be null");
        this.conditions.forEach((name, condition) -> Objects
            .requireNonNull(condition, "Condition " + name + "must not be null"));
    }

    @Override
    public Set<MCRCategoryID> getCategoryIds(String mapping, MCRObject object) {
        String trimmedMapping = mapping.trim();
        char firstCharacter = trimmedMapping.isEmpty() ? ' ' : trimmedMapping.charAt(0);
        if (firstCharacter == '"' || firstCharacter == '{' || firstCharacter == '[') {
            try {
                JsonNode jsonNode = new ObjectMapper().readTree(trimmedMapping);
                return getCategoryIds(jsonNode, object);
            } catch (JsonProcessingException e) {
                throw new MCRException("Failed to parse json from x-mapping value: " + trimmedMapping, e);
            }
        } else {
            return stringEvaluator.getCategoryIds(mapping, object);
        }
    }

    private Set<MCRCategoryID> getCategoryIds(JsonNode jsonNode, MCRObject object) {
        Set<MCRCategoryID> categoryIds = new LinkedHashSet<>();
        compileCategoryIds(categoryIds, jsonNode, object);
        return categoryIds;
    }

    private void compileCategoryIds(Set<MCRCategoryID> categoryIds, JsonNode jsonNode, MCRObject object) {
        switch (jsonNode.getNodeType()) {
            case STRING -> {
                categoryIds.addAll(stringEvaluator.getCategoryIds(jsonNode.asText(), object));
            }
            case ARRAY -> {
                ArrayNode arrayNode = (ArrayNode) jsonNode;
                for (JsonNode elementNode : arrayNode) {
                    compileCategoryIds(categoryIds, elementNode, object);
                }
            }
            case OBJECT -> {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                compileObjectCategoryIds(categoryIds, objectNode, object);
            }
            default -> throw new MCRException("Unexpected JSON node type " + jsonNode.getNodeType() + ": " + jsonNode);
        }
    }

    private void compileObjectCategoryIds(Set<MCRCategoryID> categoryIds, ObjectNode objectNode, MCRObject object) {

        JsonNode conditionTypeNode = objectNode.get("@type");
        if (conditionTypeNode == null || conditionTypeNode.getNodeType() != JsonNodeType.STRING) {
            throw new MCRException("Missing or incorrect @type " + conditionTypeNode + ": " + objectNode);
        }

        String conditionType = conditionTypeNode.asText();
        Condition condition = conditions.get(conditionType);
        if (condition == null) {
            throw new MCRException("Unknown condition " + conditionType + ": " + objectNode);
        }

        Set<String> conditionValues = condition.evaluate(object);
        if (conditionValues.isEmpty()) {
            JsonNode noneNode = objectNode.get("@none");
            if (noneNode != null) {
                compileCategoryIds(categoryIds, noneNode, object);
            }
        } else {
            compileObjectCategoryIds(categoryIds, objectNode, conditionValues, object);
        }

    }

    private void compileObjectCategoryIds(Set<MCRCategoryID> categoryIds, ObjectNode objectNode,
        Set<String> conditionValues, MCRObject object) {

        Map<String, Set<JsonNode>> jsonNodesByFieldValue = new HashMap<>();
        objectNode.forEachEntry((fieldName, jsonNode) -> {
            if (!fieldName.startsWith("@")) {
                String[] fieldValues = fieldName.split("\\s");
                for (String fieldValue : fieldValues) {
                    jsonNodesByFieldValue.computeIfAbsent(fieldValue, name -> new LinkedHashSet<>()).add(jsonNode);
                }
            }
        });

        boolean otherUsed = false;
        Set<JsonNode> chosenJsonNodes = new LinkedHashSet<>();
        for (String conditionValue : conditionValues) {
            Set<JsonNode> jsonNodesForConditionValue = jsonNodesByFieldValue.get(conditionValue);
            if (jsonNodesForConditionValue != null) {
                chosenJsonNodes.addAll(jsonNodesForConditionValue);
            } else if (!otherUsed) {
                JsonNode otherNode = objectNode.get("@other");
                if (otherNode != null) {
                    chosenJsonNodes.add(otherNode);
                }
                otherUsed = true;
            }
        }

        if (chosenJsonNodes.isEmpty()) {
            JsonNode otherwiseNode = objectNode.get("@otherwise");
            if (otherwiseNode != null) {
                chosenJsonNodes.add(otherwiseNode);
            }
        }

        chosenJsonNodes.forEach(jsonNode -> compileCategoryIds(categoryIds, jsonNode, object));

    }

    public interface Condition {

        Set<String> evaluate(MCRObject object);

    }

    public static class Factory implements Supplier<MCRConditionalXMappingEvaluator> {

        @MCRInstance(name = STRING_EVALUATOR_KEY, valueClass = Evaluator.class,
            required = false, sentinel = @MCRSentinel(name = MCRSentinel.DEFAULT_KEY, rejectionValue = true,
                defaultValue = false))
        public Evaluator stringEvaluator;

        @MCRInstanceMap(name = CONDITIONS_KEY, valueClass = Condition.class, required = false, sentinel = @MCRSentinel)
        public Map<String, Condition> conditions;

        @Override
        public MCRConditionalXMappingEvaluator get() {
            return new MCRConditionalXMappingEvaluator(getStringEvaluator(), conditions);
        }

        private Evaluator getStringEvaluator() {
            return Objects.requireNonNullElseGet(stringEvaluator, MCRSimpleXMappingEvaluator::new);
        }

    }

}
