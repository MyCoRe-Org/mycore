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

package org.mycore.services.queuedjob.staticcontent;

import java.util.concurrent.ExecutionException;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.staticcontent.MCRObjectStaticContentGenerator;

public class MCRStaticContentGeneratorJobAction extends MCRJobAction {

    public static final String CONFIG_ID_PARAMETER = "configID";

    public static final String OBJECT_ID_PARAMETER = "objectID";

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     *
     * @param job the job holding the parameters for the action
     */
    public MCRStaticContentGeneratorJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "Static Content - " + getConfigID();
    }

    private String getConfigID() {
        return this.job.getParameters().get(CONFIG_ID_PARAMETER);
    }

    private MCRObjectID getObjectID() {
        String objectIDStr = this.job.getParameters().get(OBJECT_ID_PARAMETER);
        return MCRObjectID.getInstance(objectIDStr);
    }

    @Override
    public void execute() throws ExecutionException {
        try {
            MCRObject object = MCRMetadataManager.retrieveMCRObject(getObjectID());
            MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(getConfigID());
            generator.generate(object);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void rollback() {
        // do nothing
    }
}
