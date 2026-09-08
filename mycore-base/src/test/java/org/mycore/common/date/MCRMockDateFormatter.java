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

package org.mycore.common.date;

import java.time.Instant;
import java.util.function.Supplier;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

@MCRConfigurationProxy(proxyClass = MCRMockDateFormatter.Factory.class)
public class MCRMockDateFormatter extends MCRInstantFormatterBase {

    public static final String VALUE_KEY = "Value";

    private final String value;

    public MCRMockDateFormatter(String value) {
        this.value = value;
    }

    @Override
    public String format(Instant instant) {
        return value;
    }

    public static final class Factory implements Supplier<MCRMockDateFormatter> {

        @MCRProperty(name = VALUE_KEY)
        public String value;

        @Override
        public MCRMockDateFormatter get() {
            return new MCRMockDateFormatter(value);
        }

    }

}
