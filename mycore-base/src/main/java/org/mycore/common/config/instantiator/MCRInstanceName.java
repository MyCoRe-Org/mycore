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

package org.mycore.common.config.instantiator;

/**
 * Represents a property name that can be used to convey the class name of a class that should be instantiated.
 */
public final class MCRInstanceName {

    private static final String CLASS_SUFFIX = ".Class";

    private final String canonical;

    private final String actual;

    private MCRInstanceName(String canonical, String actual) {
        this.canonical = canonical;
        this.actual = actual;
    }

    /**
     * Creates a {@link MCRInstanceName} given a <em>name</em>.
     * <p>
     * If the given <em>name</em> ends with <code>.Class</code>, that suffix is removed
     * and the remainder used as the {@link MCRInstanceName#canonical()} name.
     *
     * @param name the name
     * @return the name
     */
    public static MCRInstanceName of(String name) {
        if (name == null || name.isEmpty() || CLASS_SUFFIX.equals(name)) {
            throw new IllegalArgumentException("Instance name must not be empty");
        }
        if (name.endsWith(CLASS_SUFFIX)) {
            return new MCRInstanceName(name.substring(0, name.length() - CLASS_SUFFIX.length()), name);
        } else {
            return new MCRInstanceName(name, name + CLASS_SUFFIX);
        }
    }

    public String actual() {
        return actual;
    }

    public String canonical() {
        return canonical;
    }

    public MCRInstanceName nested(String segment) {
        String nestedCanonical = canonical + "." + segment;
        return new MCRInstanceName(nestedCanonical, nestedCanonical + CLASS_SUFFIX);

    }

    @Override
    public String toString() {
        return "MCRInstanceName {" + "canonical=" + canonical + ", " + "actual=" + actual + "}";
    }

}
