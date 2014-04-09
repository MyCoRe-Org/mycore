package org.mycore.wcms2.navigation;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.wcms2.MCRWebPagesSynchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class provides methods to get, create and save MyCoRe webpages.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSContentManager {

    private static final Logger LOGGER = Logger.getLogger(MCRWCMSContentManager.class);

    private MCRWCMSSectionProvider sectionProvider;

    public enum ErrorType {
        notExist, invalidFile, notMyCoReWebPage, invalidDirectory, couldNotSave
    }

    public MCRWCMSContentManager() {
        MCRConfiguration conf = MCRConfiguration.instance();
        Object sectionProvider = conf.getInstanceOf("MCR.WCMS2.sectionProvider",
            MCRWCMSDefaultSectionProvider.class.getName());
        if (!(sectionProvider instanceof MCRWCMSSectionProvider)) {
            LOGGER.error("MCR.WCMS2.sectionProvider is not an instance of SectionProvider");
            return;
        }
        this.sectionProvider = (MCRWCMSSectionProvider) sectionProvider;
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
     * looks like:<br />
     * {
     *  type: "error",
     *  errorType: "invalidFile"
     *  webpageId: "myfolder/webpage1.xml"
     * }
     * </p>
     *
     * @param webpageId id of the webpage
     * @return json object
     * @throws IOException
     * @throws jdomException
     * @see ErrorType
     */
    public JsonObject getContent(String webpageId) throws IOException, JDOMException {
        File webDir = MCRWebPagesSynchronizer.getWebAppBaseDir();
        File xmlFile = new File(webDir, webpageId);
        boolean isXML = xmlFile.getName().endsWith(".xml");
        // file is not in web application directory
        if (!xmlFile.getCanonicalPath().startsWith(webDir.getCanonicalPath())) {
            throwError(ErrorType.invalidDirectory, webpageId);
        } else if (!xmlFile.exists() && isXML) {
            throwError(ErrorType.notExist, webpageId);
        } else if (!xmlFile.isFile() || !isXML) {
            throwError(ErrorType.invalidFile, webpageId);
        }

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlFile);
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
     * json looks like:<br />
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
     * @param items
     */
    public void save(JsonArray items) {
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
        for (JsonElement e : items) {
            if (!e.isJsonObject()) {
                LOGGER.warn("Invalid json element in items " + e);
                continue;
            }
            JsonObject item = e.getAsJsonObject();
            if (!item.has("dirty") || !item.get("dirty").getAsBoolean()) {
                continue;
            }
            if (!item.has("href") || !item.get("href").isJsonPrimitive()) {
                continue;
            }
            //TODO wenn man nur den href ändert und nicht den content muss die datei
            // trotzdem umgeschrieben werden -> check auf file exists
            if (!item.has("content") || !item.get("content").isJsonArray()) {
                continue;
            }
            String webpageId = item.get("href").getAsString();
            JsonArray content = item.get("content").getAsJsonArray();

            Element mycoreWebpage = this.sectionProvider.fromJSON(content);
            if (!webpageId.endsWith(".xml")) {
                throwError(ErrorType.invalidFile, webpageId);
            }
            // save
            try (OutputStream fout = MCRWebPagesSynchronizer.getOutputStream(webpageId)) {
                out.output(new Document(mycoreWebpage), fout);
            } catch (Exception exc) {
                LOGGER.error("Error while saving " + webpageId, exc);
                throwError(ErrorType.couldNotSave, webpageId);
            }
        }
    }

    /**
     * Throws a new webapplication exception with given error type.
     * 
     * @param errorType type of the error
     * @param webpageId webpageId where the error occur
     * @return { type: "error", errorType: "typeOfError", webpageId: "abc.xml" }
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
