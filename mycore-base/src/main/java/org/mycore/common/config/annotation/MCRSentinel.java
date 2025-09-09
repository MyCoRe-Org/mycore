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
 * This annotation is used to configure which sub-property acts as a sentinel, i.e. which property should be used to
 * decide if a component, otherwise included in a field annotated with {@link MCRInstance}, {@link MCRInstanceMap}
 * or {@link MCRInstanceList}, should actually be instantiated and configured, or if it should be rejected and treated
 * as if it is not part of the configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRSentinel {

    String DEFAULT_KEY = "Default";

    String ENABLED_KEY = "Enabled";

    /**
     * @return The name of sub-property to act as a sentinel.
     */
    String name() default ENABLED_KEY;

    /**
     * @return The default value to be used if the configured sub-property is not present.
     */
    boolean defaultValue() default true;

    /**
     * @return The value of the sub-property that causes the component to be rejected.
     */
    boolean rejectionValue() default false;

    /**
     * @return Weather or not the sentinel is enabled.
     */
    boolean enabled() default true;

}
