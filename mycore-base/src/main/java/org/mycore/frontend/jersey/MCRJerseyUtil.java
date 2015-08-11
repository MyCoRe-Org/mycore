package org.mycore.frontend.jersey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jdom2.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Contains some jersey utility methods.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRJerseyUtil {

    /**
     * Transforms a jdom document to a <code>MCRContent</code> via the <code>MCRLayoutService</code>.
     * 
     * @param document the document to transform
     * @param request the http request
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
     * @param id id as string
     * @return mycore object id
     */
    public static MCRObjectID getID(String id) {
        MCRObjectID mcrId;
        try {
            mcrId = MCRObjectID.getInstance(id);
        } catch (MCRException mcrExc) {
            throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("invalid mycore id").build());
        }
        if (!MCRMetadataManager.exists(mcrId)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return mcrId;
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param id mycore object id
     * @param permission permission to check
     */
    public static void checkPermission(MCRObjectID id, String permission) {
        if (!MCRAccessManager.checkPermission(id, permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param id mycore object id
     * @param permission permission to check
     */
    public static void checkPermission(String id, String permission) {
        if (!MCRAccessManager.checkPermission(id, permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Checks if the current user has the given permission. Throws an unauthorized exception otherwise.
     * 
     * @param permission permission to check
     */
    public static void checkPermission(String permission) {
        if (!MCRAccessManager.checkPermission(permission)) {
            throw new WebApplicationException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * Returns a human readable message of a http status code. 
     * 
     * @param statusCode http status code
     * @return human readable string
     */
    public static String fromStatusCode(int statusCode) {
        Status status = Response.Status.fromStatusCode(statusCode);
        return status != null ? status.getReasonPhrase() : "Unknown Error";
    }

}
