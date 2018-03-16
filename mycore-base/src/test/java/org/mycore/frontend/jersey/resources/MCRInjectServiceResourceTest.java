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

package org.mycore.frontend.jersey.resources;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;

import com.google.inject.AbstractModule;
import org.mycore.common.config.MCRConfiguration2;

public class MCRInjectServiceResourceTest extends MCRJerseyTest {

    @Path("service")
    public static class ServiceTestCase {

        @Inject
        private TestService service;

        @GET
        public String get() {
            return service.get();
        }

    }

    @BeforeClass
    public static void register() {
        JERSEY_CLASSES.add(ServiceTestCase.class);
    }

    @Override
    public Application configure() {
        MCRConfiguration2.set("MCR.Inject.Module.GuiceTest", TestModule.class.getName());
        return super.configure();
    }

    @Test
    public void test() {
        final String hello = target("service").request().get(String.class);
        assertEquals("welcome to my service", hello);
    }

    public static class TestModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(TestService.class).to(TestServiceImpl.class);
        }

    }

    private interface TestService {

        String get();

    }

    private static class TestServiceImpl implements TestService {

        @Override
        public String get() {
            return "welcome to my service";
        }

    }

}
