package org.mycore.frontend.classeditor.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("xxx")
public class TestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("test")
    public String test() {
        return "Hallo";
    }

}
