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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSessionMgr;
import org.mycore.tools.MyCoReWebPageProvider;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The default implementation to convert MyCoRe Webpage sections
 * from and to json.
 *
 * @author Matthias Eichner
 */
public class MCRWCMSDefaultSectionProvider implements MCRWCMSSectionProvider {

    private static final Logger LOGGER = LogManager.getLogger(MCRWCMSDefaultSectionProvider.class);

    private static final List<String> HTML_TAG_LIST = Arrays.asList("a", "abbr", "acronym", "address", "applet", "area",
        "article", "aside", "audio", "b", "base", "basefont", "bdi", "bdo", "big", "blockquote", "body", "br", "button",
        "canvas", "caption", "center", "cite", "code", "col", "colgroup", "data", "datalist", "dd", "del", "details",
        "dfn", "dialog", "dir", "div", "dl", "dt", "em", "embed", "fieldset", "figcaption", "figure", "font", "footer",
        "form", "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hr", "html", "i", "iframe",
        "img", "input", "ins", "kbd", "label", "legend", "li", "link", "main", "map", "mark", "meta", "meter", "nav",
        "noframes", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "picture", "pre",
        "progress", "q", "rp", "rt", "ruby", "s", "samp", "script", "section", "select", "small", "source", "span",
        "strike", "strong", "style", "sub", "summary", "sup", "svg", "table", "tbody", "td", "template", "textarea",
        "tfoot", "th", "thead", "time", "title", "tr", "track", "tt", "u", "ul", "var", "video", "wbr");

    public JsonArray toJSON(Element rootElement) {
        JsonArray sectionArray = new JsonArray();
        for (Element section : rootElement.getChildren(MyCoReWebPageProvider.XML_SECTION)) {
            // get infos of element
            String title = section.getAttributeValue(MyCoReWebPageProvider.XML_TITLE);
            String lang = section.getAttributeValue(MyCoReWebPageProvider.XML_LANG, Namespace.XML_NAMESPACE);
            String sectionAsString = null;
            try {
                sectionAsString = getContentAsString(section);
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
            JsonArray unknownHTMLTags = new JsonArray();
            listUnknownHTMLTags(section).forEach(unknownHTMLTags::add);
            jsonObject.add("unknownHTMLTags", unknownHTMLTags);
            jsonObject.addProperty(JSON_DATA, sectionAsString);
            // add to array
            sectionArray.add(jsonObject);
        }
        return sectionArray;
    }

    /**
     * Returns a list of unknown HTML tags.
     *
     * @param element the element to validate
     * @return a list of unknown HTML tags.
     */
    private List<String> listUnknownHTMLTags(Element element) {
        List<String> unknownTagList = new ArrayList<>();
        String elementName = element.getName().toLowerCase(Locale.ROOT);
        if (!HTML_TAG_LIST.contains(elementName)) {
            unknownTagList.add(elementName);
        }
        for (Element el : element.getChildren()) {
            unknownTagList.addAll(listUnknownHTMLTags(el));
        }
        return unknownTagList;
    }

    /**
     * Returns the content of an element as string. The element itself
     * is ignored.
     *
     * @param element the element to get the content from
     * @return the content as string
     */
    protected String getContentAsString(Element element) throws IOException {
        XMLOutputter out = new XMLOutputter();
        StringWriter writer = new StringWriter();
        for (Content child : element.getContent()) {
            if (child instanceof Element) {
                out.output((Element) child, writer);
            } else if (child instanceof Text text) {
                String trimmedText = text.getTextTrim();
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
            } catch (IOException | JDOMException exc) {
                throw new WebApplicationException("unable to add section " + title, exc);
            }
        }
        wp.updateMeta(MCRSessionMgr.getCurrentSession().getUserInformation().getUserID(), null);
        return wp.getXML().detachRootElement();
    }

}
