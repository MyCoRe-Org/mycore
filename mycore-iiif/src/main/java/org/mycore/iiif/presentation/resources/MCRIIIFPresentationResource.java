/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.iiif.presentation.resources;

import static org.mycore.iiif.presentation.MCRIIIFPresentationUtil.correctIDs;

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
import org.mycore.iiif.common.MCRIIIFMediaType;
import org.mycore.iiif.presentation.MCRIIIFPresentationManifestQuickAccess;
import org.mycore.iiif.presentation.impl.MCRIIIFPresentationImpl;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/iiif/presentation/{impl}")
public class MCRIIIFPresentationResource {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRCache<String, MCRIIIFPresentationManifestQuickAccess> cache = new MCRCache<>(1000,
        MCRIIIFPresentationResource.class.getName().toString());

    private static final String IMPL_PARAM = "impl";

    private static final String NAME_PARAM = "name";

    private static final String IDENTIFIER_PARAM = "identifier";

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("collection/{" + NAME_PARAM + "}")
    public Response getCollection(@PathParam(IMPL_PARAM) String impl, @PathParam(NAME_PARAM) String name) {
        return null;
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/manifest")
    public Response getManifest(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier)
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
                    + MCRIIIFMediaType.APPLICATION_LD_JSON + "\"")
            .header("Access-Control-Allow-Origin", "*");
    }

    protected Gson getGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/sequence/{" + NAME_PARAM + "}")
    public Response getSequence(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String sequenceAsJSON = getGson().toJson(quickAccess.getSequence(name));
        return addHeaders(Response.ok()).entity(sequenceAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/canvas/{" + NAME_PARAM + "}")
    public Response getCanvas(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String canvasAsJSON = getGson().toJson(quickAccess.getCanvas(name));
        return addHeaders(Response.ok()).entity(canvasAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/annotation/{" + NAME_PARAM + "}")
    public Response getAnnotation(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String annotationAsJSON = getGson().toJson(quickAccess.getAnnotationBase(name));
        return addHeaders(Response.ok()).entity(annotationAsJSON).build();
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/list/{" + NAME_PARAM + "}")
    public Response getAnnotationList(@PathParam(IMPL_PARAM) String impl,
        @PathParam(IDENTIFIER_PARAM) String identifier, @PathParam(NAME_PARAM) String name) {
        return getAnnotation(impl, identifier, name);
    }

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/range/{" + NAME_PARAM + "}")
    public Response getRange(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        MCRIIIFPresentationManifestQuickAccess quickAccess = getManifestQuickAccess(impl, identifier);
        String rangeAsJSON = getGson().toJson(quickAccess.getRange(name));
        return addHeaders(Response.ok()).entity(rangeAsJSON).build();
    }

    // layers and resources are not supported currently

    @GET
    @Produces(MCRIIIFMediaType.APPLICATION_LD_JSON)
    @Path("{" + IDENTIFIER_PARAM + "}/layer/{" + NAME_PARAM + "}")
    public Response getLayer(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name) {
        return null;
    }

    @GET
    @Path("{" + IDENTIFIER_PARAM + "}/res/{" + NAME_PARAM + "}[.]{format}")
    public Response getContent(@PathParam(IMPL_PARAM) String impl, @PathParam(IDENTIFIER_PARAM) String identifier,
        @PathParam(NAME_PARAM) String name, @PathParam("format") String format) {
        return null;
    }

}
