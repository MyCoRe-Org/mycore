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

import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.xml.sax.SAXException;

import com.google.gson.JsonObject;

/**
 * Converts the source to an MCRObject object and transforms it to JSON using MCRObject.createJSON();
 * 
 * @author Robert Stephan
 */
public class MCRObject2JSONTransformer extends MCRToJSONTransformer {

    @Override
    protected JsonObject toJSON(MCRContent source) throws IOException {
        try {
            MCRObject mcrObj = new MCRObject(source.asXML());
            return mcrObj.createJSON();
        } catch (SAXException | JDOMException e) {
            throw new IOException(
                "Could not generate JSON from " + source.getClass().getSimpleName() + ": " + source.getSystemId(), e);
        }
    }
   
}
