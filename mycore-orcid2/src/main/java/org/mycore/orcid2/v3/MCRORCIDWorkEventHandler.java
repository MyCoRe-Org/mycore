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

package org.mycore.orcid2.v3;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRIdentifier;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Work handler which publishes object to ORCID.
 */
public class MCRORCIDWorkEventHandler extends org.mycore.orcid2.MCRORCIDWorkEventHandler<Work> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected Work transformContent(MCRContent content) throws MCRORCIDTransformationException {
        return MCRORCIDWorkTransformerHelper.transformContent(content);
    }

    @Override
    protected void publishWork(MCRObject object, Work work, Set<MCRIdentifier> identifiers,
        List<MCRORCIDCredentials> credentials) {
        credentials.forEach(c -> {
            final String orcid = c.getORCID();
            try {
                MCRORCIDUserInfo userInfo = MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid);
                if (userInfo == null) {
                    userInfo = new MCRORCIDUserInfo(orcid);
                }
                MCRORCIDWorkHelper.retrieveWorkInfo(object, c, userInfo);
                final long putCode = MCRORCIDWorkHelper.publishWork(work, c, userInfo.getWorkInfo());
                final long ownPutCode = userInfo.getWorkInfo().getOwnPutCode();
                if (!Objects.equals(ownPutCode, putCode)) {
                    userInfo.getWorkInfo().setOwnPutCode(putCode);
                    MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
                }
            } catch (Exception ex) {
                LOGGER.warn("Could not publish {} to ORCID profile: {}.", object.getId(), c.getORCID(), ex);
            }
        });
    }
}
