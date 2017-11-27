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

package org.mycore.iiif.presentation.model.additional;

import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;

public class MCRIIIFAnnotation extends MCRIIIFAnnotationBase {

    public static final String TYPE = "@oa:Annotation";

    private MCRIIIFResource resource;

    public MCRIIIFAnnotation(String id, MCRIIIFCanvas parent) {
        super(id, parent, TYPE);
    }

    public MCRIIIFResource getResource() {
        return resource;
    }

    public void setResource(MCRIIIFResource resource) {
        this.resource = resource;
    }
}
