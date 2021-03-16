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
 *
 *
 */

package org.mycore.mcr.cronjob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.processing.MCRProcessableStatus;

public class MCRTestCronJob extends MCRCronjob {

    public static final Logger LOGGER = LogManager.getLogger();

    public static int count = 0;
    private Integer n;

    @Override
    public void runJob() {
        getProcessable().setStatus(MCRProcessableStatus.processing);
        getProcessable().setProgress(0);

        for (int i = 0; i < getN(); i++) {
            Double progress = ((double) i / getN()) * 100;
            getProcessable().setProgress(progress.intValue());
            count++;
            LOGGER.info("Process: " + i);
        }
    }

    public Integer getN() {
        return n;
    }

    @MCRProperty(name = "N")
    public void setN(String n) {
        Integer n1 = Integer.valueOf(n);
        if (n1 < 0) {
            throw new IllegalArgumentException("N should be >0");
        }
        this.n = n1;
    }

    @Override
    public String getDescription() {
        return "Counts to " + getN();
    }
}
