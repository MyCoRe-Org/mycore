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
import java.util.function.Function;
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
        return withScopedValues(ScopedValues::locale, super::getLocale);
    }

    @Override
    public void setUserInformation(MCRUserInformation userSystemAdapter) {
        withScopedValues(values -> {
            throw new IllegalArgumentException(
                "User information should not be set when running in restricted scope: " + values);
        }, () -> {
            super.setUserInformation(userSystemAdapter);
            return null;
        });
    }

    @Override
    public Object get(Object key) {
        return withScopedValues(v -> v.map.get(key), () -> super.get(key));
    }

    @Override
    public Object put(Object key, Object value) {
        return withScopedValues(v -> v.map.put(key, value), () -> super.put(key, value));
    }

    @Override
    public String getCurrentIP() {
        return withScopedValues(ScopedValues::ip, super::getCurrentIP);
    }

    @Override
    public void close() {
        clearScopedValues();
        super.close();
    }

    @Override
    public MCRUserInformation getUserInformation() {
        return withScopedValues(ScopedValues::userInformation, super::getUserInformation);
    }

    @Override
    public Iterator<Object> getObjectsKeyList() {
        return withScopedValues(v -> v.map.keySet().iterator(), super::getObjectsKeyList);
    }

    @Override
    public List<Map.Entry<Object, Object>> getMapEntries() {
        return withScopedValues(v -> v.map.entrySet().stream().toList(), super::getMapEntries);
    }

    @Override
    public void deleteObject(Object key) {
        withScopedValues(v -> {
            v.map.remove(key);
            return null;
        }, () -> {
            super.deleteObject(key);
            return null;
        });
    }

    @Override
    public Object computeIfAbsent(Object key, Function<Object, Object> mappingFunction) {
        return withScopedValues(v -> v.map.computeIfAbsent(key, mappingFunction),
            () -> super.computeIfAbsent(key, mappingFunction));
    }

    /**
     * Executes an operation either within a scoped context or with default behavior.
     * <p>
     * This method serves as a template for the commonly used logic to check scoped values.
     * If scoped values are present, the {@code scopedAction} is executed, otherwise the {@code defaultAction}.
     *
     * @param <T> The return type of the operation
     * @param scopedAction The operation to execute with scoped values
     * @param defaultAction The default operation to execute when no scoped values are present
     * @return The result of the executed operation
     */
    private <T> T withScopedValues(Function<ScopedValues, T> scopedAction, Supplier<T> defaultAction) {
        ScopedValues values = scopedValues.get();
        return values == null ? defaultAction.get() : scopedAction.apply(values);
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
