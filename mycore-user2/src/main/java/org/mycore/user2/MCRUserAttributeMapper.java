/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRUserInformation;
import org.mycore.user2.annotation.MCRUserAttribute;
import org.mycore.user2.annotation.MCRUserAttributeJavaConverter;

/**
 * This class is used to map attributes on {@link MCRUser} or {@link MCRUserInformation} to annotated properties or methods.
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
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRUserAttributeMapper {

    private static Logger LOGGER = LogManager.getLogger(MCRUserAttributeMapper.class);

    private HashMap<String, List<Attribute>> attributeMapping = new HashMap<String, List<Attribute>>();

    public static MCRUserAttributeMapper instance(Element attributeMapping) {
        try {
            JAXBContext jaxb = JAXBContext.newInstance(Mappings.class.getPackage().getName(),
                Mappings.class.getClassLoader());

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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean mapAttributes(final Object object, final Map<String, ?> attributes) throws Exception {
        boolean changed = false;

        for (Object annotated : getAnnotated(object)) {
            MCRUserAttribute attrAnno = null;

            if (annotated instanceof Field) {
                attrAnno = ((Field) annotated).getAnnotation(MCRUserAttribute.class);
            } else if (annotated instanceof Method) {
                attrAnno = ((Method) annotated).getAnnotation(MCRUserAttribute.class);
            }

            if (attrAnno != null) {
                final String name = attrAnno.name().isEmpty() ? getAttriutebName(annotated) : attrAnno.name();
                final List<Attribute> attribs = attributeMapping.get(name);

                if (attributes != null) {
                    for (Attribute attribute : attribs) {
                        if (attributes.containsKey(attribute.mapping)) {
                            Object value = attributes.get(attribute.mapping);

                            MCRUserAttributeJavaConverter aConv = null;

                            if (annotated instanceof Field) {
                                aConv = ((Field) annotated).getAnnotation(MCRUserAttributeJavaConverter.class);
                            } else if (annotated instanceof Method) {
                                aConv = ((Method) annotated).getAnnotation(MCRUserAttributeJavaConverter.class);
                            }

                            Class<? extends MCRUserAttributeConverter> convCls = null;
                            if (attribute.converter != null) {
                                convCls = (Class<? extends MCRUserAttributeConverter>) Class
                                    .forName(attribute.converter);
                            } else if (aConv != null) {
                                convCls = aConv.value();
                            }

                            if (convCls != null) {
                                MCRUserAttributeConverter converter = convCls.newInstance();
                                LOGGER.debug(
                                    "convert value \"" + value + "\" with \"" + converter.getClass().getName()
                                        + "\"");
                                value = converter.convert(value,
                                    attribute.separator != null ? attribute.separator : attrAnno.separator(),
                                    attribute.getValueMap());
                            }

                            if (value != null || ((attrAnno.nullable() || attribute.nullable) && value == null)) {
                                Object oldValue = getValue(object, annotated);
                                if (oldValue != null && oldValue.equals(value))
                                    continue;

                                if (annotated instanceof Field) {
                                    final Field field = (Field) annotated;

                                    LOGGER.debug("map attribute \"" + attribute.mapping + "\" with value \""
                                        + value.toString() + "\" to field \"" + field.getName() + "\"");

                                    boolean accState = field.isAccessible();
                                    field.setAccessible(true);
                                    field.set(object, value);
                                    field.setAccessible(accState);

                                    changed = true;
                                } else if (annotated instanceof Method) {
                                    final Method method = (Method) annotated;

                                    LOGGER.debug("map attribute \"" + attribute.mapping + "\" with value \""
                                        + value.toString() + "\" to method \"" + method.getName() + "\"");

                                    boolean accState = method.isAccessible();
                                    method.setAccessible(true);
                                    method.invoke(object, value);
                                    method.setAccessible(accState);

                                    changed = true;
                                }
                            } else {
                                throw new IllegalArgumentException(
                                    "A not nullable attribute \"" + name + "\" was null.");
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Returns a collection of mapped attribute names.
     * 
     * @return a collection of mapped attribute names
     */
    public Set<String> getAttributeNames() {
        Set<String> mAtt = new HashSet<String>();

        for (final String name : attributeMapping.keySet()) {
            attributeMapping.get(name).forEach(a -> mAtt.add(a.mapping));
        }

        return mAtt;
    }

    private List<Object> getAnnotated(final Object obj) {
        List<Object> al = new ArrayList<Object>();

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
        if (annotated instanceof Field) {
            return ((Field) annotated).getName();
        } else if (annotated instanceof Method) {
            String name = ((Method) annotated).getName();
            if (name.startsWith("set"))
                name = name.substring(3);
            return name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
        }

        return null;
    }

    private Object getValue(final Object object, final Object annotated) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Object value = null;

        if (annotated instanceof Field) {
            final Field field = (Field) annotated;

            boolean accState = field.isAccessible();
            field.setAccessible(true);
            value = field.get(object);
            field.setAccessible(accState);
        } else if (annotated instanceof Method) {
            Method method = null;
            String name = ((Method) annotated).getName();
            if (name.startsWith("get")) {
                name = "s" + name.substring(1);
                method = object.getClass().getMethod(name);
            }

            if (method != null) {
                boolean accState = method.isAccessible();
                method.setAccessible(true);
                value = method.invoke(object);
                method.setAccessible(accState);
            }
        }

        return value;
    }

    @XmlRootElement(name = "realm")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Mappings {
        @XmlElementWrapper(name = "attributeMapping")
        @XmlElement(name = "attribute")
        List<Attribute> attributes;

        Map<String, List<Attribute>> getAttributeMap() {
            return attributes.stream().collect(Collectors.groupingBy(attrib -> attrib.name));
        }
    }

    @XmlRootElement(name = "attribute")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class Attribute {
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
            if (valueMapping == null)
                return null;

            Map<String, String> map = new HashMap<String, String>();
            for (ValueMapping vm : valueMapping) {
                map.put(vm.name, vm.mapping);
            }
            return map;
        }
    }

    @XmlRootElement(name = "valueMapping")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class ValueMapping {
        @XmlAttribute(required = true)
        String name;

        @XmlValue
        String mapping;
    }
}
