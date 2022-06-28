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

package org.mycore.common.content.transformer;

import java.io.IOException;
import java.io.OutputStream;

import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

/**
 * Uses {@link MCRBase#createJSON()} to transform the source (must be XML) to JSON.
 * The MyCoRe ID is retrieved from the ID attribute of the root element of the source.
 * 
 * @author Robert Stephan
 */
public class MCRBase2JSONTransformer extends MCRContentTransformer {

    
    /* (non-Javadoc)
     * @see org.mycore.common.content.transformer.MCRContentTransformer#transform(org.mycore.common.content.MCRContent)
     */
    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        JsonObject jsonObject = toJSON(source);
        MCRStringContent result = new MCRStringContent(jsonObject.toString());
        result.setMimeType(mimeType);
        result.setEncoding(getEncoding());
        result.setUsingSession(source.isUsingSession());
        return result;
    }

    private static JsonObject toJSON(MCRContent source) throws IOException {
        try {
            String idAttr = source.asXML().getRootElement().getAttributeValue("ID");
            MCRObjectID mcrId = MCRObjectID.getInstance(idAttr);
            MCRBase mcrBase = MCRMetadataManager.retrieve(mcrId);
            return mcrBase.createJSON();
        } catch (JDOMException | SAXException | NullPointerException e) {
            throw new MCRException(
                "Could not generate JSON from " + source.getClass().getSimpleName() + ": " + source.getSystemId(), e);
        }
    }

    @Override
    public void transform(MCRContent source, OutputStream out) throws IOException {
        JsonObject jsonObject = toJSON(source);
        out.write(jsonObject.toString().getBytes(getEncoding()));
    }

    @Override
    public String getDefaultMimeType() {
        //default by RFC 4627
        return "application/json";
    }

    @Override
    public String getEncoding() {
        //default by RFC 4627
        return "UTF-8";
    }

    @Override
    protected String getDefaultExtension() {
        return "json";
    }

}
