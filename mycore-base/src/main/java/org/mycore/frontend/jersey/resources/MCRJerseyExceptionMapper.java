package org.mycore.frontend.jersey.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.servlets.MCRErrorServlet;

/**
 * Handles jersey web application exceptions. By default this class
 * just forwards the exception. Only if the request accepts HTML a
 * custom error page is generated.
 *
 * @author Matthias Eichner
 */
@Provider
public class MCRJerseyExceptionMapper implements ExceptionMapper<Exception> {

    private static Logger LOGGER = LogManager.getLogger();

    @Context
    HttpHeaders headers;

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(Exception exc) {
        LOGGER.warn(() -> "Error while processing request " + request.getRequestURI(), exc);
        Response response = getResponse(exc);
        if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
            // try to return a html error page
            if (!MCRSessionMgr.getCurrentSession().isTransactionActive()) {
                MCRSessionMgr.getCurrentSession().beginTransaction();
            }
            try {
                int status = response.getStatus();
                String source = exc.getStackTrace().length > 0 ? exc.getStackTrace()[0].toString() : null;
                Document errorPageDocument = MCRErrorServlet.buildErrorPage(exc.getMessage(), status,
                    request.getRequestURI(), null, source, exc);
                MCRContent result = MCRJerseyUtil.transform(errorPageDocument, request);
                return Response.serverError()
                    .entity(result.getInputStream())
                    .type(MediaType.valueOf(result.getMimeType()))
                    .status(status)
                    .build();
            } catch (Exception transformException) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(transformException).build();
            } finally {
                MCRSessionMgr.getCurrentSession().commitTransaction();
            }
        }
        return response;
    }

    private Response getResponse(Exception exc) {
        if (exc instanceof WebApplicationException) {
            return ((WebApplicationException) exc).getResponse();
        }

        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(exc).build();
    }

}
