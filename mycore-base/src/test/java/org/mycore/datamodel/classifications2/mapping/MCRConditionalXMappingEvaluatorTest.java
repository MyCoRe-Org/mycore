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
package org.mycore.datamodel.classifications2.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.mapping.MCRConditionalXMappingEvaluator.Condition;
import org.mycore.datamodel.classifications2.mapping.MCRXMappingClassificationGeneratorBase.Evaluator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings({ "TrailingWhitespacesInTextBlock", "unchecked" })
public class MCRConditionalXMappingEvaluatorTest {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String JSON_WITH_NO_OTHER = """
        {
          "@type": "foobar",
          "foo": "result-for-foo-1 result-for-foo-2",
          "bar": "result-for-bar-only",
          "bar baz": ["result-for-bar-or-baz-1", "result-for-bar-or-baz-2"]
        }
        """;

    private static final String JSON_WITH_OTHER = """
        {
          "@type": "foobar",
          "foo": "result-for-foo-1 result-for-foo-2",
          "bar": "result-for-bar-only",
          "bar baz": ["result-for-bar-or-baz-1", "result-for-bar-or-baz-2"],
          "@other" : "result-for-something-else"
        }
        """;

    private static final String JSON_WITH_OTHERWISE = """
        {
          "@type": "foobar",
          "foo": "result-for-foo-1 result-for-foo-2",
          "bar": "result-for-bar-only",
          "bar baz": ["result-for-bar-or-baz-1", "result-for-bar-or-baz-2"],
          "@otherwise" : "result-if-nothing-else"
        }
        """;

    private static final List<String> FOO_IDS = List.of(
        "result-for-foo-1", "result-for-foo-2");

    private static final List<String> BAR_IDS = List.of(
        "result-for-bar-only", "result-for-bar-or-baz-1", "result-for-bar-or-baz-2");

    private static final List<String> BAZ_IDS = List.of(
        "result-for-bar-or-baz-1", "result-for-bar-or-baz-2");

    private static final List<String> OTHER_IDS = List.of(
        "result-for-something-else");

    private static final List<String> OTHERWISE_IDS = List.of(
        "result-if-nothing-else");

    private static final List<String> EVERYTHING_IDS = List.of(
        "result-for-everything-1", "result-for-everything-2", "result-for-everything-3");

    private static final List<String> NOTHING_IDS = List.of(
        "result-for-nothing");

    @Test
    public void testFooConditionValue() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo"));

        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testBarConditionValue() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "bar"));

        assertEquals(categoryIds(BAR_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(BAR_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(BAR_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testBazConditionValue() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "baz"));

        assertEquals(categoryIds(BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testXxxConditionValue() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "xxx"));

        assertEquals(categoryIds(), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(OTHER_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(OTHERWISE_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testFooBarConditionValues() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo", "bar"));

        assertEquals(categoryIds(FOO_IDS, BAR_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(FOO_IDS, BAR_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(FOO_IDS, BAR_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testFooBazConditionValues() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo", "baz"));

        assertEquals(categoryIds(FOO_IDS, BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(FOO_IDS, BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(FOO_IDS, BAZ_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testFooXxxConditionValues() {

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo", "xxx"));

        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(JSON_WITH_NO_OTHER, null));
        assertEquals(categoryIds(FOO_IDS, OTHER_IDS), evaluator.getCategoryIds(JSON_WITH_OTHER, null));
        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(JSON_WITH_OTHERWISE, null));

    }

    @Test
    public void testNoConditionValues() {

        String json = """
            {
              "@type": "foobar"
            }
            """;

        String jsonWithFoo = """
            {
              "@type": "foobar",
              "foo": "result-for-foo-1 result-for-foo-2"
            }
            """;

        String jsonWithNone = """
            {
              "@type": "foobar",
              "@none": "result-for-nothing"
            }
            """;

        String jsonWithFooAndNone = """
            {
              "@type": "foobar",
              "foo": "result-for-foo-1 result-for-foo-2",
              "@none": "result-for-nothing"
            }
            """;

        Evaluator evaluatorFoo = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo"));

        assertEquals(categoryIds(), evaluatorFoo.getCategoryIds(json, null));
        assertEquals(categoryIds(FOO_IDS), evaluatorFoo.getCategoryIds(jsonWithFoo, null));
        assertEquals(categoryIds(), evaluatorFoo.getCategoryIds(jsonWithNone, null));
        assertEquals(categoryIds(FOO_IDS), evaluatorFoo.getCategoryIds(jsonWithFooAndNone, null));

        Evaluator evaluatorNothing = new MCRConditionalXMappingEvaluator(createConditionMap("foobar"));

        assertEquals(categoryIds(), evaluatorNothing.getCategoryIds(json, null));
        assertEquals(categoryIds(), evaluatorNothing.getCategoryIds(jsonWithFoo, null));
        assertEquals(categoryIds(NOTHING_IDS), evaluatorNothing.getCategoryIds(jsonWithNone, null));
        assertEquals(categoryIds(NOTHING_IDS), evaluatorNothing.getCategoryIds(jsonWithFooAndNone, null));

    }

    @Test
    public void testFooConditionValueInArrayWithAdditionalIds() {

        String json = """
            [
              "result-for-everything-1 result-for-everything-2",
              "result-for-everything-3",
              """ + JSON_WITH_NO_OTHER + """
            ]
            """;

        logJson(json, "condition embedded in array");

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo"));

        assertEquals(categoryIds(EVERYTHING_IDS, FOO_IDS), evaluator.getCategoryIds(json, null));

    }

    @Test
    public void testFooConditionValueInObjectWithAdditionalCondition() {

        String json = """
            {
              "@type" : "age",
              "42" : """ + JSON_WITH_NO_OTHER + """
            }
            """;

        logJson(json, "condition embedded in other condition");

        Evaluator evaluator = new MCRConditionalXMappingEvaluator(createConditionsMap("age", "42", "foobar", "foo"));

        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(json, null));

    }

    @Test
    public void testFooConditionValueInObjectWithAdditionalConditionWithStackedEvaluators() throws Exception {

        String json = """
            {
              "@type" : "age",
              "42" : """ + new ObjectMapper().writeValueAsString(JSON_WITH_NO_OTHER) + """
            }
            """;

        logJson(json, "condition embedded in other condition with stacked evaluators");

        /* no one would ever do this, but is still works :) */
        Evaluator evaluator = new MCRConditionalXMappingEvaluator(
            new MCRConditionalXMappingEvaluator(createConditionMap("foobar", "foo")),
            createConditionMap("age", "42"));

        assertEquals(categoryIds(FOO_IDS), evaluator.getCategoryIds(json, null));

    }

    private Set<MCRCategoryID> categoryIds(List<String>... categoryIds) {
        return Arrays.stream(categoryIds).flatMap(List::stream).map(MCRCategoryID::ofString)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Condition> createConditionMap(String name, String... values) {
        return Map.of(name, object -> Set.of(values));
    }

    private Map<String, Condition> createConditionsMap(String name1, String value1, String name2, String... values2) {
        return Map.of(name1, object -> Set.of(value1), name2, object -> Set.of(values2));
    }

    private static void logJson(String jsonString, String description) {
        if (LOGGER.isInfoEnabled()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Object json = objectMapper.readValue(jsonString, Object.class);
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                LOGGER.info("JSON: {}\n{}", description, prettyJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
