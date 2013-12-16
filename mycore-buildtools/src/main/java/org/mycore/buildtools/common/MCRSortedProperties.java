/*
 * $Revision: 21452 $ 
 * $Date: 2011-07-13 10:39:39 +0200 (Mi, 13 Jul 2011) $
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
package org.mycore.buildtools.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/**
 * This class enhances the Java Properties class.
 * If the properties are stored, they will be sorted by key names.
 * Additionally if properties are in this form:
 * <code>key=%key%,value</code>
 * old values will NOT be overwritten but the new value will be appended.
 * 
 * @author Thomas Scheffler (yagee)
 * @author R. Adler
 */
public class MCRSortedProperties extends Properties {
    private static final long serialVersionUID = 1L;

    @Override
    public synchronized Object put(Object key, Object value) {
        return putString((String) key, (String) value);
    }

    private Object putString(String key, String value) {
        String oldValue = (String) super.getProperty(key);
        String newValue = oldValue == null ? value : value.replaceAll('%' + key + '%', oldValue);
        if (!newValue.equals(value) && newValue.startsWith(",")) {
            //replacement took place, but starts with 'empty' value
            newValue = newValue.substring(1);
        }
        return super.put(key, newValue);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
    }
}
