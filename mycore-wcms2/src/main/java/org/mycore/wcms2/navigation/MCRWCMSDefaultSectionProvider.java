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

package org.mycore.wcms2.navigation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.tools.MyCoReWebPageProvider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.xml.sax.SAXParseException;

import javax.ws.rs.WebApplicationException;

/**
 * The default implementation to convert MyCoRe Webpage sections
 * from and to json.
 * 
 * @author Matthias Eichner
 */
public class MCRWCMSDefaultSectionProvider implements MCRWCMSSectionProvider {
    private static final Logger LOGGER = LogManager.getLogger(MCRWCMSDefaultSectionProvider.class);

    private static final List<String> HTML_TAG_LIST = Arrays.asList("html", "head", "title", "base", "link", "meta",
        "style", "script", "noscript", "body", "body", "section", "nav", "article", "aside", "h1", "h2", "h3", "h4",
        "h5", "h6", "header", "footer", "address", "main", "p", "hr", "pre", "blockquote", "ol", "ul", "li", "dl", "dt",
        "dd", "figure", "figcaption", "div", "a", "em", "strong", "small", "s", "cite", "q", "dfn", "abbr", "data",
        "time", "code", "var", "samp", "kbd", "sub", "sup", "i", "b", "u", "mark", "ruby", "rt", "rp", "bdi", "bdo",
        "span", "br", "wbr", "ins", "del", "img", "iframe", "embed", "object", "param", "video", "audio", "source",
        "track", "canvas", "map", "area", "svg", "math", "table", "caption", "colgroup", "col", "tbody", "thead",
        "tfoot", "tr", "td", "th", "form", "fieldset", "legend", "label", "input", "button", "select", "datalist",
        "optgroup", "option", "textarea", "keygen", "output", "progress", "meter", "details", "summary", "menuitem",
        "menu", "font");

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private List<String> mycoreTagList = new ArrayList<>();

    public MCRWCMSDefaultSectionProvider() {
        String mycoreTagListString = CONFIG.getString("MCR.WCMS2.mycoreTagList", "");
        for (String tag : mycoreTagListString.split(",")) {
            mycoreTagList.add(tag.trim());
        }
    }

    public JsonArray toJSON(Element rootElement) {
        JsonArray sectionArray = new JsonArray();
        for (Element section : rootElement.getChildren(MyCoReWebPageProvider.XML_SECTION)) {
            // get infos of element
            String title = section.getAttributeValue(MyCoReWebPageProvider.XML_TITLE);
            String lang = section.getAttributeValue(MyCoReWebPageProvider.XML_LANG, Namespace.XML_NAMESPACE);
            String data = null;
            if (section.getContent(new ElementFilter()).size() > 1) {
                Element div = new Element("div");
                while (section.getChildren().size() > 0) {
                    div.addContent(section.getChildren().get(0).detach());
                }
                section.addContent(div);
            }
            try {
                data = getContent(section);
            } catch (IOException ioExc) {
                LOGGER.error("while reading section data.", ioExc);
                continue;
            }

            // create json object
            JsonObject jsonObject = new JsonObject();
            if (title != null && !title.equals("")) {
                jsonObject.addProperty(JSON_TITLE, title);
            }
            if (lang != null && !lang.equals("")) {
                jsonObject.addProperty(JSON_LANG, lang);
            }
            String invalidElementName = validateElement(section);
            if (invalidElementName != null) {
                jsonObject.addProperty("hidden", "true");
                jsonObject.addProperty("invalidElement", invalidElementName);
            }
            jsonObject.addProperty(JSON_DATA, data);
            // add to array
            sectionArray.add(jsonObject);
        }
        return sectionArray;
    }

    /**
     * Returns null if an element and all children using HTML or Mycore Tags.
     * 
     * @param element the element to validate
     * @return the invalid element name or null if everything is fine
     */
    private String validateElement(Element element) {
        String elementName = element.getName().toLowerCase(Locale.ROOT);
        if (!(HTML_TAG_LIST.contains(elementName) || mycoreTagList.contains(elementName))) {
            return elementName;
        }
        for (Element el : element.getChildren()) {
            String childElementName = validateElement(el);
            if (childElementName != null) {
                return childElementName;
            }
        }
        return null;
    }

    /**
     * Returns the content of an element as string. The element itself
     * is ignored.
     * 
     * @param e the element to get the content from
     * @return the content as string
     */
    protected String getContent(Element e) throws IOException {
        XMLOutputter out = new XMLOutputter();
        StringWriter writer = new StringWriter();
        for (Content child : e.getContent()) {
            if (child instanceof Element) {
                out.output((Element) child, writer);
            } else if (child instanceof Text) {
                Text t = (Text) child;
                String trimmedText = t.getTextTrim();
                if (!"".equals(trimmedText)) {
                    Text newText = new Text(trimmedText);
                    out.output(newText, writer);
                }
            }
        }
        return writer.toString();
    }

    @Override
    public Element fromJSON(JsonArray jsonSectionArray) {
        // create new document
        MyCoReWebPageProvider wp = new MyCoReWebPageProvider();
        // parse sections
        for (JsonElement sectionElement : jsonSectionArray) {
            if (!sectionElement.isJsonObject()) {
                LOGGER.warn("Invalid json element in content array! {}", sectionElement);
                continue;
            }
            JsonObject sectionObject = sectionElement.getAsJsonObject();
            String title = null;
            String lang = null;
            if (sectionObject.has(JSON_TITLE)) {
                title = sectionObject.get(JSON_TITLE).getAsJsonPrimitive().getAsString();
            }
            if (sectionObject.has(JSON_LANG)) {
                lang = sectionObject.get(JSON_LANG).getAsJsonPrimitive().getAsString();
            }
            String xmlAsString = sectionObject.get(JSON_DATA).getAsJsonPrimitive().getAsString();
            try {
                wp.addSection(title, xmlAsString, lang);
            } catch (IOException | SAXParseException | JDOMException exc) {
                throw new WebApplicationException("unable to add section " + title, exc);
            }
        }
        wp.updateMeta(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID(), null);
        return wp.getXML().detachRootElement();
    }
}
