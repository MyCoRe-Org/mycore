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

import org.mycore.common.MCRException;

import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Test job action for testing purposes.
 * @author Sebastian Hofmann
 */
public class MCRTestJobAction extends MCRJobAction {

    /**
     * Creates a new instance of {@link MCRTestJobAction}.
     * @param job the job to execute
     */
    public MCRTestJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "Test job";
    }

    @Override
    public void execute() throws ExecutionException {
        Random random = new Random();

        try {
            Thread.sleep(random.nextInt(10000));
        } catch (InterruptedException e) {
            throw new MCRException(e);
        }
        if(random.nextBoolean()){
            throw new MCRException("Test exception");
        }
    }

    @Override
    public void rollback() {
        // nothing to do
    }
}
