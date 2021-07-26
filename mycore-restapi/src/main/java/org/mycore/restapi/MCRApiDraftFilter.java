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

package org.mycore.restapi;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.restapi.annotations.MCRApiDraft;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Filter all methods and classes marked by {@link MCRApiDraft} and return {@link Response.Status#NOT_FOUND}.
 *
 * Use Property <code>MCR.RestApi.Draft.{{@link MCRApiDraft#value()}}=true</code> to enable a group of endpoints
 * marked with the same <code>value</code>, e.g.<br>
 * "<code>MCR.RestApi.Draft.Proposed=true</code>" to include all endpoints marked with
 * <code>@MCRApiDraft("Proposed")</code>.
 * @author Thomas Scheffler (yagee)
 * @see MCRApiDraft
 */
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
