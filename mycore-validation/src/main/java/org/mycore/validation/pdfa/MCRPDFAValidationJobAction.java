/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
package org.mycore.validation.pdfa;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

/**
 * Validates a single PDF file of a derivate and stores its report.
 *
 * @see MCRPDFAReportManager#validate(MCRObjectID, String)
 */
public class MCRPDFAValidationJobAction extends MCRJobAction {

    public static final String DERIVATE_ID_PARAMETER = "derivateID";

    public static final String PATH_PARAMETER = "path";

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     *
     * @param job the job holding the parameters for the action
     */
    public MCRPDFAValidationJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "PDF/A validation - " + job.getParameter(DERIVATE_ID_PARAMETER) + ":"
            + job.getParameter(PATH_PARAMETER);
    }

    @Override
    public void execute() throws ExecutionException {
        MCRObjectID derivateId = MCRObjectID.getInstance(job.getParameter(DERIVATE_ID_PARAMETER));
        try {
            MCRPDFAReportManager.validate(derivateId, job.getParameter(PATH_PARAMETER));
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void rollback() {
        // nothing to roll back, reports are written atomically
    }
}
