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

package org.mycore.common.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods that should be called after the creation of the object.
 * <p>
 * The method may have a single parameter of type {@link String} for which, if present, the name of the
 * configuration property (which contains the class name of the configured instance) will be passed; either
 * <ul>
 *   <li>
 *     the entire name, including the trailing <code>.Class</code> suffix (ACTUAL)
 *   </li>
 *   <li>
 *     the entire name, except the trailing <code>.Class</code> suffix (CANONICAL, default)
 *   </li>
 *   <li>
 *     just the last actual part of the name (the part before the trailing <code>.Class</code> suffix) (TRAILING_NAME).
 *   </li>
 * </ul>
 * The last option is useful for instances that are listed in a map (for example via {@link MCRInstanceMap}),
 * where the instance is interested in its map key.
 * <p>
 * Example:
 * <pre><code>
 * MCR.Foo.Bar.Widgets.fancy_widget.Class=foo.bar.widgets.FancyWidget
 * </code></pre>
 * <ul>
 *   <li>ACTUAL: <code>MCR.Foo.Bar.Widgets.fancy_widget.Class</code></li>
 *   <li>CANONICAL: <code>MCR.Foo.Bar.Widgets.fancy_widget</code></li>
 *   <li>TRAILING_NAME: <code>fancy_widget</code></li>
 * </ul>
 * <p>
 * The method needs to be public.
 *
 * @author Sebastian Hofmann
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
public @interface MCRPostConstruction {

    /**
     * @return Weather to inject the actual or the canonical property.
     */
    Value value() default Value.CANONICAL;

    /**
     * @return The order in which the annotated methods are processed. The higher the value, the later the
     * method is processed.
     */
    int order() default 0;

    enum Value {

        /**
         * Provides the entire name, including the trailing <code>.Class</code> suffix.
         * <p>
         * Example: <code>MCR.Foo.Bar.Widgets.fancy_widget.Class</code>
         */
        ACTUAL,

        /**
         * Provides the entire name, except the trailing <code>.Class</code> suffix.
         * <p>
         * Example: <code>MCR.Foo.Bar.Widgets.fancy_widget</code>
         */
        CANONICAL,

        /**
         * Provides just the last actual part of the name (the part before the trailing <code>.Class</code> suffix).
         * <p>
         * Example: <code>fancy_widget</code>
         */
        TRAILING_NAME

    }

}
