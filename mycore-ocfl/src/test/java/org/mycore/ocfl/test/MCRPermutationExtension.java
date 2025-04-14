package org.mycore.ocfl.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * Provides all value combinations for fields annotated with {@link PermutedParam}
 * using a test template. This enables parameterized testing across permutations
 * of booleans, enums, and strings.
 * <p>
 * Supported field types:
 * <ul>
 *   <li>{@code boolean}: true and false</li>
 *   <li>{@code enum}: all enum constants</li>
 *   <li>{@code String}: values from {@link PermutedValue}</li>
 * </ul>
 * <p>
 * Fields are automatically injected before each test invocation. Each combination
 * of values results in a separate test execution, with a descriptive name.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @ExtendWith(MCRPermutationExtension.class)
 * class MyTest {
 *     @PermutedParam
 *     boolean enabled;
 *
 *     @PermutedParam
 *     @PermutedValue(strings = { "dev", "prod" })
 *     String profile;
 *
 *     @TestTemplate
 *     void testSomething() { ... }
 * }
 * }
 * </pre>
 */
public class MCRPermutationExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
        org.junit.jupiter.api.extension.ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        List<Field> permutedFields = new ArrayList<>();

        // Collect all fields annotated with @PermutedParam
        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(PermutedParam.class)) {
                permutedFields.add(field);
            }
        }

        // For each field, determine the list of candidate values.
        List<List<?>> valuesForFields = new ArrayList<>();
        for (Field field : permutedFields) {
            valuesForFields.add(getValuesForField(field));
        }

        // Compute the Cartesian product (all combinations)
        List<List<?>> combinations = cartesianProduct(valuesForFields);
        List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();

        // Create an invocation context for each combination
        for (List<?> combination : combinations) {
            invocationContexts.add(new MCRPermutationInvocationContext(permutedFields, combination));
        }

        return invocationContexts.stream();
    }

    /**
     * Determine candidate values for the field based on its type.
     * Boolean fields default to [true, false]; enum fields use the enum constants;
     * and String fields must be annotated with @PermutedValue.
     */
    private List<?> getValuesForField(Field field) {
        Class<?> type = field.getType();
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Arrays.asList(Boolean.TRUE, Boolean.FALSE);
        } else if (type.isEnum()) {
            Object[] enumConstants = type.getEnumConstants();
            return Arrays.asList(enumConstants);
        } else if (type.equals(String.class)) {
            PermutedValue permutedValue = field.getAnnotation(PermutedValue.class);
            if (permutedValue != null) {
                return List.of(permutedValue.strings());
            } else {
                throw new IllegalStateException("Field " + field.getName() +
                    " of type String must be annotated with @PermutedValue");
            }
        } else {
            throw new IllegalStateException("Unsupported field type for permutation: " + type.getName());
        }
    }

    /**
     * Computes the Cartesian product for a list of lists.
     *
     * @param lists a list of candidate value lists for each parameter.
     * @return a list containing all possible combinations.
     */
    private List<List<?>> cartesianProduct(List<List<?>> lists) {
        List<List<?>> resultLists = new ArrayList<>();
        if (lists.isEmpty()) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            cartesianProductRecursive(lists, resultLists, 0, new ArrayList<>());
        }
        return resultLists;
    }

    private void cartesianProductRecursive(List<List<?>> lists,
        List<List<?>> result,
        int depth,
        List<Object> current) {
        if (depth == lists.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (Object value : lists.get(depth)) {
            current.add(value);
            cartesianProductRecursive(lists, result, depth + 1, current);
            current.removeLast();
        }
    }

    /**
     * A single test invocation context representing one specific combination
     * of values for all {@link PermutedParam} fields.
     * <p>
     * Responsible for injecting values into the test instance and generating
     * a human-readable display name based on the current permutation.
     */
    static class MCRPermutationInvocationContext implements TestTemplateInvocationContext {
        private final List<Field> permutedFields;
        private final List<?> values;

        MCRPermutationInvocationContext(List<Field> permutedFields, List<?> values) {
            this.permutedFields = permutedFields;
            this.values = values;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            StringBuilder displayName = new StringBuilder();
            for (int i = 0; i < permutedFields.size(); i++) {
                displayName.append(permutedFields.get(i).getName())
                    .append("=")
                    .append(values.get(i));
                if (i < permutedFields.size() - 1) {
                    displayName.append(", ");
                }
            }
            return displayName.toString();
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return List.of((TestInstancePostProcessor) (testInstance, context) -> {
                for (int i = 0; i < permutedFields.size(); i++) {
                    Field field = permutedFields.get(i);
                    field.setAccessible(true);
                    try {
                        field.set(testInstance, values.get(i));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject field " + field.getName(), e);
                    }
                }
            });
        }
    }

}
