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

package org.mycore.mods.bibtex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Comment;
import org.jdom2.Element;

/**
 * Helper class to log messages during transformation and add those messages as comment nodes in the resulting MODS XML.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRMessageLogger {

    private static final Logger LOGGER = LogManager.getLogger(MCRMessageLogger.class);

    static void logMessage(String message) {
        LOGGER.warn(message);
    }

    static void logMessage(String message, Element parent) {
        logMessage(message);
        parent.addContent(new Comment(message));
    }
}
