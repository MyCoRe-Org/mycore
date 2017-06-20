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

package org.mycore.restapi.v1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Properties;

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
import org.mycore.frontend.jersey.MCRStaticContent;
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
@MCRStaticContent
public class MCRRestAPIMessages {
    public static final String FORMAT_JSON = "json";

    public static final String FORMAT_XML = "xml";

    public static final String FORMAT_PROPERTY = "property";

    /**
     * 
     * @param info - a Jersey Context Object for URI
     *      
     * @param format
     * 		Possible values are: props (default) | json | xml
     * @param filter
     * 		';'-separated list of message key prefixes
     */
    @GET
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=ISO-8859-1" })
    public Response listMessages(@Context UriInfo info, @QueryParam("lang") @DefaultValue("de") String lang,
        @QueryParam("format") @DefaultValue("property") String format,
        @QueryParam("filter") @DefaultValue("") String filter) {

        Locale locale = Locale.forLanguageTag(lang);
        String[] check = filter.split(";");

        Properties data = MCRConfiguration.sortProperties(null);
        for (String prefix : check) {
            data.putAll(MCRTranslation.translatePrefix(prefix, locale));
        }

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

                return Response.ok(sw.toString()).type("application/json; charset=UTF-8").build();
            }
        } catch (IOException e) {
            //toDo
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    /**
     * returns a single messages entry.
     * 
     * @param info - a Jersey Context Object for URI
     * @param format 
     *     Possible values are: props (default) | json | xml (required)
     */
    @GET
    @Path("/{value}")
    @Produces({ MediaType.TEXT_XML + ";charset=UTF-8", MediaType.APPLICATION_JSON + ";charset=UTF-8",
        MediaType.TEXT_PLAIN + ";charset=UTF-8" })
    public Response getMessage(@Context UriInfo info, @PathParam("value") String key,
        @QueryParam("lang") @DefaultValue("de") String lang,
        @QueryParam("format") @DefaultValue("text") String format) {

        Locale locale = Locale.forLanguageTag(lang);
        String result = MCRTranslation.translate(key, locale);
        try {
            if (FORMAT_PROPERTY.equals(format)) {
                return Response.ok(key + "=" + result).type("text/plain; charset=ISO-8859-1").build();
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
                return Response.ok(sw.toString()).type("application/xml; charset=UTF-8").build();
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
                return Response.ok(sw.toString()).type("application/json; charset=UTF-8").build();
            }
            //text only
            return Response.ok(result).type("text/plain; charset=UTF-8").build();
        } catch (IOException e) {
            //toDo
        }
        return Response.status(Status.BAD_REQUEST).build();
    }
}
