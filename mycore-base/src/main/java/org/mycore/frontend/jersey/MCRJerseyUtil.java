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

package org.mycore.frontend.jersey;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.resources.MCRJerseyExceptionMapper;

/**
 * Contains some jersey utility methods.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRJerseyUtil {

    public static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=utf-8";

    /**
     * Transforms a jdom document to a <code>MCRContent</code> via the <code>MCRLayoutService</code>.
     * 
     * @param document
     *            the document to transform
     * @param request
     *            the http request
     */
    public static MCRContent transform(Document document, HttpServletRequest request) throws Exception {
        MCRParameterCollector parameter = new MCRParameterCollector(request);
        MCRContent result;
        MCRJDOMContent source = new MCRJDOMContent(document);
        MCRContentTransformer transformer = MCRLayoutService.getContentTransformer(source.getDocType(), parameter);
        if (transformer instanceof MCRParameterizedTransformer) {
            result = ((MCRParameterizedTransformer) transformer).transform(source, parameter);
        } else {
            result = transformer.transform(source);
        }
        return result;
    }

    /**
     * Returns the mycore id. Throws a web application exception if the id is invalid or not found.
     * 
     * @param id
     *            id as string
     * @return mycore object id
     */
    public static MCRObjectID getID(String id) {
        MCRObjectID mcrId = null;
        try {
            mcrId = MCRObjectID.getInstance(id);
            if (!MCRMetadataManager.exists(mcrId)) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
        } catch (MCRException mcrExc) {
            throwException(Status.BAD_REQUEST, "invalid mycore id '" + id + "'");
        }
        return mcrId;
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param id
     *            mycore object id
     * @param permission
     *            permission to check
     */
    public static void checkPermission(MCRObjectID id, String permission) {
        if (!MCRAccessManager.checkPermission(id, permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Checks if the current user has the read permission on the given derivate. Throws an unauthorized exception
     * otherwise.
     * 
     * @param id
     *            mycore object id
     * @see MCRAccessManager#checkPermissionForReadingDerivate(String)
     */
    public static void checkDerivateReadPermission(MCRObjectID id) {
        if (!MCRAccessManager.checkPermissionForReadingDerivate(id.toString())) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param id
     *            mycore object id
     * @param permission
     *            permission to check
     */
    public static void checkPermission(String id, String permission) {
        if (!MCRAccessManager.checkPermission(id, permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param permission
     *            permission to check
     */
    public static void checkPermission(String permission) {
        if (!MCRAccessManager.checkPermission(permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Throws a {@link WebApplicationException} with status and message.
     * This exception will be handled by the {@link MCRJerseyExceptionMapper}.
     * See <a href="http://stackoverflow.com/questions/29414041/exceptionmapper-for-webapplicationexceptions-thrown-with-entity">stackoverflow</a>.
     * 
     * @param status the http return status
     * @param message the message to print
     */
    public static void throwException(Status status, String message) {
        throw new WebApplicationException(new MCRException(message), status);
    }

    /**
     * Returns a human readable message of a http status code.
     * 
     * @param statusCode
     *            http status code
     * @return human readable string
     */
    public static String fromStatusCode(int statusCode) {
        Status status = Response.Status.fromStatusCode(statusCode);
        return status != null ? status.getReasonPhrase() : "Unknown Error";
    }

    /**
     * Returns the base URL of the mycore system.
     *
     * @param info the UriInfo
     *
     * @return base URL of the mycore system as string
     */
    public static String getBaseURL(UriInfo info) {
        String baseURL = info.getBaseUri().toString();
        List<String> applicationPaths = MCRConfiguration.instance()
            .getStrings("MCR.Jersey.Resource.ApplicationPaths");
        for (String path : applicationPaths) {
            if (baseURL.endsWith("/" + path + "/")) {
                return baseURL.substring(0, baseURL.indexOf(path));
            }
        }
        return baseURL;
    }

}
