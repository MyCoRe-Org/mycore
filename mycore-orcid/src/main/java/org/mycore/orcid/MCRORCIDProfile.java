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

package org.mycore.orcid;

import java.io.IOException;

import javax.ws.rs.client.WebTarget;

import org.jdom2.JDOMException;
import org.mycore.orcid.works.MCRWorksFetcher;
import org.mycore.orcid.works.MCRWorksPublisher;
import org.mycore.orcid.works.MCRWorksSection;
import org.xml.sax.SAXException;

/**
 * Represents the profile of a given ORCID ID.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDProfile {

    private String orcid;

    /** The base target of the REST api for this ORCID profile */
    private WebTarget target;

    private MCRWorksSection worksSection;

    private MCRWorksPublisher publisher = new MCRWorksPublisher(this);

    private MCRWorksFetcher fetcher = new MCRWorksFetcher(this);

    /** The access token required to modify entries in this ORCID profile */
    private String accessToken;

    public MCRORCIDProfile(String orcid) {
        this.orcid = orcid;
        this.target = MCRORCIDClient.instance().getBaseTarget().path(orcid);
    }

    public String getORCID() {
        return orcid;
    }

    public MCRWorksPublisher getPublisher() {
        return publisher;
    }

    public MCRWorksFetcher getFetcher() {
        return fetcher;
    }

    /** Returns the base web target of the REST API for this ORCID profile */
    public WebTarget getWebTarget() {
        return target;
    }

    /** Sets the access token required to modify entries in this ORCID profile */
    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    /** Returns the access token required to modify entries in this ORCID profile */
    public String getAccessToken() {
        return accessToken;
    }

    /** Returns the "works" section of the ORCID profile, which holds the publication data */
    public synchronized MCRWorksSection getWorksSection() throws JDOMException, IOException, SAXException {
        if (worksSection == null) {
            worksSection = new MCRWorksSection(this);
        }
        return worksSection;
    }
}
