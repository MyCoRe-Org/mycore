/**
 * 
 */
package org.mycore.media.frontend.jersey;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.JSONP;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.media.video.MCRMediaSource;
import org.mycore.media.video.MCRMediaSourceProvider;

import com.google.gson.Gson;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("jwplayer")
public class MCRJWPlayerResource {
    @Context
    private HttpServletRequest request;

    @GET
    @Path("{derivateId}/{path: .+}/sources.js")
    @Produces({ "application/javascript" })
    @JSONP(callback = "callback", queryParam = "callback")
    public String getSourcesAsJSONP(@PathParam("derivateId") String derivateId, @PathParam("path") String path)
        throws URISyntaxException, IOException {
        // TODO: FIX THIS: https://jersey.java.net/documentation/latest/user-guide.html#d0e8837
        return getSources(derivateId, path);
    }

    @GET
    @Path("{derivateId}/{path: .+}/sources.json")
    @Produces({ "application/javascript" })
    public String getSources(@PathParam("derivateId") String derivateId, @PathParam("path") String path)
        throws URISyntaxException, IOException {
        MCRObjectID derivate = MCRJerseyUtil.getID(derivateId);
        MCRJerseyUtil.checkDerivateReadPermission(derivate);
        try {
            MCRMediaSourceProvider formatter = new MCRMediaSourceProvider(derivateId, path,
                Optional.ofNullable(request.getHeader("User-Agent")),
                () -> Arrays.stream(Optional.ofNullable(request.getQueryString()).orElse("").split("&"))
                    .filter(p -> !p.startsWith("callback="))
                    .toArray(size -> new String[size]));
            return toJson(formatter.getSources());
        } catch (NoSuchFileException e) {
            LogManager.getLogger().warn("Could not find video file.", e);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    private String toJson(List<MCRMediaSource> sources) {
        return new Gson().toJson(sources.stream().map(s -> new Source(s.getUri(), s.getType().getSimpleType())).toArray(
            size -> new Source[size]));
    }

    /* simple pojo for json output */
    private static class Source {
        @SuppressWarnings("unused")
        private String file;

        @SuppressWarnings("unused")
        private String type;

        public Source(String file, String type) {
            LogManager.getLogger().info("file : " + file);
            this.file = file;
            this.type = type;
        }
    }

}
