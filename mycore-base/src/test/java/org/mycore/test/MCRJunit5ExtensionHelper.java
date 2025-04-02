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

package org.mycore.test;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Helper class for JUnit 5 extensions.
 */
public class MCRJunit5ExtensionHelper {

    /**
     * Check if the current test class is a nested test class.
     *
     * @param context the current extension context
     * @return true if the current test class is a nested test class
     */
    public static boolean isNestedTestClass(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();
        return testClass.map(clazz -> {
            return clazz.isAnnotationPresent(Nested.class) ||
                clazz.isMemberClass();
        }).orElse(false);
    }
}
