/*
 * $Id$
 * $Revision: 5697 $ $Date: Feb 19, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.jersey.filter.access;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
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
