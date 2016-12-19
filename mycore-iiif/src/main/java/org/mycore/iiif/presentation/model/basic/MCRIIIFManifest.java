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
import java.util.Date;
import java.util.List;

import org.mycore.iiif.presentation.model.MCRIIIFPresentationBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFViewingDirection;

public class MCRIIIFManifest extends MCRIIIFPresentationBase {

    public static final String TYPE = "sc:Manifest";

    public List<MCRIIIFSequence> sequences = new ArrayList<>();

    public List<MCRIIIFMetadata> metadata = new ArrayList<>();

    public List<MCRIIIFRange> structures = new ArrayList<>();

    private String label = null;

    private String description = null;

    private MCRIIIFResource thumbnail = null;

    private MCRIIIFViewingDirection viewingDirection = null;

    private String within = null;

    private Date navDate;

    public MCRIIIFManifest() {
        super(TYPE, API_PRESENTATION_2);
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

    public MCRIIIFResource getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(MCRIIIFResource thumbnail) {
        this.thumbnail = thumbnail;
    }

    public MCRIIIFViewingDirection getViewingDirection() {
        return viewingDirection;
    }

    public void setViewingDirection(MCRIIIFViewingDirection viewingDirection) {
        this.viewingDirection = viewingDirection;
    }

    public String getWithin() {
        return within;
    }

    public void setWithin(String within) {
        this.within = within;
    }

    public Date getNavDate() {
        return navDate;
    }

    public void setNavDate(Date navDate) {
        this.navDate = navDate;
    }

}
