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

import java.beans.Transient;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.mycore.restapi.converter.MCRInstantXMLAdapter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class MCRErrorResponse {

    UUID uuid;

    Instant timestamp;

    String errorCode;

    Throwable cause;

    String message;

    String detail;

    int status;

    public static MCRErrorResponse fromStatus(int status) {
        final MCRErrorResponse response = new MCRErrorResponse();
        response.setStatus(status);
        return response;
    }

    private MCRErrorResponse() {
        uuid = UUID.randomUUID();
        timestamp = Instant.now();
    }

    public WebApplicationException toException() {
        WebApplicationException e;
        Response.Status s = Response.Status.fromStatusCode(status);
        final Response response = Response.status(s)
            .entity(this)
            .build();
        //s maybe null
        switch (Response.Status.Family.familyOf(status)) {
            case CLIENT_ERROR:
                //Response.Status.OK is to trigger "default" case
                switch (s != null ? s : Response.Status.OK) {
                    case BAD_REQUEST:
                        e = new BadRequestException(getMessage(), response, getCause());
                        break;
                    case FORBIDDEN:
                        e = new ForbiddenException(getMessage(), response, getCause());
                        break;
                    case NOT_ACCEPTABLE:
                        e = new NotAcceptableException(getMessage(), response, getCause());
                        break;
                    case METHOD_NOT_ALLOWED:
                        e = new NotAllowedException(getMessage(), response, getCause());
                        break;
                    case UNAUTHORIZED:
                        e = new NotAuthorizedException(getMessage(), response, getCause());
                        break;
                    case NOT_FOUND:
                        e = new NotFoundException(getMessage(), response, getCause());
                        break;
                    case UNSUPPORTED_MEDIA_TYPE:
                        e = new NotSupportedException(getMessage(), response, getCause());
                        break;
                    default:
                        e = new ClientErrorException(getMessage(), response, getCause());
                }
                break;
            case SERVER_ERROR:
                //Response.Status.OK is to trigger "default" case
                switch (s != null ? s : Response.Status.OK) {
                    case INTERNAL_SERVER_ERROR:
                        e = new InternalServerErrorException(getMessage(), response, getCause());
                        break;
                    default:
                        e = new ServerErrorException(getMessage(), response, getCause());
                }
                break;
            default:
                e = new WebApplicationException(getMessage(), getCause(), response);
        }
        LogManager.getLogger().error(this::getLogMessage, e);
        return e;
    }

    String getLogMessage() {
        return getUuid() + " - " + getErrorCode() + ": " + getMessage();
    }

    @XmlAttribute
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @XmlAttribute
    public String getErrorCode() {
        return Optional.ofNullable(errorCode).filter(s -> !s.isBlank()).orElse("UNKNOWN");
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Transient
    @XmlTransient
    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @XmlElement
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @XmlAttribute
    @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp.toInstant();
    }

    @XmlTransient
    @Transient
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public MCRErrorResponse withCause(Throwable cause) {
        setCause(cause);
        return this;
    }

    public MCRErrorResponse withMessage(String message) {
        setMessage(message);
        return this;
    }

    public MCRErrorResponse withDetail(String detail) {
        setDetail(detail);
        return this;
    }

    public MCRErrorResponse withErrorCode(String errorCode) {
        setErrorCode(errorCode);
        return this;
    }

    @Override
    public String toString() {
        return "MCRErrorResponse{" +
            "uuid=" + uuid +
            ", status=" + status +
            ", timestamp=" + timestamp +
            ", errorCode='" + errorCode + '\'' +
            ", message='" + message + '\'' +
            ", detail='" + detail + '\'' +
            ", cause=" + cause +
            '}';
    }
}
