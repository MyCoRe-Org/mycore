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
package org.mycore.services.queuedjob;

import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MCRTestJobAction extends MCRJobAction {

    private static Logger LOGGER = LogManager.getLogger(MCRTestJobAction.class);

    /**
     * 
     */
    public MCRTestJobAction() {
    }

    /**
     * @param job
     */
    public MCRTestJobAction(MCRJob job) {
        super(job);
    }

    /* (non-Javadoc)
     * @see org.mycore.services.queuedjob.MCRJobAction#isActivated()
     */
    @Override
    public boolean isActivated() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.queuedjob.MCRJobAction#name()
     */
    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see org.mycore.services.queuedjob.MCRJobAction#execute()
     */
    @Override
    public void execute() throws ExecutionException {
        LOGGER.info("job num: {}", job.getParameter("count"));
        job.setParameter("done", "true");
    }

    /* (non-Javadoc)
     * @see org.mycore.services.queuedjob.MCRJobAction#rollback()
     */
    @Override
    public void rollback() {
        LOGGER.info(job);
        job.setParameter("done", "false");
    }

}
