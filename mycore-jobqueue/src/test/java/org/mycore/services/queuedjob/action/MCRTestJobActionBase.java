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

package org.mycore.services.queuedjob.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

/**
 * @author René Adler (eagle)
 *
 */
public abstract class MCRTestJobActionBase extends MCRJobAction {

    public static final String ERROR_MESSAGE = "Error parameter was set to true";

    private static Logger LOGGER = LogManager.getLogger(MCRTestJobActionBase.class);

    /**
     * @param job
     */
    public MCRTestJobActionBase(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void execute() {
        LOGGER.info("job num: {}", job.getParameter("count"));
        job.setParameter("done", "true");

        if (job.getParameters().containsKey("error") && Boolean.parseBoolean(job.getParameter("error"))) {
            throw new MCRException(ERROR_MESSAGE);
        }
    }

    @Override
    public void rollback() {
        LOGGER.info(job);
        job.setParameter("done", "false");
    }

}
