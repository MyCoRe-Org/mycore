/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 6, 2013 $
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

package org.mycore.common.config;

import java.util.Map;
import java.util.Properties;

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

    private static final long serialVersionUID = 8801587133852810123L;

    @Override
    public synchronized Object put(Object key, Object value) {
        return putString((String) key, (String) value);
    }

    private Object putString(String key, String value) {
        String systemProperty = System.getProperties().getProperty(key);
        if (systemProperty != null && !systemProperty.equals(value)) {
            LogManager.getLogger(getClass()).error("Cannot overwrite system property: " + key + "=" + value);
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
    public static MCRProperties copy(Properties properties) {
        MCRProperties p = new MCRProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            p.put(entry.getKey(), entry.getValue());
        }
        return p;
    }

}
