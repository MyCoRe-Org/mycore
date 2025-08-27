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

package org.mycore.ocfl.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test field as a parameter that should be included in test permutations.
 * <p>
 * Fields annotated with {@code @PermutedParam} will be assigned values
 * from a predefined set depending on their type:
 * <ul>
 *   <li>{@code boolean}: true and false</li>
 *   <li>{@code enum}: all enum constants</li>
 *   <li>{@code String}: values provided via {@link PermutedValue}</li>
 * </ul>
 * <p>
 * This annotation is used in conjunction with {@code MCRPermutationExtension}
 * to generate and inject all combinations of parameter values into a test class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PermutedParam {
}
