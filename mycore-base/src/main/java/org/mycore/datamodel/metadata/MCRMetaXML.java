/*
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(MCRMetaXML.class);

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
            this.content = new ArrayList<Content>();
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
        List<Content> addedContent = new ArrayList<Content>(content.size());
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
                LOGGER.warn("Unable to serialize xml content '" + content + "' to json.");
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
            dest.add((Content) c.clone());
        }
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the subtag is not null or empty
     * <li>the text is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        if (content == null) {
            LOGGER.warn(getSubTag() + ": content is null or empty");
            return false;
        }

        return true;
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
        super.debugDefault();
        LOGGER.debug("Number of contents  = \n" + content.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MCRMetaXML other = (MCRMetaXML) obj;
        return MCRXMLHelper.deepEqual(this.createXML(), other.createXML());
    }

}
