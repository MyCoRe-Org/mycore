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

package org.mycore.iiif.presentation.model;

import java.util.List;

import org.mycore.iiif.model.MCRIIIFBase;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFLDURI;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFResource;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFService;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFViewingHint;

public class MCRIIIFPresentationBase extends MCRIIIFBase {
    public List<MCRIIIFLDURI> seeAlso = null;

    private String attribution = null;

    private MCRIIIFResource logo = null;

    private MCRIIIFLDURI licence = null;

    private MCRIIIFViewingHint viewingHint = null;

    private MCRIIIFResource related = null;

    private MCRIIIFService service = null;

    public MCRIIIFPresentationBase(String id, String type, String context) {
        super(id, type, context);
    }

    public MCRIIIFPresentationBase(String type, String context) {
        super(type, context);
    }

    public MCRIIIFPresentationBase(String context) {
        super(context);
    }

    public MCRIIIFPresentationBase() {
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public MCRIIIFResource getLogo() {
        return logo;
    }

    public void setLogo(MCRIIIFResource logo) {
        this.logo = logo;
    }

    public MCRIIIFLDURI getLicence() {
        return licence;
    }

    public void setLicence(MCRIIIFLDURI licence) {
        this.licence = licence;
    }

    public MCRIIIFViewingHint getViewingHint() {
        return viewingHint;
    }

    public void setViewingHint(MCRIIIFViewingHint viewingHint) {
        this.viewingHint = viewingHint;
    }

    public MCRIIIFResource getRelated() {
        return related;
    }

    public void setRelated(MCRIIIFResource related) {
        this.related = related;
    }

    public MCRIIIFService getService() {
        return service;
    }

    public void setService(MCRIIIFService service) {
        this.service = service;
    }

}
