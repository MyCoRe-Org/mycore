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

package org.mycore.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.mycore.common.config.MCRConfiguration2;

/**
 * A scoped session that allows to execute actions within a restricted scope.
 * <p>
 * This session is used to restrict the access to certain resources or information
 * of the current user. These information can be overwritten in the
 * {@link #doAs(ScopedValues, Supplier)} method.
 */
public final class MCRScopedSession extends MCRSession {

    private final ThreadLocal<ScopedValues> scopedValues = new ThreadLocal<>();

    /**
     * If running in a scoped context the session contains this key {@link MCRSession#get(Object)}.
     */
    public static final String SCOPED_HINT = MCRScopedSession.class.getCanonicalName();

    /**
     * Executes an action within a restricted scope.
     * <p>
     * The scoped values are set for the duration of the action and then removed.
     * This allows to execute actions that require a specific context or access to certain resources.
     * The base MCRSession is left untouched in all other running threads. Basically this
     * allows to overwrite most of the base MCRSession information while performing {@code action}
     * and using the same database transaction.
     *
     * @param scopeValues the scoped values to use during the execution of the action
     * @param action the action to be executed within the restricted scope
     */
    public void doAs(ScopedValues scopeValues, Runnable action) {
        doAs(scopeValues, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Executes an action within a restricted scope.
     * <p>
     * The scoped values are set for the duration of the action and then removed.
     * This allows to execute actions that require a specific context or access to certain resources.
     * The base MCRSession is left untouched in all other running threads. Basically this
     * allows to overwrite most of the base MCRSession information while performing {@code action}
     * and using the same database transaction.
     *
     * @param scopeValues the scoped values to use during the execution of the action
     * @param action the action to be executed within the restricted scope
     * @return the result of the action
     */
    public <T> T doAs(ScopedValues scopeValues, Supplier<T> action) {
        Objects.requireNonNull(scopeValues).map.put(SCOPED_HINT, scopeValues);
        scopedValues.set(scopeValues);
        try {
            return action.get();
        } finally {
            clearScopedValues();
        }
    }

    private void clearScopedValues() {
        ScopedValues values = scopedValues.get();
        try {
            if (values != null) {
                clearClosableValues(values.map);
            }
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
        clearScopedValues();
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
        return values.map.keySet().iterator();
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
        Locale locale,
        String ip) {

        private static final Locale DEFAULT_LOCALE = MCRConfiguration2
            .getString("MCR.Metadata.DefaultLang")
            .map(Locale::forLanguageTag)
            .orElseGet(() -> Locale.forLanguageTag(MCRConstants.DEFAULT_LANG));

        /**
         * Creates a new instance of ScopedValues with default values.
         * <p>
         * Default values are:
         * <ul>
         *     <li><em>empty</em> map, see: {@link MCRSession#getMapEntries()}</li>
         *     <li>Default locale, see {@link MCRSession#getLocale()}</li>
         *     <li>Remote IP address: 0.0.0.0</li>
         * </ul>
         *
         * @param userInformation the user representation that should be used
         */
        public ScopedValues(MCRUserInformation userInformation) {
            this(new HashMap<>(), userInformation, DEFAULT_LOCALE, "0.0.0.0");
        }
    }

}
