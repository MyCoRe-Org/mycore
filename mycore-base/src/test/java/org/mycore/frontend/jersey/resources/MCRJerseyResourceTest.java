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

package org.mycore.frontend.jersey.resources;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

public class MCRJerseyResourceTest {

    private MCRJerseyTestFeature jersey;

    @BeforeEach
    public void setUpJersey() throws Exception {
        jersey = new MCRJerseyTestFeature();
        jersey.setUp(Set.of(TestCase.class));
    }

    @AfterEach
    public void tearDownJersey() throws Exception {
        jersey.tearDown();
    }

    @Test
    public void test() {
        System.out.println("Running test");
        final String hello = jersey.target("hello").request().get(String.class);
        assertEquals("Hello World!", hello);

        final String logout = jersey.target("hello/logout/Peter").request().get(String.class);
        assertEquals("GoodBye Peter!", logout);
    }

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

}
