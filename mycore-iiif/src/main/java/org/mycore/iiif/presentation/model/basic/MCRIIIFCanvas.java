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

package org.mycore.iiif.presentation.model.basic;

import java.util.ArrayList;
import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;

public class MCRIIIFCanvas extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Canvas";

    public List<MCRIIIFAnnotationBase> images = new ArrayList<>();

    public List<MCRIIIFMetadata> metadata = new ArrayList<>();

    private String label;

    private String description;

    private MCRIIIFResource thumbnail;

    private int height;

    private int width;

    public MCRIIIFCanvas(String id, String label, int width, int height) {
        super(id, TYPE, API_PRESENTATION_2);
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public MCRIIIFResource getThumbnail() {
        return thumbnail;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setThumbnail(MCRIIIFResource thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
