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

package org.mycore.frontend.jersey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.CacheControl;

/**
 * Used to define the {@link javax.ws.rs.core.HttpHeaders#CACHE_CONTROL} header via annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MCRCacheControl {
    /**
     * sets {@code private} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">private definition</a>
     */
    FieldArgument private_() default @FieldArgument();

    /**
     * sets {@code no-cache} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">no-cache definition</a>
     */
    FieldArgument noCache() default @FieldArgument;

    /**
     * if {@link #noCache()}, sets {@code noCache} directive argument to these values
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">no-cache definition</a>
     */
    String[] noCacheFields() default {};

    /**
     * if true, sets {@code no-store} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">no-store definition</a>
     */
    boolean noStore() default false;

    /**
     * if true, sets {@code no-transform} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">no-transform definition</a>
     */
    boolean noTransform() default false;

    /**
     * if true, sets {@code must-revalidate} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">must-revalidate definition</a>
     */
    boolean mustRevalidate() default false;

    /**
     * if true, sets {@code proxy-revalidate} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">proxy-revalidate definition</a>
     */
    boolean proxyRevalidate() default false;

    /**
     * if true, sets {@code public} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">public definition</a>
     */
    boolean public_() default false;

    /**
     * Sets {@code max-age} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">max-age definition</a>
     */
    Age maxAge() default @Age(time = -1, unit = TimeUnit.SECONDS);

    /**
     * Sets {@code s-maxage} directive
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">s-maxage definition</a>
     */
    Age sMaxAge() default @Age(time = -1, unit = TimeUnit.SECONDS);

    /**
     * Sets further Cache-Control Extensions
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.3">Cache Control Extensions</a>
     */
    Extension[] extensions() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    public static @interface Age {
        long time();

        TimeUnit unit() default TimeUnit.MINUTES;

    }

    public static @interface FieldArgument {
        /**
         * if true, this directive is present in header value
         */
        boolean active() default false;

        /**
         * if {@link #active()}, sets directive argument to these values
         */
        String[] fields() default {};
    }

    public static @interface Extension {
        String directive();

        String argument() default "";
    }

}
