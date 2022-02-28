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

package org.mycore.iiif.presentation.resources;

import static org.mycore.iiif.presentation.MCRIIIFPresentationUtil.correctIDs;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.frontend.jersey.MCRCacheControl;
import org.mycore.iiif.common.MCRIIIFMediaTypeHelper;
import org.mycore.iiif.presentation.MCRIIIFPresentationManifestQuickAccess;
import org.mycore.iiif.presentation.impl.MCRIIIFPresentationImpl;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("/presentation/v2{noop: /?}{impl: ([a-zA-Z0-9]+)?}")
public class MCRIIIFPresentationResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRCache<String, MCRIIIFPresentationManifestQuickAccess> CACHE = new MCRCache<>(1000,
        MCRIIIFPresentationResource.class.getName());

    private static final String IMPL_PARAM = "impl";

    private static final String NAME_PARAM = "name";

    private static final String IDENTIFIER_PARAM = "identifier";

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("collection/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getCollection(@PathParam(IMPL_PARAM) String impl, @PathParam(NAME_PARAM) String name) {
        return null;
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/manifest")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getManifest(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier)
        throws CloneNotSupportedException {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String manifestAsJSON = getGson().toJson(quickAccess.getManifest());
        return addHeaders(Response.ok()).entity(manifestAsJSON).build();
    }

    protected MCRIIIFPresentationManifestQuickAccess getManifestQuickAccess(String impl, String identifier) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = CACHE
            .getIfUpToDate(impl + identifier, TimeUnit.HOURS.toMillis(1));
        if (quickAccess == null) {
            long startTime = new Date().getTime();
            MCRIIIFManifest manifest = MCRIIIFPresentationImpl.getInstance(impl).getManifest(identifier);
            long endTime = new Date().getTime();
            long timeNeeded = endTime - startTime;
            LOGGER.info("Manifest {}:{} generation needed: {}ms", impl, identifier, timeNeeded);

            quickAccess = new MCRIIIFPresentationManifestQuickAccess(manifest);
            CACHE.put(impl + identifier, quickAccess);
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
                    + MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON + "\"")
            .header("Access-Control-Allow-Origin", "*");
    }

    protected Gson getGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/sequence/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getSequence(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String sequenceAsJSON = getGson().toJson(quickAccess.getSequence(name));
        return addHeaders(Response.ok()).entity(sequenceAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/canvas/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response getCanvas(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String canvasAsJSON = getGson().toJson(quickAccess.getCanvas(name));
        return addHeaders(Response.ok()).entity(canvasAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/annotation/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getAnnotation(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String annotationAsJSON = getGson().toJson(quickAccess.getAnnotationBase(name));
        return addHeaders(Response.ok()).entity(annotationAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/list/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getAnnotationList(@PathParam(IMPL_PARAM) String impl,
        @PathParam(IDENTIFIER_PARAM) String identifier, @PathParam(NAME_PARAM) String name) {
        return getAnnotation(impl, identifier, name);
    }

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/range/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS))
    public Response getRange(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String rangeAsJSON = getGson().toJson(quickAccess.getRange(name));
        return addHeaders(Response.ok()).entity(rangeAsJSON).build();
    }

    // layers and resources are not supported currently

    @GET
    @Produces(MCRIIIFMediaTypeHelper.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/layer/{" + NAME_PARAM + "}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response getLayer(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        return null;
    }

    @GET
    @Path("{" + IDENTIFIER_PARAM + "}/res/{" + NAME_PARAM + "}[.]{format}")
    @MCRCacheControl(maxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.DAYS),
        sMaxAge = @MCRCacheControl.Age(time = 1, unit = TimeUnit.HOURS))
    public Response getContent(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name, @PathParam("format") String format) {
        return null;
    }

}
