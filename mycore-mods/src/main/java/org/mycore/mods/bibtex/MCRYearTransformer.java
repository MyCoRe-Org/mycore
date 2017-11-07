/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods.bibtex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;

import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexString;

class MCRYearTransformer extends MCRField2XPathTransformer {

    MCRYearTransformer() {
        super("year", "mods:originInfo/mods:dateIssued[@encoding='w3cdtf']");
    }

    void buildField(BibtexAbstractValue value, Element parent) {
        String content = ((BibtexString) value).getContent();
        content = normalizeValue(content);
        String year = getFourDigitYear(content);
        if (year != null) {
            buildElement(xPath, year, parent);
        } else {
            MCRMessageLogger.logMessage("Field year: No 4-digit year found: " + content, parent);
        }
    }

    private final static Pattern YEAR_PATTERN = Pattern.compile(".*(\\d{4}).*");

    private String getFourDigitYear(String text) {
        Matcher m = YEAR_PATTERN.matcher(text);
        return m.matches() ? m.group(1) : null;
    }
}