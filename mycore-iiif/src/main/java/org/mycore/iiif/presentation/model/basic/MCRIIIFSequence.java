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

import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFViewingDirection;

public class MCRIIIFSequence extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Sequence";

    public List<MCRIIIFCanvas> canvases;

    public List<MCRIIIFMetadata> metadata;

    protected MCRIIIFReference startCanvas;

    private transient MCRIIIFCanvas origStartCanvas;

    private String description;

    private String label;

    private MCRIIIFAnnotationBase thumbnail = null;

    private MCRIIIFViewingDirection viewingDirection = null;

    public MCRIIIFSequence(String id) {
        super(id, TYPE, API_PRESENTATION_2);
    }

    public MCRIIIFCanvas getStartCanvas() {
        return origStartCanvas;
    }

    public void setStartCanvas(MCRIIIFCanvas startCanvas) {
        this.startCanvas = new MCRIIIFReference(this.origStartCanvas = startCanvas);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public MCRIIIFAnnotationBase getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(MCRIIIFAnnotationBase thumbnail) {
        this.thumbnail = thumbnail;
    }

    public MCRIIIFViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(MCRIIIFViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

}
