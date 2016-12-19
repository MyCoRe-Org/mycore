/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
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

    private String description = null;

    private MCRIIIFResource thumbnail = null;

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
}
