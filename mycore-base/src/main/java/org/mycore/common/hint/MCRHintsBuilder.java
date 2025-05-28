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

package org.mycore.common.hint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A builder for {@link MCRHints}.
 */
public final class MCRHintsBuilder {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Set<Entry<?>> entries = new HashSet<>();

    public <T> MCRHintsBuilder add(MCRHintKey<T> hintKey, T hintValue) {
        Entry<T> entry = new Entry<>(hintKey, hintKey.check(hintValue));
        entries.remove(entry);
        entries.add(entry);
        return this;
    }

    public <T> MCRHintsBuilder add(MCRHintKey<T> hintKey, Optional<T> hintValue) {
        hintValue.ifPresent(value -> add(hintKey, value));
        return this;
    }

    public <T> MCRHintsBuilder add(MCRHint<T> hint) {
        add(hint.key(), hint.value());
        return this;
    }

    public MCRHints build() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Building hints …");
            entries.forEach(entry -> LOGGER.debug(" … with entry {}: {}", entry.key, entry.formattedValue()));
        }
        return new Hints(entries);
    }

    private static final class Hints implements MCRHints {

        private final Map<MCRHintKey<?>, Object> hints = new HashMap<>();

        private Hints(Set<Entry<?>> entries) {
            entries.forEach(hint -> hints.put(hint.key, hint.value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(MCRHintKey<T> hintKey) {
            return Optional.ofNullable((T) hints.get(hintKey));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getOrElse(MCRHintKey<T> hintKey, T fallback) {
            return (T) hints.getOrDefault(hintKey, fallback);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MCRHintsBuilder builder() {
            MCRHintsBuilder builder = new MCRHintsBuilder();
            hints.forEach((key, value) -> builder.add((MCRHintKey<Object>) key, value));
            return builder;
        }

    }

    private static final class Entry<T> {

        private final MCRHintKey<T> key;

        private final T value;

        private Entry(MCRHintKey<T> hintKey, T hintValue) {
            this.key = hintKey;
            this.value = hintValue;
        }

        public String formattedValue() {
            return key.format(value);
        }

        @Override
        public String toString() {
            return key.toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry && key == ((Entry<?>) other).key;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

    }

}
