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

package org.mycore.frontend.jersey.resources;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
    HttpServletRequest request; //required for MCRParamCollector

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exc) {
        Response response = getResponse(exc);
        if (exc instanceof WebApplicationException && !(exc instanceof InternalServerErrorException)) {
            LOGGER.warn("{}: {}, {}", uriInfo.getRequestUri(), response.getStatus(), exc.getMessage());
            LOGGER.debug("Exception: ", exc);
        } else {
            LOGGER.warn(() -> "Error while processing request " + uriInfo.getRequestUri(), exc);
        }
        if (headers.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE)) {
            // try to return a html error page
            if (!MCRSessionMgr.getCurrentSession().isTransactionActive()) {
                MCRSessionMgr.getCurrentSession().beginTransaction();
            }
            try {
                int status = response.getStatus();
                String source = exc.getStackTrace().length > 0 ? exc.getStackTrace()[0].toString() : null;
                Document errorPageDocument = MCRErrorServlet.buildErrorPage(exc.getMessage(), status,
                    uriInfo.getRequestUri().toASCIIString(), null, source, exc);
                MCRContent result = MCRJerseyUtil.transform(errorPageDocument, request);
                return Response.serverError()
                    .entity(result.getInputStream())
                    .type(result.getMimeType())
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
            Response response = ((WebApplicationException) exc).getResponse();
            if (response.hasEntity()) {
                return response;
            }
            return Response.fromResponse(response)
                .entity(exc.getMessage())
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build();
        }
        Object entity = Optional.ofNullable(request.getContentType())
            .map(MediaType::valueOf)
            .filter(t -> t.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE) || t
                .isCompatible(MediaType.MULTIPART_FORM_DATA_TYPE))
            .map(t -> (Object) exc.getMessage()) //do not container form requests responses
            .orElseGet(() -> new MCRExceptionContainer(exc));
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(entity).build();
    }

    @XmlRootElement(name = "error")
    private static class MCRExceptionContainer{
        private Exception exception;

        private MCRExceptionContainer() {
            //required for JAXB
        }

        public MCRExceptionContainer(Exception e){
            this.exception=e;
        }

        @XmlElement
        @XmlJavaTypeAdapter(ExceptionTypeAdapter.class)
        public Exception getException(){
            return exception;
        }

        @XmlAttribute
        public Class<? extends Exception> getType() {
            return exception.getClass();
        }

    }

    private static class ExceptionTypeAdapter extends XmlAdapter<RException, Exception> {
        @Override
        public Exception unmarshal(RException v) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public RException marshal(Exception v) {
            RException ep = new RException();
            ep.message = v.getMessage();
            ep.stackTrace = Stream.of(v.getStackTrace()).map(RStackTraceElement::getInstance)
                .toArray(RStackTraceElement[]::new);
            Optional.ofNullable(v.getCause())
                .filter(Exception.class::isInstance)
                .map(Exception.class::cast)
                .map(this::marshal)
                .ifPresent(c -> ep.cause = c);
            return ep;
        }
    }

    private static class RException {
        @XmlElement
        private String message;

        @XmlElement
        private RStackTraceElement[] stackTrace;

        @XmlElement
        private RException cause;

    }

    private static class RStackTraceElement {
        @XmlAttribute
        private String className;

        @XmlAttribute
        private String method;

        @XmlAttribute
        private String file;

        @XmlAttribute
        private int line;

        private static RStackTraceElement getInstance(StackTraceElement ste) {
            RStackTraceElement rste = new RStackTraceElement();
            rste.className = ste.getClassName();
            rste.method = ste.getMethodName();
            rste.file = ste.getFileName();
            rste.line = ste.getLineNumber();
            return rste;
        }
    }

}
