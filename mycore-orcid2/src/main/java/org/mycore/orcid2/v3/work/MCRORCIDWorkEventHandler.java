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

package org.mycore.orcid2.v3.work;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Work handler which publishes object to ORCID.
 */
public class MCRORCIDWorkEventHandler extends org.mycore.orcid2.MCRORCIDWorkEventHandler<Work> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void removeWork(MCRORCIDPutCodeInfo workInfo, String orcid, MCRORCIDCredential credential) {
        MCRORCIDWorkService.doDeleteWork(workInfo, orcid, credential);
    }

    @Override
    protected void createWork(Work work, MCRORCIDPutCodeInfo workInfo, String orcid, MCRORCIDCredential credential) {
        MCRORCIDWorkService.doCreateWork(work, workInfo, orcid, credential);
    }

    @Override
    protected void updateWork(long putCode, Work work, String orcid, MCRORCIDCredential credential) {
        MCRORCIDWorkService.doUpdateWork(putCode, work, orcid, credential);
    }

    @Override
    protected void updateWorkInfo(Work work, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential) {
        MCRORCIDWorkService.doUpdateWorkInfo(work, workInfo, orcid, credential);
    }

    @Override
    protected Work transformWork(MCRObject object) {
        return MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(object.createXML()));
    }
}
