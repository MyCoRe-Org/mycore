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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Function;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MCRTestUrl {

    String url();

    String content() default "";

    ContentEncoding contentEncoding() default ContentEncoding.UTF8_STRING;

    Header[] headers() default {};

    enum ContentEncoding {

        UTF8_STRING(content -> content.getBytes(StandardCharsets.UTF_8)),

        BASE64_BYTES(content -> Base64.getDecoder().decode(content));

        private final Function<String, byte[]> contentDecoder;

        ContentEncoding(Function<String, byte[]> contentDecoder) {
            this.contentDecoder = contentDecoder;
        }

        public byte[] decode(MCRTestUrl url) {
            return contentDecoder.apply(url.content());
        }

    }

    @interface Header {

        String name();

        String value();

    }

}
