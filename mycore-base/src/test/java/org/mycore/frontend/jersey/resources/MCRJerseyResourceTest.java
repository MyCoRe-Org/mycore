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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.junit.BeforeClass;
import org.junit.Test;

public class MCRJerseyResourceTest extends MCRJerseyTest {

    @Path("hello")
    public static class TestCase {

        @GET
        public String get() {
            return "Hello World!";
        }

        @GET
        @Path("logout/{id}")
        public String logout(@PathParam("id") String id) {
            return "GoodBye " + id + "!";
        }
    }

    @BeforeClass
    public static void register() {
        JERSEY_CLASSES.add(TestCase.class);
    }

    @Test
    public void test() {
        final String hello = target("hello").request().get(String.class);
        assertEquals("Hello World!", hello);

        final String logout = target("hello/logout/Peter").request().get(String.class);
        assertEquals("GoodBye Peter!", logout);
    }

}
