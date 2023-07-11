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

package org.mycore.common.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a property name that can be used to convey the class name of a class that should be instantiated.
 */
public class MCRInstanceName {

    private final String actual;

    private final String canonical;

    private final Suffix suffix;

    private MCRInstanceName(String canonical, Suffix suffix) {
        this.actual = suffix.appendTo(canonical);
        this.canonical = canonical;
        this.suffix = suffix;
    }

    /**
     * Creates a {@link MCRInstanceName} given a <em>name</em>.
     * <p>
     * If the given <em>name</em> ends with <code>.Class</code> or <code>.class</code>, that suffix is removed from
     * the {@link MCRInstanceName#canonical()} form and made available as the {@link MCRInstanceName#suffix()}.
     * <p>
     * In such a case, both <code>Class</code> and <code>class</code> are reported as
     * {@link MCRInstanceName#ignoredKeys()} that should not be included in the
     * {@link MCRInstanceConfiguration#properties()} of an {@link MCRInstanceConfiguration} using the created
     * {@link MCRInstanceName}.
     *
     * @param name the name
     * @return the name
     */
    public static MCRInstanceName of(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            return of(name, "", name);
        } else {
            return of(name, name.substring(0, index), name.substring(index + 1));
        }
    }

    private static MCRInstanceName of(String fullName, String leadingSegments, String lastSegment) {
        for (Suffix suffix : Suffix.representedValues()) {
            if (Objects.equals(lastSegment, suffix.representation)) {
                return new MCRInstanceName(leadingSegments, suffix);
            }
        }
        return new MCRInstanceName(fullName, Suffix.NONE);
    }

    public String actual() {
        return actual;
    }

    public String canonical() {
        return canonical;
    }

    public Suffix suffix() {
        return suffix;
    }

    public List<String> ignoredKeys() {
        return suffix.ignoredKeys;
    }

    public MCRInstanceName subName(String segment) {
        if (canonical.isEmpty()) {
            return new MCRInstanceName(segment, suffix);
        } else {
            return new MCRInstanceName(canonical + "." + segment, suffix);
        }
    }

    @Override
    public String toString() {
        return "MCRInstanceName {" +
            "actual=" + actual + ", " +
            "canonical=" + canonical + ", " +
            "suffix=" + suffix + "}";
    }

    public enum Suffix {

        NONE(null, Collections.emptyList()),

        UPPER_CASE("Class", Arrays.asList("Class", "class")),

        LOWER_CASE("class", Arrays.asList("Class", "class"));

        private static final Suffix[] REPRESENTED_VALUES = Arrays.stream(values())
            .filter(suffix -> suffix.representation != null)
            .collect(Collectors.toList())
            .toArray(new Suffix[0]);

        private final String representation;

        private final List<String> ignoredKeys;

        Suffix(String representation, List<String> ignoredKeys) {
            this.representation = representation;
            this.ignoredKeys = ignoredKeys;
        }

        public Optional<String> representation() {
            return Optional.ofNullable(representation);
        }

        public static Suffix[] representedValues() {
            return REPRESENTED_VALUES;
        }

        public String appendTo(String string) {
            if (representation == null) {
                return string;
            } else {
                if (string.isEmpty()) {
                    return representation;
                } else {
                    return string + "." + representation;

                }
            }
        }
    }

}
