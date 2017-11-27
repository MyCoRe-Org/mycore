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

package org.mycore.datamodel.ifs;

import org.jdom2.Element;

/**
 * Detects the file content type from filename and file header. The rules to do
 * this are implementation specific, but MyCoRe provides a simple detector
 * implementation in the class MCRSimpleFCTDetector.
 * 
 * @see MCRSimpleFCTDetector
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public interface MCRFileContentTypeDetector {
    /**
     * Adds a detection rule from the file content type definition XML file. The
     * detector is responsible for parsing the &lt;rules&gt; element provided
     * and registering the rules stored there with the content type given.
     * 
     * @param type
     *            the file content type the rule is for
     * @param rules
     *            the rules XML element containing the rules for detecting that
     *            type
     */
    void addRule(MCRFileContentType type, Element rules);

    /**
     * Detects the file content type from filename and/or file header.
     * 
     * @param filename
     *            the name of the file
     * @param header
     *            the first bytes of the file content
     * @return the file content type detected, or null if detection was not
     *         possible
     */
    MCRFileContentType detectType(String filename, byte[] header);
}
