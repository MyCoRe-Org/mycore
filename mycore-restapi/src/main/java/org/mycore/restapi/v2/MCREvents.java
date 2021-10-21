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

package org.mycore.restapi.v2;

import java.net.URI;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.restapi.v2.annotation.MCRRequireAuthorization;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/events")
@OpenAPIDefinition(
    tags = @Tag(name = MCRRestUtils.TAG_MYCORE_ABOUT, description = "repository events"))
@Singleton
@MCRRequireAuthorization
public class MCREvents {

    @Context
    Sse sse;

    @Context
    ServletContext context;

    @Context
    Application application;

    @Context
    UriInfo uriInfo;

    private volatile SseBroadcaster objectBroadcaster,
        derivateBroadcaster, pathBroadcaster;

    @PostConstruct
    public void init() {
        LogManager.getLogger().error("Base URI: {}", uriInfo::getBaseUri);
        URI baseUri = uriInfo.getBaseUri(); //accquired from first request
        URI webAppBase = URI.create(MCRFrontendUtil.getBaseURL()); //use official URL
        Function<URI, URI> uriResolver = webAppBase.resolve(baseUri.getPath())::resolve;
        objectBroadcaster = sse.newBroadcaster();
        MCREventManager.instance().addEventHandler(MCREvent.OBJECT_TYPE,
            new MCREventHandler.MCRObjectHandler(objectBroadcaster, sse, uriResolver));
        derivateBroadcaster = sse.newBroadcaster();
        MCREventManager.instance().addEventHandler(MCREvent.DERIVATE_TYPE,
            new MCREventHandler.MCRDerivateHandler(derivateBroadcaster, sse, uriResolver));
        pathBroadcaster = sse.newBroadcaster();
        MCREventManager.instance().addEventHandler(MCREvent.PATH_TYPE,
            new MCREventHandler.MCRPathHandler(pathBroadcaster, sse, uriResolver, context));
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void registerAllEvents(@Context SseEventSink sseEventSink) {
        registerObjectEvents(sseEventSink);
        registerDerivateEvents(sseEventSink);
        registerPathEvents(sseEventSink);
    }

    @GET
    @Path("/objects")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void registerObjectEvents(@Context SseEventSink sseEventSink) {
        objectBroadcaster.register(sseEventSink);
    }

    @GET
    @Path("/derivates")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void registerDerivateEvents(@Context SseEventSink sseEventSink) {
        derivateBroadcaster.register(sseEventSink);
    }

    @GET
    @Path("/files")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void registerPathEvents(@Context SseEventSink sseEventSink) {
        pathBroadcaster.register(sseEventSink);
    }
}
