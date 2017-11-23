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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaLangText class present a single item, which
 * has triples of a text and his corresponding language and optional a type.
 * 
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * @author Johannes B\u00fchler
 * @version $Revision$ $Date$
 */
public class MCRMetaXML extends MCRMetaDefault {
    List<Content> content;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * Set the java.util.ArrayList of child elements to new.
     */
    public MCRMetaXML() {
        super();
    }

    public MCRMetaXML(String set_subtag, String set_type, int set_inherited) throws MCRException {
        super(set_subtag, null, set_type, set_inherited);
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        content = element.cloneContent();
    }

    public void addContent(Content content) {
        if (this.content == null) {
            this.content = new ArrayList<>();
        }

        this.content.add(content);
    }

    public List<Content> getContent() {
        return content;
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaLangText definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaLangText part
     */
    @Override
    public org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        List<Content> addedContent = new ArrayList<>(content.size());
        cloneListContent(addedContent, content);
        elm.addContent(addedContent);

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     content: [
     *       ... json objects of parsed content ...
     *     ]
     *   }
     * </pre>
     * 
     * @see MCRXMLHelper#jsonSerialize(Element)
     */
    @Override
    public JsonObject createJSON() {
        JsonObject json = super.createJSON();
        JsonArray jsonContentArray = new JsonArray();
        getContent().forEach(content -> {
            JsonElement jsonContent = MCRXMLHelper.jsonSerialize(content);
            if (jsonContent == null) {
                LOGGER.warn("Unable to serialize xml content '{}' to json.", content);
                return;
            }
            jsonContentArray.add(jsonContent);
        });
        json.add("content", jsonContentArray);
        return json;
    }

    private static void cloneListContent(List<Content> dest, List<Content> source) {
        dest.clear();
        for (Content c : source) {
            dest.add(c.clone());
        }
    }

    /**
     * Validates this MCRMetaXML. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the content is null</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaXML is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (content == null) {
            throw new MCRException(getSubTag() + ": content is null or empty");
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public MCRMetaXML clone() {
        MCRMetaXML out = new MCRMetaXML();
        out.setFromDOM(createXML());
        return out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Number of contents  = \n{}", content.size());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaXML other = (MCRMetaXML) obj;
        return MCRXMLHelper.deepEqual(this.createXML(), other.createXML());
    }

}
