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

package org.mycore.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorRawPropertiesBasicTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.1", string = "1"),
            @MCRTestProperty(key = "Foo.2", string = "2"),
            @MCRTestProperty(key = "Foo.Values.1", string = "Value-1"),
            @MCRTestProperty(key = "Foo.Values.2", string = "Value-2"),
            @MCRTestProperty(key = "MCR.Values.1", string = "MCR-Value-1"),
            @MCRTestProperty(key = "MCR.Values.2", string = "MCR-Value-2")
        })
    public void annotated() {

        TestClass instance = ofName(TestClass.class);

        assertEquals(Map.of("1", "1", "2", "2", "Values.1", "Value-1", "Values.2", "Value-2"), instance.values);
        assertEquals(Map.of("1", "Value-1", "2", "Value-2"), instance.nestedValues);
        assertEquals(Map.of("1", "MCR-Value-1", "2", "MCR-Value-2"), instance.absoluteValues);

    }

    private <S> S ofName(Class<S> superClass) {
        return MCRInstanceConfiguration.ofName(superClass, "Foo", MCRConfiguration2
            .getAllPropertiesTree()).instantiate();
    }

    public static class TestClass {

        @MCRRawProperties(namePattern = "*")
        public Map<String, String> values;

        @MCRRawProperties(namePattern = "Values.*")
        public Map<String, String> nestedValues;

        @MCRRawProperties(namePattern = "MCR.Values.*", absolute = true)
        public Map<String, String> absoluteValues;

    }

}
