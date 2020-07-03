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

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import com.google.inject.AbstractModule;

public class MCRConfiguration2Test extends MCRTestCase {

    @Test
    public void instantiateClass() {
        TestClass testClass = MCRConfiguration2.instantiateClass(TestClass.class.getName());
        assertEquals("welcome to my service", testClass.useService());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCRConfiguration2.set("MCR.Inject.Module.GuiceTest", TestModule.class.getName());
    }

    public static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestService.class).to(TestServiceImpl.class);
        }

    }

    private static class TestClass {

        @Inject
        private TestService service;

        public String useService() {
            return service.get();
        }

    }

    private interface TestService {

        String get();

    }

    private static class TestServiceImpl implements MCRConfiguration2Test.TestService {

        @Override
        public String get() {
            return "welcome to my service";
        }

    }

}
