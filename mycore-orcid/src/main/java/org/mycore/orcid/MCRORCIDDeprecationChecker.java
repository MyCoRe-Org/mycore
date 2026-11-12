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

package org.mycore.orcid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler;

import jakarta.servlet.ServletContext;

/**
 * Logs a warning on startup that the module mycore-orcid is deprecated.
 *
 * @deprecated The module mycore-orcid is deprecated and will be removed.
 * Use mycore-orcid2 instead.
 */
@Deprecated(forRemoval = true)
public class MCRORCIDDeprecationChecker implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getName() {
        return "ORCID Deprecation Checker";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        LOGGER.warn("The module mycore-orcid is deprecated and will be removed in a future release. "
            + "Please migrate to mycore-orcid2.");
    }
}
