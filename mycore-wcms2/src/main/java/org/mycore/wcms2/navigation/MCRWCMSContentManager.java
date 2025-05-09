/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.wcms2.navigation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRURLContent;
import org.mycore.resource.MCRResourceHelper;
import org.mycore.tools.MyCoReWebPageProvider;
import org.mycore.wcms2.MCRWCMSUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * This class provides methods to get, create and save MyCoRe webpages.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSContentManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String JSON_PROPERTY_TYPE = "type";

    private static final String JSON_PROPERTY_CONTENT = "content";

    private static final String JSON_PROPERTY_WEBPAGE_ID = "webpageId";

    private static final String JSON_PROPERTY_DIRTY = "dirty";

    private final MCRWCMSSectionProvider sectionProvider;

    public enum ErrorType {
        NOT_EXIST, INVALID_FILE, NOT_MYCORE_WEBPAGE, INVALID_DIRECTORY, COULD_NOT_SAVE, COULD_NOT_MOVE
    }

    public MCRWCMSContentManager() {
        this.sectionProvider = MCRConfiguration2.getInstanceOfOrThrow(
            MCRWCMSSectionProvider.class, "MCR.WCMS2.sectionProvider");
    }

    /**
     * Return a json object with the content of a MyCoRe webpage.
     * <p>
     * {
     *  type: "content",
     *  content: @see {@link MCRWCMSDefaultSectionProvider}
     * }
     * </p>
     * <p>
     * If an error occur (e.g. file not exist) the returning json
     * looks like:<br>
     * {
     *  type: "error",
     *  errorType: "invalidFile"
     *  webpageId: "myfolder/webpage1.xml"
     * }
     * </p>
     *
     * @param webpageId id of the webpage
     * @return json object
     * @see ErrorType
     */
    public JsonObject getContent(String webpageId) throws IOException, JDOMException {
        boolean isXML = webpageId.endsWith(".xml");
        URL resourceURL = MCRResourceHelper.getWebResourceUrl(webpageId);
        // file is not in web application directory
        if (!isXML) {
            throwError(ErrorType.INVALID_FILE, webpageId);
        }
        Document doc;
        if (resourceURL == null) {
            MyCoReWebPageProvider wpp = new MyCoReWebPageProvider();
            wpp.addSection("neuer Eintrag", new Element("p").setText("TODO"), "de");
            doc = wpp.getXML();
        } else {
            doc = new MCRURLContent(resourceURL).asXML();
        }
        Element rootElement = doc.getRootElement();
        if (!"MyCoReWebPage".equals(rootElement.getName())) {
            throwError(ErrorType.NOT_MYCORE_WEBPAGE, webpageId);
        }
        // return content
        return getContent(rootElement);
    }

    public JsonObject getContent(Element e) {
        JsonArray sectionArray = this.sectionProvider.toJSON(e);
        JsonObject content = new JsonObject();
        content.addProperty(JSON_PROPERTY_TYPE, "content");
        content.add(JSON_PROPERTY_CONTENT, sectionArray);
        return content;
    }

    /**
     * Saves the content of the given items.
     * <p>
     * Returns a json object, if everything is ok:
     * <p>
     * { type: "saveDone" }
     * </p>
     * 
     * <p>
     * If one or more files couldn't be saved because of an error the returning
     * json looks like:<br>
     * {
     *   type: "error",
     *   errorArray: [
     *       {type: "error", errorType: "invalidDirectory", webpageId: "abc.xml"},
     *       {type: "error", errorType: "invalidFile", webpageId: "abc2.xml"}
     *       ...
     *  ]
     * }
     * </p>
     * 
     */
    public void save(JsonArray items) {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8"));
        for (JsonElement e : items) {
            validateContent(e).ifPresent(item -> {
                String webpageId = getWebPageId(item).get();
                if (!webpageId.endsWith(".xml")) {
                    throwError(ErrorType.INVALID_FILE, webpageId);
                }
                JsonArray content = item.get(JSON_PROPERTY_CONTENT).getAsJsonArray();
                Element mycoreWebpage = this.sectionProvider.fromJSON(content);
                // save
                try (OutputStream out = MCRWCMSUtil.getOutputStream(webpageId)) {
                    xmlOutputter.output(new Document(mycoreWebpage), out);
                } catch (IOException | RuntimeException exc) {
                    LOGGER.error("Error while saving {}", webpageId, exc);
                    throwError(ErrorType.COULD_NOT_SAVE, webpageId);
                }
            });
        }
    }

    private Optional<String> getWebPageId(JsonObject item) {
        JsonElement webpageIdElement = item.has("href") ? item.get("href")
            : item.has("hrefStartingPage") ? item.get("hrefStartingPage") : null;
        if (webpageIdElement == null || !webpageIdElement.isJsonPrimitive()) {
            return Optional.empty();
        }
        return Optional.of(webpageIdElement).map(JsonElement::getAsString);
    }

    private Optional<JsonObject> validateContent(JsonElement e) {
        if (!e.isJsonObject()) {
            LOGGER.warn("Invalid json element in items {}", e);
            return Optional.empty();
        }
        JsonObject item = e.getAsJsonObject();
        if (!item.has(JSON_PROPERTY_DIRTY) || !item.get(JSON_PROPERTY_DIRTY).getAsBoolean()) {
            return Optional.empty();
        }
        //TODO wenn man nur den href ändert und nicht den content muss die datei
        // trotzdem umgeschrieben werden -> check auf file exists
        if (getWebPageId(item).isEmpty() || !item.has(JSON_PROPERTY_CONTENT)
            || !item.get(JSON_PROPERTY_CONTENT).isJsonArray()) {
            return Optional.empty();
        }
        return Optional.of(item);
    }

    public void move(String from, String to) {
        try {
            // get from
            URL fromURL = MCRResourceHelper.getWebResourceUrl(from);
            Document document;
            if (fromURL == null) {
                // if the from resource couldn't be found we assume its not created yet.
                MyCoReWebPageProvider wpp = new MyCoReWebPageProvider();
                wpp.addSection("neuer Eintrag", new Element("p").setText("TODO"), "de");
                document = wpp.getXML();
            } else {
                SAXBuilder builder = new SAXBuilder();
                document = builder.build(fromURL);
            }
            // save
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
            try (OutputStream fout = MCRWCMSUtil.getOutputStream(to)) {
                out.output(document, fout);
            }
            // delete old
            if (fromURL != null) {
                Files.delete(Paths.get(fromURL.toURI()));
            }
        } catch (Exception exc) {
            LOGGER.error("Error moving {} to {}", from, to, exc);
            throwError(ErrorType.COULD_NOT_MOVE, to);
        }
    }

    /**
     * Throws a new webapplication exception with given error type.
     * <code>{ type: "error", errorType: "typeOfError", webpageId: "abc.xml" }</code>
     * 
     * @param errorType type of the error
     * @param webpageId webpageId where the error occur
     */
    private void throwError(ErrorType errorType, String webpageId) {
        JsonObject error = new JsonObject();
        error.addProperty(JSON_PROPERTY_TYPE, errorType.name());
        error.addProperty(JSON_PROPERTY_WEBPAGE_ID, webpageId);
        Response response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.toString())
            .type(MediaType.APPLICATION_JSON).build();
        throw new WebApplicationException(response);
    }

}
