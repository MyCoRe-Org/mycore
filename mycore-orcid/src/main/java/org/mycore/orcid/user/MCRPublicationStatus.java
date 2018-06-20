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

package org.mycore.orcid.user;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.works.MCRWorksSection;
import org.xml.sax.SAXException;

public class MCRPublicationStatus {

    private MCRUserStatus user;

    private String objectID;

    private boolean isUsersPublication = false;

    private boolean isInORCIDProfile = false;

    private String putCode = null;

    MCRPublicationStatus(MCRORCIDUser orcidUser, MCRObjectID oid)
        throws JDOMException, IOException, SAXException {
        this.user = orcidUser.getStatus();

        this.objectID = oid.toString();

        if (user.isORCIDUser()) {
            isUsersPublication = orcidUser.isMyPublication(oid);

            MCRORCIDProfile profile = orcidUser.getProfile();
            MCRWorksSection works = profile.getWorksSection();
            isInORCIDProfile = works.containsWork(oid);

            if (isInORCIDProfile) {
                putCode = works.findWork(oid).get().getPutCode();
            }
        }
    }

    public MCRUserStatus getUserStatus() {
        return user;
    }

    public String getObjectID() {
        return objectID;
    }

    public boolean isUsersPublication() {
        return isUsersPublication;
    }

    public boolean isInORCIDProfile() {
        return isInORCIDProfile;
    }

    public String getPutCode() {
        return putCode;
    }
}
