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

package org.mycore.mcr.cronjob;

import org.mycore.common.MCRException;
import org.mycore.common.processing.MCRProcessableStatus;

public class MCRTestErrorCronJob extends MCRCronjob {

    public static int count;

    @Override
    public void runJob() {
        getProcessable().setStatus(MCRProcessableStatus.PROCESSING);
        getProcessable().setProgress(0);
        count++;
        throw new MCRException("test error");
    }

    @Override
    public String getDescription() {
        return "Will always throw an exception, for testing only";
    }
}
