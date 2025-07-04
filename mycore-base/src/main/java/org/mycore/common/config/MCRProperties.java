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

package org.mycore.common.config;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;

/**
 * Like {@link Properties} but with in-place replacement of properties that want to append a value.
 * 
 * Properties for System.getProperties() have always precedence for Properties defined here.
 * 
 * <pre>
 * key=value1
 * key=%key%,value2
 * </pre>
 * 
 * will be resolved to
 * 
 * <pre>
 * key=value1,value2
 * </pre>
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRProperties extends Properties {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Object put(Object key, Object value) {
        return putString((String) key, (String) value);
    }

    private Object putString(String key, String value) {
        String systemProperty = System.getProperties().getProperty(key);
        if (systemProperty != null && !systemProperty.equals(value)) {
            LogManager.getLogger(getClass()).error("Cannot overwrite system property: {}={}", key, value);
            return systemProperty;
        }
        String oldValue = (String) super.get(key);
        String newValue = oldValue == null ? value : value.replaceAll('%' + key + '%', oldValue);
        if (!newValue.equals(value) && newValue.startsWith(",")) {
            //replacement took place, but starts with 'empty' value
            newValue = newValue.substring(1);
        }
        return super.put(key, newValue);
    }

    @Override
    public synchronized Object get(Object key) {
        String systemProperty = System.getProperties().getProperty((String) key);
        return systemProperty != null ? systemProperty : super.get(key);
    }

    Map<String, String> getAsMap() {
        @SuppressWarnings("rawtypes")
        Map compileFix = this;
        @SuppressWarnings("unchecked")
        Map<String, String> returns = compileFix;
        return returns;
    }

    /**
     * Creates a new <code>MCRProperties</code> instance with the values
     * of the given properties.
     */
    public static MCRProperties ofProperties(Properties properties) {
        MCRProperties p = new MCRProperties();
        p.putAll(properties);
        return p;
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        toSortedProperties().store(out, comments);
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        toSortedProperties().store(writer, comments);
    }

    @Override
    public void storeToXML(OutputStream os, String comment, Charset charset) throws IOException {
        toSortedProperties().storeToXML(os, comment, charset);
    }

    private Properties toSortedProperties() {
        Properties sortedProps = new Properties() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                Set<Map.Entry<Object, Object>> sortedSet = new TreeSet<>(
                    Comparator.comparing(o -> o.getKey().toString()));
                sortedSet.addAll(super.entrySet());
                return sortedSet;
            }

            @Override
            public Set<Object> keySet() {
                return new TreeSet<>(super.keySet());
            }

            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(super.keySet()));
            }

        };
        sortedProps.putAll(this);
        return sortedProps;
    }
}
