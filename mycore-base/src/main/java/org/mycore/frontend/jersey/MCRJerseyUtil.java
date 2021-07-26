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
import java.util.Optional;
import java.util.stream.Collectors;

import org.glassfish.jersey.server.ResourceConfig;
import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.jersey.resources.MCRJerseyExceptionMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Response.Status;

/**
 * Contains some jersey utility methods.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRJerseyUtil {

    public static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=utf-8";

    public static final String APPLICATION_XML_UTF8 = MediaType.APPLICATION_XML + ";charset=utf-8";

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
     * Checks if the current user has the read permission on the given derivate content.
     * Throws an unauthorized exception otherwise.
     * 
     * @param id
     *            mycore object id
     * @see MCRAccessManager#checkPermissionForReadingDerivate(String)
     */
    public static void checkDerivateReadPermission(MCRObjectID id) {
        if (!MCRAccessManager.checkDerivateContentPermission(id, MCRAccessManager.PERMISSION_READ)) {
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
    public static String getBaseURL(UriInfo info, Application app) {
        String baseURL = info.getBaseUri().toString();
        List<String> applicationPaths = MCRConfiguration2
            .getOrThrow("MCR.Jersey.Resource.ApplicationPaths", MCRConfiguration2::splitValue)
            .collect(Collectors.toList());
        for (String path : applicationPaths) {
            baseURL = removeAppPath(baseURL, path);
        }
        Optional<String> appPath = Optional.ofNullable(app)
            .map(a -> a instanceof ResourceConfig ? ((ResourceConfig) a).getApplication() : a)
            .map(Application::getClass)
            .map(c -> c.getAnnotation(ApplicationPath.class))
            .map(ApplicationPath::value)
            .map(s -> s.startsWith("/") ? s.substring(1) : s);
        if (appPath.isPresent()) {
            baseURL = removeAppPath(baseURL, appPath.get());
        }
        return baseURL;
    }

    private static String removeAppPath(String baseURL, String path) {
        if (baseURL.endsWith("/" + path + "/")) {
            return baseURL.substring(0, baseURL.indexOf(path));
        }
        return baseURL;
    }

}
