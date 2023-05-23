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

package org.mycore.ocfl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;

/**
 * @deprecated use {@link org.mycore.ocfl.classification.MCROCFLClassificationEventHandler
 * MCROCFLClassificationEventHandler}
 */
@Deprecated(forRemoval = true)
public class MCROCFLClassificationEventHandler
    extends org.mycore.ocfl.classification.MCROCFLClassificationEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DEP_WARN
        = "\u001b[31m" + "Usage of the toplevel ocfl classes is deprecated and will be removed in future releases, " +
            "please use 'org.mycore.ocfl.classification.MCROCFLClassificationEventHandler' instead." + "\u001B[0m";

    public MCROCFLClassificationEventHandler() {
        LOGGER.warn(DEP_WARN);
    }

    @Override
    public void doHandleEvent(MCREvent evt) throws MCRException {
        LOGGER.warn(DEP_WARN);
        super.doHandleEvent(evt);
    }

    @Override
    public void undoHandleEvent(MCREvent evt) throws MCRException {
        LOGGER.warn(DEP_WARN);
        super.undoHandleEvent(evt);
    }

}
