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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jdom2.Element;
import org.mycore.user2.annotation.MCRUserAttribute;
import org.mycore.user2.annotation.MCRUserAttributeJavaConverter;

/**
 * This attribute mapper is used to map attributes to {@link Object} properties or methods.
 * <br><br>
 * You can configure the mapping within <code>realms.xml</code> like this:
 * <br>
 * <pre>
 * <code>
 *  &lt;realms local="local"&gt;
 *      ...
 *      &lt;realm ...&gt;
 *          &lt;attributeMapping&gt;
 *              &lt;attribute name="userName"&gt;eduPersonPrincipalName&lt;/attribute&gt;
 *              &lt;attribute name="realName"&gt;displayName&lt;/attribute&gt;
 *              &lt;attribute name="eMail"&gt;mail&lt;/attribute&gt;
 *              &lt;attribute name="systemRoles" separator=","&gt;eduPersonAffiliation&lt;/attribute&gt;
 *              &lt;attribute name="externalRoles" separator="," 
 *                  converter="org.mycore.user2.utils.MCRExternalRolesConverter"&gt;eduPersonAffiliation&lt;/attribute&gt;
 *          &lt;/attributeMapping&gt;
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

    private HashMap<String, Attribute> attributeMapping = new HashMap<String, Attribute>();

    public static MCRUserAttributeMapper instance(Element attributeMapping) {
        MCRUserAttributeMapper uam = new MCRUserAttributeMapper();

        for (Element child : attributeMapping.getChildren("attribute")) {
            final String name = child.getAttributeValue("name");
            if (name != null && !name.isEmpty()) {
                Attribute attr = new Attribute();

                attr.mapping = child.getTextTrim();
                attr.separator = child.getAttributeValue("separator");
                attr.nullable = Boolean.getBoolean(child.getAttributeValue("nullable"));
                attr.converter = child.getAttributeValue("converter");

                uam.attributeMapping.put(name, attr);
            }
        }

        return uam;
    }

    /**
     * Maps configured attributes to {@link Object}.
     * 
     * @param object the {@link Object}
     * @param attributes a collection of attributes to map
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void mapAttributes(final Object object, final Map<String, ?> attributes) throws Exception {
        for (Object annotated : getAnnotated(object)) {
            MCRUserAttribute attrAnno = null;

            if (annotated instanceof Field) {
                attrAnno = ((Field) annotated).getAnnotation(MCRUserAttribute.class);
            } else if (annotated instanceof Method) {
                attrAnno = ((Method) annotated).getAnnotation(MCRUserAttribute.class);
            }

            if (attrAnno != null) {
                final String name = attrAnno.name().isEmpty() ? getAttriutebName(annotated) : attrAnno.name();
                final Attribute attribute = attributeMapping.get(name);

                if (attribute != null) {
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
                            convCls = (Class<? extends MCRUserAttributeConverter>) Class.forName(attribute.converter);
                        } else if (aConv != null) {
                            convCls = aConv.value();
                        }

                        if (convCls != null) {
                            MCRUserAttributeConverter converter = convCls.newInstance();
                            value = converter.convert(value, attribute.separator);
                        }

                        if (value != null || ((attrAnno.nullable() || attribute.nullable) && value == null)) {
                            if (annotated instanceof Field) {
                                final Field field = (Field) annotated;

                                boolean accState = field.isAccessible();
                                field.setAccessible(true);
                                field.set(object, value);
                                field.setAccessible(accState);
                            } else if (annotated instanceof Method) {
                                final Method method = (Method) annotated;

                                boolean accState = method.isAccessible();
                                method.setAccessible(true);
                                method.invoke(object, value);
                                method.setAccessible(accState);
                            }
                        } else {
                            throw new IllegalArgumentException("A not nullable attribute \"" + name + "\" was null.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a collection of mapped attributes.
     * 
     * @return a collection of mapped attributes
     */
    public Set<String> getMappedAttributes() {
        Set<String> mAtt = new HashSet<String>();

        for (final String name : attributeMapping.keySet()) {
            mAtt.add(attributeMapping.get(name).mapping);
        }

        return mAtt;
    }

    private List<Object> getAnnotated(final Object obj) {
        List<Object> al = new ArrayList<Object>();

        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.getAnnotation(MCRUserAttribute.class) != null) {
                al.add(field);
            }
        }

        for (Method method : obj.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(MCRUserAttribute.class) != null) {
                al.add(method);
            }
        }

        return al;
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

    private static class Attribute {
        String mapping;

        String separator;

        boolean nullable;

        String converter;
    }

}
