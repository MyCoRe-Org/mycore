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

package org.mycore.sword.application;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.sword.MCRSwordConstants;
import org.mycore.sword.MCRSwordUtil;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;

public abstract class MCRSwordMetadataProvider implements MCRSwordLifecycle {
    private MCRSwordLifecycleConfiguration lifecycleConfiguration;

    public abstract DepositReceipt provideMetadata(MCRObject object) throws SwordError;

    /**
     * @param id    the id of the MyCoReObject as String
     */
    public Entry provideListMetadata(MCRObjectID id) throws SwordError {
        Entry feedEntry = Abdera.getInstance().newEntry();

        feedEntry.setId(id.toString());
        MCRSwordUtil.BuildLinkUtil.getEditMediaIRIStream(lifecycleConfiguration.getCollection(), id.toString())
            .forEach(feedEntry::addLink);
        feedEntry.addLink(MCRFrontendUtil.getBaseURL() + MCRSwordConstants.SWORD2_EDIT_IRI
            + lifecycleConfiguration.getCollection() + "/" + id, "edit");
        return feedEntry;
    }

    @Override
    public void init(MCRSwordLifecycleConfiguration lifecycleConfiguration) {
        this.lifecycleConfiguration = lifecycleConfiguration;
    }
}
