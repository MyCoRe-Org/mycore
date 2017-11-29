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

package org.mycore.orcid.works;

import java.io.IOException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.orcid.MCRORCIDException;
import org.mycore.orcid.MCRORCIDProfile;
import org.xml.sax.SAXException;

/**
 * Represents a single "work", that means a publication within the "works" section of an ORCID profile,
 * from a single source.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWork {

    private MCRORCIDProfile orcid;

    private String putCode;

    private Element mods;

    private MCRWorkSource source;

    private MCRObjectID objectID;

    MCRWork(MCRORCIDProfile orcid, String putCode) {
        this.orcid = orcid;
        this.putCode = putCode;
    }

    /**
     * Returns the put code, which is the unique identifier of this work within the ORCID profile
     */
    public String getPutCode() {
        return putCode;
    }

    /**
     * Returns the client application that created this work entry.
     */
    public MCRWorkSource getSource() {
        return source;
    }

    void setSource(MCRWorkSource source) {
        this.source = source;
    }

    /**
     * If this work's source is this MyCoRe application,
     * returns the MCRObjectID of the publication the work was created from.
     */
    public MCRObjectID getObjectID() {
        return objectID;
    }

    void setObjectID(MCRObjectID objectID) {
        this.objectID = objectID;
    }

    /**
     * Returns the MODS representation of the work's publication data
     */
    public Element getMODS() {
        return mods;
    }

    void setMODS(Element mods) {
        this.mods = mods;
    }

    /**
     * Fetches the work's details with the complete publication data from the ORCID profile.
     * Initially, only the work summary was fetched.
     */
    void fetchDetails() throws JDOMException, IOException, SAXException {
        orcid.getFetcher().fetchDetails(this);
    }

    /**
     * If this work's source is this MyCoRe application,
     * updates the work in the remote ORCID profile from the local MyCoRe object
     */
    public void update() throws IOException, SAXException, JDOMException {
        if (!source.isThisApplication()) {
            throw new MCRORCIDException("can not update that work, is not from us");
        }
        if (!MCRMetadataManager.exists(objectID)) {
            throw new MCRORCIDException("can not update that work, object " + objectID + " does not exist locally");
        }
        orcid.getPublisher().update(this);
    }

    /** Deletes this work from the remote ORCID profile */
    public void delete() throws IOException, JDOMException, SAXException {
        if (!source.isThisApplication()) {
            throw new MCRORCIDException("can not delete that work, is not from us");
        }
        orcid.getPublisher().delete(this);
    }
}
