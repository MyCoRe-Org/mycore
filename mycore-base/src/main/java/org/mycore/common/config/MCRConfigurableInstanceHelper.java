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

package org.mycore.common.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.inject.Singleton;

/**
 * Creates Objects which are preconfigured with properties.
 *
 * Example Configuration<br>

 * @author Sebastian Hofmann
 */
class MCRConfigurableInstanceHelper {

    public static final Comparator<Annotated<MCRProperty, ?>> PROPERTY_COMPARATOR =
        Comparator.comparingInt(left -> left.annotation.order());
    public static final Comparator<Annotated<MCRPostConstruction, ?>> POST_CONTRUCTION_COMPARATOR =
        Comparator.comparingInt(left -> left.annotation.order());

    /**
     * Creates a configured instance of a class extending .
     * After creating the methods with the {@link MCRPostConstruction} annotation are called with the property as
     * parameter.
      *@param property the property which contains the class
     * @return a not null instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <T> Optional<T> getInstance(String property) throws MCRConfigurationException {
        final Map<String, String> properties = getClassProperties(property);
        return MCRConfiguration2.getString(property)
            .map(propertyVal -> {
                Class<T> tClass;
                try {
                    tClass = (Class<T>) MCRClassTools.forName(propertyVal);
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException("The configurable instance has a not existing class " +
                        "(" + propertyVal + ") configured " + property, e);
                }
                return getInstance(tClass, properties, property);
            });
    }

    /**
     * checks if a class is annotated with @{@link Singleton}
     * @param property the property which contains the class
     * @return true if the class in the property is annotated with Singleton
     */
    public static boolean isSingleton(String property) {
        return MCRConfiguration2.getString(property)
            .stream().anyMatch(propertyVal -> {
                try {
                    Singleton declaredAnnotation = MCRClassTools.forName(propertyVal)
                        .getAnnotation(Singleton.class);
                    return declaredAnnotation != null;
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException("The configurable instance has a not existing class " +
                        "(" + propertyVal + ") configured " + property, e);
                }
            });
    }

    public static String getIDFromClassProperty(String property) {
        String propertyWOClass = property;
        if (property.endsWith(".Class") || property.endsWith(".class")) {
            propertyWOClass = property.substring(0, property.length() - "Class".length());
        }

        int lastDotIndex = propertyWOClass.lastIndexOf('.');
        return property.substring(lastDotIndex);
    }

    private static Map<String, String> getClassProperties(String property) {
        final Map<String, String> properties;
        if (property.endsWith(".Class") || property.endsWith(".class")) {
            properties = MCRConfiguration2
                .getSubPropertiesMap(property.substring(0, property.length() - "Class".length()));
            properties.remove("Class");
            properties.remove("class");
        } else {
            properties = MCRConfiguration2.getSubPropertiesMap(property + ".");
        }
        return properties;
    }

    public static <T> T getInstance(Class<T> cl, Map<String, String> properties, String property) {
        T newInstance;

        try {
            newInstance = cl.getConstructor().newInstance();
        } catch (Exception e) {
            // no default constructor, check for singleton factory method
            try {
                newInstance = (T) Stream.of(cl.getMethods())
                    .filter(m -> m.getReturnType().isAssignableFrom(cl))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains("instance"))
                    .findAny()
                    .orElseThrow(() -> new MCRConfigurationException("Could not instantiate class " + cl.getName(), e))
                    .invoke(cl, (Object[]) null);
            } catch (ReflectiveOperationException r) {
                throw new MCRConfigurationException("Could not instantiate class " + cl.getName(), r);
            }
        }

        processAnnotatedFields(newInstance, property, properties);
        processAnnotatedMethods(newInstance, property, properties);
        processPostConstruction(newInstance, property);

        return newInstance;
    }

    private static void processAnnotatedField(Object object, String classProperty, Map<String, String> properties,
                                             Annotated<MCRProperty, Field> annotatedField) {

        final MCRProperty annotation = annotatedField.annotation;
        final Field field = annotatedField.payload;

        final String defaultPropertyName = annotation.defaultName();
        final String propertyName = annotation.name();

        if (Objects.equals(propertyName, "*")) {
            setFieldValue(object, field, Map.class, properties);
        } else {
            final Optional<String> value = getValue(properties, annotation);
            if (value.isPresent()) {
                setFieldValue(object, field, String.class, value.get());
            } else if (!Objects.equals(defaultPropertyName, "")) {
                String defaultValue = MCRConfiguration2.getStringOrThrow(defaultPropertyName);
                setFieldValue(object, field, String.class, defaultValue);
            } else if (annotation.required()) {
                throw new MCRConfigurationException("The required property " + propertyName
                    + " of the class "
                    + ((classProperty != null) ? "configured in " + classProperty
                    : object.getClass().toString())
                    + " is not set!");
            }
        }

    }

    private static <T> void setFieldValue(Object object, Field field, Class<T> valueClass, T value) {
        try {
            if(field.getType() != valueClass) {
                throw invalidType(object, field, valueClass);
            }
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw inaccessibleField(object, field, e);
        }
    }

    private static void processAnnotatedMethods(Object object, String classProperty, Map<String, String> properties) {
        final Method[] methods = object.getClass().getMethods();
        final List<Annotated<MCRProperty, Method>> annotatedMethods = new ArrayList<>(methods.length);
        for (Method method : methods) {
            final MCRProperty annotation = method.getAnnotation(MCRProperty.class);
            if (annotation != null) {
                annotatedMethods.add(new Annotated<>(annotation, method));
            }
        }
        annotatedMethods.sort(PROPERTY_COMPARATOR);
        for (Annotated<MCRProperty, Method> annotatedMethod : annotatedMethods) {
            processAnnotatedMethod(object, classProperty, properties, annotatedMethod);
        }
    }

    private static void processAnnotatedMethod(Object object, String classProperty, Map<String, String> properties,
                                               Annotated<MCRProperty, Method> annotatedMethod) {

        final MCRProperty annotation = annotatedMethod.annotation;
        final Method method = annotatedMethod.payload;

        final String defaultPropertyName = annotation.defaultName();
        final String propertyName = annotation.name();

        if (Objects.equals(propertyName, "*")) {
            setMethodValue(object, method, Map.class, properties);
        } else {
            final Optional<String> value = getValue(properties, annotation);
            if (value.isPresent()) {
                setMethodValue(object, method, String.class, value.get());
            } else if (!Objects.equals(defaultPropertyName, "")) {
                String defaultValue = MCRConfiguration2.getStringOrThrow(defaultPropertyName);
                setMethodValue(object, method, String.class, defaultValue);
            } else if (annotation.required()) {
                throw new MCRConfigurationException("The required property " + propertyName
                    + " of the Configured Class " +
                    ((classProperty != null) ? classProperty : object.getClass().toString())
                    + " is not set!");
            }
        }

    }

    private static <T> void setMethodValue(Object object, Method method, Class<T> valueClass, T value) {
        try {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {
                if (parameterTypes[0] != valueClass) {
                    throw invalidParameterTypes(object, method, valueClass);
                }
                method.invoke(object, value);
            } else {
                throw invalidParameterTypes(object, method, valueClass);
            }
        } catch (IllegalAccessException e) {
            throw inaccessibleMethod(object, method, e);
        } catch (InvocationTargetException e) {
            throw new MCRException("The annotated method " + method.getName() + " errored!", e);
        }
    }

    private static void processPostConstruction(Object object, String property) {
        final Method[] methods = object.getClass().getMethods();
        final List<Annotated<MCRPostConstruction, Method>> annotatedMethods = new ArrayList<>(methods.length);
        for (Method method : methods) {
            final MCRPostConstruction annotation = method.getAnnotation(MCRPostConstruction.class);
            if (annotation != null) {
                annotatedMethods.add(new Annotated<>(annotation, method));
            }
        }
        annotatedMethods.sort(POST_CONTRUCTION_COMPARATOR);
        for (Annotated<MCRPostConstruction, Method> annotatedMethod : annotatedMethods) {
            processPostConstruction(object, property, annotatedMethod);
        }
    }

    private static void processPostConstruction(Object object, String property,
                                                Annotated<MCRPostConstruction, Method> annotatedMethod) {

        final Method method = annotatedMethod.payload;

        try {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {
                method.invoke(object);
            } else if (parameterTypes.length == 1) {
                if (parameterTypes[0] != String.class) {
                    throw invalidParameterTypes(object, method, String.class);
                }
                method.invoke(object, property);
            } else {
                throw invalidParameterTypes(object, method, String.class);
            }
        } catch (IllegalAccessException e) {
            throw inaccessibleMethod(object, method, e);
        } catch (InvocationTargetException e) {
            throw new MCRException("The annotated method " + method.getName() + " errored!", e);
        }
    }

    private static void processAnnotatedFields(Object object, String classProperty, Map<String, String> properties) {
        final Field[] fields = object.getClass().getFields();
        final List<Annotated<MCRProperty, Field>> annotatedFields = new ArrayList<>(fields.length);
        for (Field field : fields) {
            final MCRProperty annotation = field.getAnnotation(MCRProperty.class);
            if (annotation != null) {
                annotatedFields.add(new Annotated<>(annotation, field));
            }
        }
        annotatedFields.sort(PROPERTY_COMPARATOR);
        for (Annotated<MCRProperty, Field> annotatedField : annotatedFields) {
            processAnnotatedField(object, classProperty, properties, annotatedField);
        }

    }

    private static Optional<String> getValue(Map<String, String> properties, MCRProperty annotation) {
        final String propertyName = annotation.name();
        return annotation.absolute() ? MCRConfiguration2.getString(propertyName)
            : Optional.ofNullable(properties.get(propertyName));
    }

    private static MCRException inaccessibleField(Object object, Field field, IllegalAccessException e) {
        throw new MCRException("The annotated field " + field.getName() + " is not accessible in "
            + object.getClass().toString(), e);
    }

    private static MCRException inaccessibleMethod(Object object, Method method, IllegalAccessException e) {
        throw new MCRException("The annotated method " + method.getName() + " is not accessible in "
            + object.getClass().toString(), e);
    }

    private static MCRException invalidType(Object object, Field field, Class<?> expectedClass) {
        throw new MCRException("The annotated field " + field.getName() + " has the wrong type in "
            + object.getClass().toString() + ", expected " + expectedClass.getName());
    }

    private static MCRException invalidParameterTypes(Object object, Method method, Class<?>... expectedClasses) {
        String types = Arrays.stream(expectedClasses).map(Class::getName).collect(Collectors.joining(", "));
        throw new MCRException("The annotated method " + method.getName() + " has the wrong parameter types in "
            + object.getClass().toString() + ", expected " + types);
    }

    private record Annotated<Annotation, Payload>(Annotation annotation, Payload payload) {
    }

}
