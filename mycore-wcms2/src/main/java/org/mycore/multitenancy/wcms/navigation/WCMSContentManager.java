package org.mycore.multitenancy.wcms.navigation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class provides methods to get, create and save MyCoRe webpages.
 *
 * @author Matthias Eichner
 */
public class WCMSContentManager {

    private static final Logger LOGGER = Logger.getLogger(WCMSContentManager.class);

    private static File webDir = null;

    private SectionProvider sectionProvider;

    public enum ErrorType {
        notExist, invalidFile, notMyCoReWebPage, invalidDirectory, couldNotSave
    }

    static {
        String webpath = MCRConfiguration.instance().getString("MCR.WebApplication.basedir", null);
        if (webpath == null) {
            LOGGER.error("Unable to get web application directory! Set MCR.WebApplication.basedir.");
        } else {
            webDir = new File(webpath);
        }
    }

    public WCMSContentManager() {
        MCRConfiguration conf = MCRConfiguration.instance();
        Object sectionProvider = conf.getInstanceOf("MCR.WCMS2.sectionProvider", DefaultSectionProvider.class.getName());
        if(!(sectionProvider instanceof SectionProvider)) {
            LOGGER.error("MCR.WCMS2.sectionProvider is not an instance of SectionProvider");
            return;
        }
        this.sectionProvider = (SectionProvider)sectionProvider;
    }

    /**
     * Return a json object with the content of a MyCoRe webpage.
     * <p>
     * {
     *  type: "content",
     *  content: @see {@link DefaultSectionProvider}
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
        File xmlFile = new File(webDir, webpageId);
        boolean isXML = xmlFile.getName().endsWith(".xml");
        // file is not in web application directory
        if (!xmlFile.getCanonicalPath().startsWith(webDir.getCanonicalPath()))
            return getError(ErrorType.invalidDirectory, webpageId);
        if (!xmlFile.exists() && isXML)
            return getError(ErrorType.notExist, webpageId);
        if (!xmlFile.isFile() || !isXML)
            return getError(ErrorType.invalidFile, webpageId);

        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlFile);
        Element rootElement = doc.getRootElement();
        if (!rootElement.getName().equals("MyCoReWebPage"))
            return getError(ErrorType.notMyCoReWebPage, webpageId);

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
    public JsonObject save(JsonArray items) {
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
        JsonArray errorArr = new JsonArray();

        for(JsonElement e : items) {
            if(!e.isJsonObject()) {
                LOGGER.warn("Invalid json element in items " + e);
                continue;
            }
            JsonObject item = e.getAsJsonObject();
            if(!item.has("dirty") || !item.get("dirty").getAsBoolean()) {
                continue;
            }
            if(!item.has("href") || !item.get("href").isJsonPrimitive()) {
                continue;
            }
            //TODO wenn man nur den href ändert und nicht den content muss die datei
            // trotzdem umgeschrieben werden -> check auf file exists
            if(!item.has("content") || !item.get("content").isJsonArray()) {
                continue;
            }
            String webpageId = item.get("href").getAsString();
            JsonArray content = item.get("content").getAsJsonArray();

            Element mycoreWebpage = this.sectionProvider.fromJSON(content);
            File xmlFile = new File(webDir, webpageId);
            // file is not in web application directory
            try {
                if (!xmlFile.getCanonicalPath().startsWith(webDir.getCanonicalPath())) {
                    throw new IOException();
                }
            } catch(IOException ioExc) {
                errorArr.add(getError(ErrorType.invalidDirectory, webpageId));
                continue;
            }
            if (!xmlFile.getName().endsWith(".xml")) {
                errorArr.add(getError(ErrorType.invalidFile, webpageId));
                continue;
            }
            // save
            try {  
                FileOutputStream fout = new FileOutputStream(xmlFile);
                out.output(new Document(mycoreWebpage), fout);
                fout.close();
            } catch(Exception exc) {
                LOGGER.error("while saving webpage " + xmlFile.getAbsolutePath(), exc);
                errorArr.add(getError(ErrorType.couldNotSave, webpageId));
            }
        }

        // save webpage content
        JsonObject returnObj = new JsonObject();
        if(errorArr.size() > 0) {
            returnObj.addProperty("type", "error");
            returnObj.add("errorArray", errorArr);
        } else {
            returnObj.addProperty("type", "saveDone");
        }
        return returnObj;
    }

    /**
     * Creates an json error object.
     * 
     * @param errorType type of the error
     * @param webpageId webpageId where the error occur
     * @return { type: "error", errorType: "typeOfError", webpageId: "abc.xml" }
     */
    private JsonObject getError(ErrorType errorType, String webpageId) {
        JsonObject error = new JsonObject();
        error.addProperty("type", "error");
        error.addProperty("errorType", errorType.name());
        error.addProperty("webpageId", webpageId);
        return error;
    }

}
