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

package org.mycore.datamodel.metadata;

import java.io.IOException;
import java.net.URI;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;


/**
 * This class extends the MCRObject class and contains additional metadata and structure information which is duplicated
 * from other objects or resources.
 * @see MCRObject
 */
public class MCRExpandedObject extends MCRObject {

    public MCRExpandedObject() throws MCRException {
        super(new MCRExpandedObjectStructure(), new MCRObjectMetadata(), new MCRObjectService(), "");
    }

    public MCRExpandedObject(Document objXML) throws MCRException {
        this();
        setFromJDOM(objXML);
    }
    
    public MCRExpandedObject(URI uri) throws IOException, JDOMException {
        this();
        setFromURI(uri);
    }

    public MCRExpandedObject(MCRExpandedObjectStructure structure, MCRObjectMetadata metadata, MCRObjectService service,
                             String label) {
        super(structure, metadata, service, label);
    }

    @Override
    public MCRExpandedObjectStructure getStructure() {
        return (MCRExpandedObjectStructure) super.getStructure();
    }
}
