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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Creates a configured instance of a class extending .
     * After creating the method with the {@link MCRPostConstruction} annotation is called with the property as
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

    public static String getIDFromClassProperty(String property){
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

    private static <T> void processPostConstruction(T newInstance, String property) {
        final Method[] methods = newInstance.getClass().getMethods();
        for (Method method : methods) {
            MCRPostConstruction annot = method.getAnnotation(MCRPostConstruction.class);
            if (annot != null) {
                try {
                    method.invoke(newInstance, property);
                } catch (IllegalAccessException e) {
                    throwInaccessibleMember(newInstance, method.getName(), e);
                } catch (InvocationTargetException e) {
                    throw new MCRException("The annotated method " + method.getName() + " errored!", e);
                }
            }
        }
    }

    private static void processAnnotatedFields(Object object, String classProperty, Map<String, String> properties) {
        final Field[] fields = object.getClass().getFields();
        for (Field field : fields) {
            final MCRProperty annot = field.getAnnotation(MCRProperty.class);
            if (annot != null) {
                final String propertyName = annot.name();
                final boolean required = annot.required();
                boolean absolute = annot.absolute();

                if (propertyName.equals("*")) {
                    try {
                        field.set(object, properties);
                    } catch (IllegalAccessException e) {
                        throwInaccessibleMember(object, field.getName(), e);
                        return;
                    }
                } else {
                    final Optional<String> value = absolute ? MCRConfiguration2.getString(propertyName)
                        : Optional.ofNullable(properties.get(propertyName));

                    if (value.isEmpty() && required) {
                        throw new MCRConfigurationException("The required property " + propertyName
                            + " of the class "
                            + ((classProperty != null) ? "configured in " + classProperty
                                : object.getClass().toString())
                            + " is not set!");
                    } else if (value.isPresent()) {
                        try {
                            field.set(object, value.get());
                        } catch (IllegalAccessException e) {
                            throwInaccessibleMember(object, field.getName(), e);
                            return;
                        }
                    }
                }
            }

        }
    }

    private static void throwInaccessibleMember(Object object, String memberName, IllegalAccessException e) {
        throw new MCRException("The annotated field " + memberName + " is not accessible in "
            + object.getClass().toString(), e);
    }

    private static void processAnnotatedMethods(Object object, String classProperty, Map<String, String> properties) {
        final Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            final MCRProperty annot = method.getAnnotation(MCRProperty.class);
            if (annot != null) {
                final String propertyName = annot.name();
                final boolean required = annot.required();
                boolean absolute = annot.absolute();

                if (propertyName.equals("*")) {
                    try {
                        method.invoke(object, properties);
                    } catch (IllegalAccessException e) {
                        throwInaccessibleMember(object, method.getName(), e);
                        return;
                    } catch (InvocationTargetException e) {
                        throw new MCRException("The annotated method " + method.getName() + " errored!", e);
                    }
                } else {
                    final Optional<String> value = absolute ? MCRConfiguration2.getString(propertyName)
                        : Optional.ofNullable(properties.get(propertyName));
                    if (value.isEmpty() && required) {
                        throw new MCRConfigurationException("The required property " + propertyName
                            + " of the Configured Class " +
                            ((classProperty != null) ? classProperty : object.getClass().toString())
                            + " is not set!");
                    } else if (value.isPresent()) {
                        try {
                            method.invoke(object, value.get());
                        } catch (IllegalAccessException e) {
                            throw new MCRException(
                                "The annotated method " + method.getName() + " is not accessible in "
                                    + object.getClass(),
                                e);
                        } catch (InvocationTargetException e) {
                            throw new MCRException("The annotated method " + method.getName() + " errored!", e);
                        }
                    }
                }
            }
        }
    }

}
