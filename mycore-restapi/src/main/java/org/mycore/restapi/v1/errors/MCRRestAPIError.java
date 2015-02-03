/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
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

package org.mycore.restapi.v1.errors;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Vector;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.stream.JsonWriter;

/**
 * stores error informations during REST requests
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
public class MCRRestAPIError {
    Response.Status status = Response.Status.BAD_REQUEST;

    String message = "";

    String details = null;

    List<MCRRestAPIFieldError> errors = new Vector<MCRRestAPIFieldError>();

    public MCRRestAPIError(Response.Status status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public static MCRRestAPIError create(Response.Status status, String message, String details) {
        return new MCRRestAPIError(status, message, details);
    }

    public void addFieldError(MCRRestAPIFieldError error) {
        errors.add(error);
    }

    public String toJSONString() {
        StringWriter sw = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(sw);
            writer.setIndent("    ");
            writer.beginObject();
            writer.name("status").value(status.getStatusCode());
            writer.name("message").value(message);
            if (details != null) {
                writer.name("details").value(details);
            }
            if (errors.size() > 0) {
                writer.name("errors");
                writer.beginArray();
                for (MCRRestAPIFieldError err : errors) {
                    writer.beginObject();
                    writer.name("field").value(err.getField());
                    writer.name("message").value(err.getMessage());
                    writer.endObject();
                }
                writer.endArray();
            }

            writer.endObject();
            writer.close();
        } catch (IOException e) {
            //should not happen;
        }

        return sw.toString();
    }

    public Response createHttpResponse() {
        return Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(toJSONString()).build();
    }

    public List<MCRRestAPIFieldError> getFieldErrors() {
        return errors;
    }
}
