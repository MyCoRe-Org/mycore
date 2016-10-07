package org.mycore.iiif.presentation.resources;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.iiif.presentation.MCRIIIFPresentationManifestQuickAccess;
import org.mycore.iiif.presentation.impl.MCRIIIFPresentationImpl;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.mycore.iiif.presentation.MCRIIIFPresentationUtil.correctIDs;

@Path("/iiif/presentation")
public class MCRIIIFPresentationResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRCache<String, MCRIIIFPresentationManifestQuickAccess> cache = new MCRCache<>(1000,
        MCRIIIFPresentationResource.class.getName().toString());

    private static final String APPLICATION_LD_JSON = "application/ld+json";

    private static final String IMPL = "impl";

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/collection/{name}")
    public Response getCollection(@PathParam(IMPL) String impl, @PathParam("name") String name) {
        return null;
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/manifest")
    public Response getManifest(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier)
        throws CloneNotSupportedException {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String manifestAsJSON = getGson().toJson(quickAccess.getManifest());
        return addHeaders(Response.ok()).entity(manifestAsJSON).build();
    }

    protected MCRIIIFPresentationManifestQuickAccess getManifestQuickAccess(String impl, String identifier) {
        MCRIIIFPresentationManifestQuickAccess quickAccess;
        if ((quickAccess = cache.getIfUpToDate(impl + identifier, TimeUnit.HOURS.toMillis(1))) == null) {
            long startTime = new Date().getTime();
            MCRIIIFManifest manifest = MCRIIIFPresentationImpl.getInstance(impl).getManifest(identifier);
            long endTime = new Date().getTime();
            long timeNeeded = endTime - startTime;
            LOGGER.info("Manifest {}:{} generation needed: {}ms", impl, identifier, timeNeeded);

            quickAccess = new MCRIIIFPresentationManifestQuickAccess(manifest);
            cache.put(impl + identifier, quickAccess);
            correctIDs(manifest, impl, identifier);
        } else {
            LOGGER.info("Manifest {}:{} served from cache", impl, identifier);
        }
        return quickAccess;
    }

    protected Response.ResponseBuilder addHeaders(Response.ResponseBuilder builder) {
        return builder
            .header("Link",
                "<http://iiif.io/api/presentation/2/context.json>\n;rel=\"http://www.w3.org/ns/json-ld#context\";type=\""
                    + APPLICATION_LD_JSON + "\"")
            .header("Access-Control-Allow-Origin", "*");
    }

    protected Gson getGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/sequence/{name}")
    public Response getSequence(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String sequenceAsJSON = getGson().toJson(quickAccess.getSequence(name));
        return addHeaders(Response.ok()).entity(sequenceAsJSON).build();
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/canvas/{name}")
    public Response getCanvas(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String canvasAsJSON = getGson().toJson(quickAccess.getCanvas(name));
        return addHeaders(Response.ok()).entity(canvasAsJSON).build();
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/annotation/{name}")
    public Response getAnnotation(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String annotationAsJSON = getGson().toJson(quickAccess.getAnnotationBase(name));
        return addHeaders(Response.ok()).entity(annotationAsJSON).build();
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/list/{name}")
    public Response getAnnotationList(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        return getAnnotation(impl, identifier, name);
    }

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/range/{name}")
    public Response getRange(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String rangeAsJSON = getGson().toJson(quickAccess.getRange(name));
        return addHeaders(Response.ok()).entity(rangeAsJSON).build();
    }

    // layers and resources are not supported currently

    @GET
    @Produces(APPLICATION_LD_JSON)
    @Path("{impl}/{identifier}/layer/{name}")
    public Response getLayer(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name) {
        return null;
    }

    @GET
    @Path("{impl}/{identifier}/res/{name}[.]{format}")
    public Response getContent(@PathParam(IMPL) String impl, @PathParam("identifier") String identifier,
        @PathParam("name") String name, @PathParam("format") String format) {
        return null;
    }

}
