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

package org.mycore.iiif.presentation.model.additional;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;

public class MCRIIIFAnnotationBase extends MCRIIIFPresentationBase {

    // custom serializer needed
    protected String on;

    private String label;

    private String description;

    private String motivation = "sc:painting";

    private transient MCRIIIFCanvas parent;

    public MCRIIIFAnnotationBase(String id, MCRIIIFCanvas parent, String type) {
        super(id, type, API_PRESENTATION_2);
        this.parent = parent;
        refresh();
    }

    public void refresh() {
        this.on = this.parent.getId();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

}
