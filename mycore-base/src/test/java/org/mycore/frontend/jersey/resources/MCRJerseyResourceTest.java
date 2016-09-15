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
