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

package org.mycore.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfiguration2;

public final class MCRScopedSession extends MCRSession {

    private ThreadLocal<ScopedValues> scopedValues = new ThreadLocal<>();

    public <T> T doAs(ScopedValues scopeValue, Supplier<T> action) {
        scopedValues.set(Objects.requireNonNull(scopeValue));
        try {
            return action.get();
        } finally {
            scopedValues.remove();
        }
    }

    @Override
    public Locale getLocale() {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.getLocale();
        }
        return values.locale();
    }

    @Override
    public void setUserInformation(MCRUserInformation userSystemAdapter) {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            super.setUserInformation(userSystemAdapter);
        } else {
            throw new IllegalArgumentException(
                "User information should not be set when running in restricted scope: " + values);
        }
    }

    @Override
    public Object get(Object key) {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.get(key);
        }
        return values.map.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.put(key, value);
        }
        return values.map.put(key, value);
    }

    @Override
    public String getCurrentIP() {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.getCurrentIP();
        }
        return values.ip();
    }

    @Override
    public void close() {
        scopedValues.remove();
        super.close();
    }

    @Override
    public MCRUserInformation getUserInformation() {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.getUserInformation();
        }
        return values.userInformation();
    }

    @Override
    public Iterator<Object> getObjectsKeyList() {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.getObjectsKeyList();
        }
        return values.map.keySet().stream().collect(Collectors.toSet()).iterator();
    }

    @Override
    public List<Map.Entry<Object, Object>> getMapEntries() {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            return super.getMapEntries();
        }
        return values.map.entrySet().stream().toList();
    }

    @Override
    public void deleteObject(Object key) {
        ScopedValues values = scopedValues.get();
        if (values == null) {
            super.deleteObject(key);
        } else {
            values.map.remove(key);
        }
    }

    public record ScopedValues(
        Map<Object, Object> map,
        MCRUserInformation userInformation,
        String language,
        Locale locale,
        String ip) {

        public ScopedValues(MCRUserInformation userInformation, String ip) {
            this(new HashMap<>(), userInformation, getDefaultLang(), getDefaultLocale(), ip);
        }

        private static String getDefaultLang() {
            return MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG);
        }

        private static Locale getDefaultLocale() {
            return Locale.forLanguageTag(getDefaultLang());
        }
    }

}
