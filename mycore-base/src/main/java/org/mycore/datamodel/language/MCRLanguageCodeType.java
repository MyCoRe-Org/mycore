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

package org.mycore.datamodel.language;

/**
 * Represents a type of langauge code, currently the ISO 639-1 and ISO 639-2b and 2t code types.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public enum MCRLanguageCodeType {

    /** ISO 639-2 terminology code is identical with ISO 639-3 and used in Dublin Core and OAI output */
    termCode,
    /** ISO 639-2 bibliographic code is used in some bibliographic metadata standards */
    biblCode,
    /** ISO 639-1 is used in xml:lang and HTML lang attribute in XML and XHTML output */
    xmlCode
}
