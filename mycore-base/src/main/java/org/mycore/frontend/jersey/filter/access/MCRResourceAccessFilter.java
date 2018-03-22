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

package org.mycore.frontend.jersey.filter.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@Priority(Priorities.AUTHORIZATION)
public class MCRResourceAccessFilter implements ContainerRequestFilter {

    private MCRResourceAccessChecker accessChecker;

    public MCRResourceAccessFilter(MCRResourceAccessChecker accessChecker) {
        this.accessChecker = accessChecker;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // TODO due to ContainerRequest.getEntity() consumes InputStream, we need to keep a copy of it in memory
        try (InputStream in = requestContext.getEntityStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
            IOUtils.copy(in, out);
            byte[] entity = out.toByteArray();
            //restore input
            requestContext.setEntityStream(new ByteArrayInputStream(entity));
            boolean hasPermission = accessChecker.isPermitted(requestContext);
            if (!hasPermission) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
            //restore input
            requestContext.setEntityStream(new ByteArrayInputStream(entity));
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

}
