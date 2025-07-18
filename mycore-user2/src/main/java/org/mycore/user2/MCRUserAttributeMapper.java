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
package org.mycore.user2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.annotation.MCRUserAttribute;
import org.mycore.user2.annotation.MCRUserAttributeJavaConverter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * This class is used to map attributes on {@link MCRUser} or {@link MCRUserInformation}
 * to annotated properties or methods.
 * <br><br>
 * You can configure the mapping within <code>realms.xml</code> like this:
 * <br>
 * <pre>
 * <code>
 *  &lt;realms local="local"&gt;
 *      ...
 *      &lt;realm ...&gt;
 *          ...
 *          &lt;attributeMapping&gt;
 *              &lt;attribute name="userName" mapping="eduPersonPrincipalName" /&gt;
 *              &lt;attribute name="realName" mapping="displayName" /&gt;
 *              &lt;attribute name="eMail" mapping="mail" /&gt;
 *              &lt;attribute name="roles" mapping="eduPersonAffiliation" separator=","
 *                  converter="org.mycore.user2.utils.MCRRolesConverter"&gt;
 *                  &lt;valueMapping name="employee"&gt;editor&lt;/valueMapping&gt;
 *              &lt;/attribute&gt;
 *          &lt;/attributeMapping&gt;
 *          ...
 *      &lt;/realm&gt;
 *      ...
 *  &lt;/realms&gt;
 * </code>
 * </pre>
 *
 * @author René Adler (eagle)
 *
 */
public class MCRUserAttributeMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Map<String, List<Attribute>> attributeMapping = new HashMap<>();

    public static MCRUserAttributeMapper createInstance(Element attributeMapping) {
        try {
            JAXBContext jaxb = JAXBContext.newInstance(Mappings.class.getPackage().getName(),
                Thread.currentThread().getContextClassLoader());

            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            Mappings mappings = (Mappings) unmarshaller.unmarshal(new JDOMSource(attributeMapping));

            MCRUserAttributeMapper uam = new MCRUserAttributeMapper();
            uam.attributeMapping.putAll(mappings.getAttributeMap());
            return uam;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Maps configured attributes to {@link Object}.
     *
     * @param object the {@link Object}
     * @param attributes a collection of attributes to map
     * @return <code>true</code> if any attribute was changed
     */
    public boolean mapAttributes(final Object object, final Map<String, ?> attributes) throws Exception {
        boolean changed = false;
        for (Object annotated : getAnnotated(object)) {
            MCRUserAttribute attrAnno = retrieveMCRUserAttribute(annotated);
            if (attrAnno == null) {
                continue;
            }
            final String name = attrAnno.name().isEmpty() ? getAttriutebName(annotated) : attrAnno.name();
            final List<Attribute> attribs = attributeMapping.get(name);
            if (attributes == null) {
                continue;
            }
            for (Attribute attribute : attribs) {
                if (!attributes.containsKey(attribute.mapping)) {
                    continue;
                }
                Object value = attributes.get(attribute.mapping);
                if (value == null) {
                    LOGGER.warn("Could not apply mapping for {}", attribute.mapping);
                }
                value = convertValue(annotated, attrAnno, attribute, value);
                if (!isValueValid(attrAnno, attribute, value)) {
                    throw new IllegalArgumentException(
                        "A not nullable attribute \"" + name + "\" was null.");
                }
                if (updateFieldOrMethod(object, annotated, attribute, value)) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private MCRUserAttribute retrieveMCRUserAttribute(Object annotated) {
        if (annotated instanceof Field field) {
            return field.getAnnotation(MCRUserAttribute.class);
        } else if (annotated instanceof Method method) {
            return method.getAnnotation(MCRUserAttribute.class);
        }
        return null;
    }

    private Object convertValue(Object annotated, MCRUserAttribute attrAnno, Attribute attribute, Object value)
        throws Exception {
        MCRUserAttributeJavaConverter aConv = null;

        if (annotated instanceof Field field) {
            aConv = field.getAnnotation(MCRUserAttributeJavaConverter.class);
        } else if (annotated instanceof Method method) {
            aConv = method.getAnnotation(MCRUserAttributeJavaConverter.class);
        }

        Class<? extends MCRUserAttributeConverter> convCls = getConverterClass(attribute, aConv);
        if (convCls != null) {
            MCRUserAttributeConverter converter = convCls.getDeclaredConstructor().newInstance();
            LOGGER.debug("convert value \"{}\" with \"{}\"", () -> value, () -> converter.getClass().getName());
            return converter.convert(value, attribute.separator != null ? attribute.separator : attrAnno.separator(),
                attribute.getValueMap());
        }
        return value;
    }

    private Class<? extends MCRUserAttributeConverter> getConverterClass(Attribute attribute,
        MCRUserAttributeJavaConverter aConv) throws ClassNotFoundException {
        if (attribute.converter != null) {
            return (Class<? extends MCRUserAttributeConverter>) Class.forName(attribute.converter);
        } else if (aConv != null) {
            return aConv.value();
        }
        return null;
    }

    private boolean isValueValid(MCRUserAttribute attrAnno, Attribute attribute, Object value) {
        return value != null || ((attrAnno.nullable() || attribute.nullable) && value == null);
    }

    private boolean updateFieldOrMethod(Object object, Object annotated, Attribute attribute, Object value)
        throws Exception {
        Object oldValue = getValue(object, annotated);
        if (oldValue != null && oldValue.equals(value)) {
            return false;
        }

        if (annotated instanceof Field field) {
            LOGGER.debug("map attribute \"{}\" with value \"{}\" to field \"{}\"",
                () -> attribute.mapping, () -> value, field::getName);
            field.setAccessible(true);
            field.set(object, value);
        } else if (annotated instanceof Method method) {
            LOGGER.debug("map attribute \"{}\" with value \"{}\" to method \"{}\"",
                () -> attribute.mapping, () -> value, method::getName);
            method.setAccessible(true);
            method.invoke(object, value);
        }
        return true;
    }

    /**
     * Returns a collection of mapped attribute names.
     *
     * @return a collection of mapped attribute names
     */
    public Set<String> getAttributeNames() {
        Set<String> mAtt = new HashSet<>();

        for (final String name : attributeMapping.keySet()) {
            attributeMapping.get(name).forEach(a -> mAtt.add(a.mapping));
        }

        return mAtt;
    }

    private List<Object> getAnnotated(final Object obj) {
        List<Object> al = new ArrayList<>();

        al.addAll(getAnnotatedFields(obj.getClass()));
        al.addAll(getAnnotatedMethods(obj.getClass()));

        if (obj.getClass().getSuperclass() != null) {
            al.addAll(getAnnotatedFields(obj.getClass().getSuperclass()));
            al.addAll(getAnnotatedMethods(obj.getClass().getSuperclass()));
        }

        return al;
    }

    private List<Object> getAnnotatedFields(final Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
            .filter(field -> field.getAnnotation(MCRUserAttribute.class) != null)
            .collect(Collectors.toList());
    }

    private List<Object> getAnnotatedMethods(final Class<?> cls) {
        return Arrays.stream(cls.getDeclaredMethods())
            .filter(method -> method.getAnnotation(MCRUserAttribute.class) != null)
            .collect(Collectors.toList());
    }

    private String getAttriutebName(final Object annotated) {
        if (annotated instanceof Field field) {
            return field.getName();
        } else if (annotated instanceof Method method) {
            String name = method.getName();
            if (name.startsWith("set")) {
                name = name.substring(3);
            }
            return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
        }

        return null;
    }

    private Object getValue(final Object object, final Object annotated) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Object value = null;

        if (annotated instanceof Field field) {

            field.setAccessible(true);
            value = field.get(object);
        } else if (annotated instanceof Method annMethod) {
            Method method = null;
            String name = annMethod.getName();
            if (name.startsWith("get")) {
                name = "s" + name.substring(1);
                method = object.getClass().getMethod(name);
            }

            if (method != null) {
                method.setAccessible(true);
                value = method.invoke(object);
            }
        }

        return value;
    }

    @XmlRootElement(name = "realm")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static final class Mappings {
        @XmlElementWrapper(name = "attributeMapping")
        @XmlElement(name = "attribute")
        List<Attribute> attributes;

        Map<String, List<Attribute>> getAttributeMap() {
            return attributes.stream().collect(Collectors.groupingBy(attrib -> attrib.name));
        }
    }

    @XmlRootElement(name = "attribute")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static final class Attribute {
        @XmlAttribute(required = true)
        String name;

        @XmlAttribute(required = true)
        String mapping;

        @XmlAttribute
        String separator;

        @XmlAttribute
        boolean nullable;

        @XmlAttribute
        String converter;

        @XmlElement
        List<ValueMapping> valueMapping;

        Map<String, String> getValueMap() {
            if (valueMapping == null) {
                return null;
            }

            Map<String, String> map = new HashMap<>();
            for (ValueMapping vm : valueMapping) {
                map.put(vm.name, vm.mapping);
            }
            return map;
        }
    }

    @XmlRootElement(name = "valueMapping")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static final class ValueMapping {
        @XmlAttribute(required = true)
        String name;

        @XmlValue
        String mapping;
    }
}
