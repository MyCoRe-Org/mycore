package org.mycore.wcms2.navigation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRURLContent;
import org.mycore.tools.MyCoReWebPageProvider;
import org.mycore.wcms2.MCRWebPagesSynchronizer;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class provides methods to get, create and save MyCoRe webpages.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSContentManager {

    private static final Logger LOGGER = LogManager.getLogger(MCRWCMSContentManager.class);

    private MCRWCMSSectionProvider sectionProvider;

    public enum ErrorType {
        notExist, invalidFile, notMyCoReWebPage, invalidDirectory, couldNotSave, couldNotMove
    }

    public MCRWCMSContentManager() {
        MCRConfiguration conf = MCRConfiguration.instance();
        this.sectionProvider = conf.getInstanceOf("MCR.WCMS2.sectionProvider",
            MCRWCMSDefaultSectionProvider.class.getName());
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
    public JsonObject getContent(String webpageId) throws IOException, JDOMException, SAXException {
        boolean isXML = webpageId.endsWith(".xml");
        URL resourceURL = null;
        try {
            resourceURL = MCRWebPagesSynchronizer.getURL(webpageId);
        } catch (MalformedURLException e) {
            throwError(ErrorType.invalidDirectory, webpageId);
        }
        // file is not in web application directory
        if (!isXML) {
            throwError(ErrorType.invalidFile, webpageId);
        }
        Document doc = null;
        if (resourceURL == null) {
            MyCoReWebPageProvider wpp = new MyCoReWebPageProvider();
            wpp.addSection("neuer Eintrag", new Element("p").setText("TODO"), "de");
            doc = wpp.getXML();
        } else {
            doc = new MCRURLContent(resourceURL).asXML();
        }
        Element rootElement = doc.getRootElement();
        if (!rootElement.getName().equals("MyCoReWebPage")) {
            throwError(ErrorType.notMyCoReWebPage, webpageId);
        }
        // return content
        return getContent(rootElement);
    }

    public JsonObject getContent(Element e) {
        JsonArray sectionArray = this.sectionProvider.toJSON(e);
        JsonObject content = new JsonObject();
        content.addProperty("type", "content");
        content.add("content", sectionArray);
        return content;
    }

    /**
     * Saves the content of the given items.
     * 
     * Returns a json object, if everything is ok:
     * <p>
     * { type: "saveDone" }
     * </p>
     * 
     * <p>
     * If one or more files could'nt be saved because of an error the returning
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
        XMLOutputter out = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8"));
        for (JsonElement e : items) {
            if (!e.isJsonObject()) {
                LOGGER.warn("Invalid json element in items " + e);
                continue;
            }
            JsonObject item = e.getAsJsonObject();
            if (!item.has("dirty") || !item.get("dirty").getAsBoolean()) {
                continue;
            }

            JsonElement webpageIdElement = item.has("href") ? item.get("href") : (item.has("hrefStartingPage") ? item
                .get("hrefStartingPage") : null);
            if (webpageIdElement == null || !webpageIdElement.isJsonPrimitive()) {
                continue;
            }
            //TODO wenn man nur den href ändert und nicht den content muss die datei
            // trotzdem umgeschrieben werden -> check auf file exists
            if (!item.has("content") || !item.get("content").isJsonArray()) {
                continue;
            }
            String webpageId = webpageIdElement.getAsString();
            if (!webpageId.endsWith(".xml")) {
                throwError(ErrorType.invalidFile, webpageId);
            }
            JsonArray content = item.get("content").getAsJsonArray();
            Element mycoreWebpage = this.sectionProvider.fromJSON(content);
            // save
            try (OutputStream fout = MCRWebPagesSynchronizer.getOutputStream(webpageId)) {
                out.output(new Document(mycoreWebpage), fout);
            } catch (Exception exc) {
                LOGGER.error("Error while saving " + webpageId, exc);
                throwError(ErrorType.couldNotSave, webpageId);
            }
        }
    }

    public void move(String from, String to) {
        try {
            // get from
            URL fromURL = MCRWebPagesSynchronizer.getURL(from);
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
            try (OutputStream fout = MCRWebPagesSynchronizer.getOutputStream(to)) {
                out.output(document, fout);
            }
            // delete old
            if (fromURL != null) {
                Files.delete(Paths.get(fromURL.toURI()));
            }
        } catch (Exception exc) {
            LOGGER.error("Error moving " + from + " to " + to, exc);
            throwError(ErrorType.couldNotMove, to);
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
        error.addProperty("type", errorType.name());
        error.addProperty("webpageId", webpageId);
        Response response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(error.toString())
            .type(MediaType.APPLICATION_JSON).build();
        throw new WebApplicationException(response);
    }

}
