package org.mycore.frontend.acl2.resources;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("ACLE/gui")
public class MCRAclEditorGuiResource {
    @GET
    @Path("{filename:.*}")
    public InputStream getResources(@PathParam("filename") String filename){
        return getClass().getResourceAsStream("/META-INF/resources/modules/acl-editor2/gui/" + filename);
    }
}