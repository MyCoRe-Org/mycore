package org.mycore.restapi;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.restapi.annotations.MCRApiDraft;

@Priority(Priorities.AUTHORIZATION)
public class MCRApiDraftFilter implements ContainerRequestFilter {
    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final Optional<MCRApiDraft> apiDraft = Stream
            .of(resourceInfo.getResourceMethod(), resourceInfo.getResourceClass())
            .map(r -> r.getAnnotation(MCRApiDraft.class))
            .filter(Objects::nonNull)
            .findFirst();
        if (apiDraft.isPresent()) {
            final String apiDraftName = apiDraft.get().value();
            final String propertyName = "MCR.RestApi.Draft." + apiDraftName;
            if (!MCRConfiguration2.getBoolean(propertyName).orElse(false)) {
                LogManager.getLogger().warn("Draft API not enabled. Set '" + propertyName + "=true' to enable access.");
                requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build());
            }
        }
    }
}
