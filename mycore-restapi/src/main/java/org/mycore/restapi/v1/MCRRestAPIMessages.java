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

package org.mycore.restapi.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.restapi.v1.errors.MCRRestAPIException;
import org.mycore.restapi.v1.utils.MCRJSONWebTokenUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil;
import org.mycore.restapi.v1.utils.MCRRestAPIUtil.MCRRestAPIACLPermission;
import org.mycore.services.i18n.MCRTranslation;

import com.google.gson.stream.JsonWriter;

/**
 * REST API for messages.
 * Allows access to message properties of the application
 * 
 *  
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 */
@Path("/v1/messages")
public class MCRRestAPIMessages {

    private static final String HEADER_NAME_AUTHORIZATION = "Authorization";

    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String FORMAT_PROPERTY = "property";

    /**
     * lists all message properties for a given language
     * 
     * @param info - the injected Jersey Context Object for URI
     * 
     * @param request - the injected HTTPServletRequest object
     * 
     * @param lang - the language in which the messages should be returned (default: 'de')
     *      
     * @param format
     * 		Possible values are: props (default) | json | xml
     * @param filter
     * 		';'-separated list of message key prefixes
     * 
     * @return a Jersey Response object
     * 
     * @throws MCRRestAPIException
     */
    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=ISO-8859-1" })
    public Response listMessages(@Context UriInfo info, @Context HttpServletRequest request,
        @QueryParam("lang") @DefaultValue("de") String lang,
        @QueryParam("format") @DefaultValue("property") String format,
        @QueryParam("filter") @DefaultValue("") String filter) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/messages");
        Locale locale = Locale.forLanguageTag(lang);
        String[] check = filter.split(";");

        Properties data = MCRConfiguration.sortProperties(null);
        for (String prefix : check) {
            data.putAll(MCRTranslation.translatePrefix(prefix, locale));
        }
        String authHeader = MCRJSONWebTokenUtil
            .createJWTAuthorizationHeader(MCRJSONWebTokenUtil.retrieveAuthenticationToken(request));
        try {
            if (FORMAT_PROPERTY.equals(format)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                data.store(baos, "MyCoRe Messages (charset='ISO-8859-1')");
                return Response.ok(baos.toByteArray()).type("text/plain; charset=ISO-8859-1").build();
            }
            if (FORMAT_XML.equals(format)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                data.storeToXML(baos, "MyCoRe Messages");
                return Response.ok(baos.toString("UTF-8")).type("application/xml; charset=UTF-8").build();
            }
            if (FORMAT_JSON.equals(format)) {
                StringWriter sw = new StringWriter();

                JsonWriter writer = new JsonWriter(sw);
                writer.setIndent("    ");
                writer.beginObject();
                writer.name("messages");
                writer.beginObject();
                for (Object key : data.keySet()) {
                    writer.name(key.toString());
                    writer.value(data.getProperty(key.toString()));
                }
                writer.endObject();
                writer.endObject();
                writer.close();

                return Response.ok(sw.toString()).type("application/json; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            }
        } catch (IOException e) {
            //toDo
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * returns a single messages entry.
     * 
     * @param info - the injected Jersey context object for URI
     * @param request - the injected HTTPServletRequest object
     * @param key - the message key
     * @param lang - the language
     * @param format 
     *     Possible values are: props (default) | json | xml (required)
     * @return a Jersey Response Object
     * @throws MCRRestAPIException    
     */
    @GET
    @Path("/{value}")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=UTF-8" })
    public Response getMessage(@Context UriInfo info, @Context HttpServletRequest request,
        @PathParam("value") String key, @QueryParam("lang") @DefaultValue("de") String lang,
        @QueryParam("format") @DefaultValue("text") String format) throws MCRRestAPIException {
        MCRRestAPIUtil.checkRestAPIAccess(request, MCRRestAPIACLPermission.READ, "/v1/messages");
        Locale locale = Locale.forLanguageTag(lang);
        String result = MCRTranslation.translate(key, locale);
        String authHeader = MCRJSONWebTokenUtil
            .createJWTAuthorizationHeader(MCRJSONWebTokenUtil.retrieveAuthenticationToken(request));
        try {
            if (FORMAT_PROPERTY.equals(format)) {
                return Response.ok(key + "=" + result).type("text/plain; charset=ISO-8859-1")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            }
            if (FORMAT_XML.equals(format)) {
                Document doc = new Document();
                Element root = new Element("entry");
                root.setAttribute("key", key);
                root.setText(result);
                doc.addContent(root);
                StringWriter sw = new StringWriter();
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                outputter.output(doc, sw);
                return Response.ok(sw.toString()).type("application/xml; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            }
            if (FORMAT_JSON.equals(format)) {
                StringWriter sw = new StringWriter();
                JsonWriter writer = new JsonWriter(sw);
                writer.setIndent("    ");
                writer.beginObject();
                writer.name(key);
                writer.value(result);
                writer.endObject();
                writer.close();
                return Response.ok(sw.toString()).type("application/json; charset=UTF-8")
                    .header(HEADER_NAME_AUTHORIZATION, authHeader).build();
            }
            //text only
            return Response.ok(result).type("text/plain; charset=UTF-8").header(HEADER_NAME_AUTHORIZATION, authHeader)
                .build();
        } catch (IOException e) {
            //toDo
        }
        return Response.status(Status.BAD_REQUEST).build();
    }
}
