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

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;

import bibtex.dom.BibtexFile;
import bibtex.parser.BibtexMultipleFieldValuesPolicy;
import bibtex.parser.BibtexParser;
import bibtex.parser.ParseException;

/**
 * Transforms BibTeX source code to JDOM MODS elements
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBibTeX2MODSTransformer extends MCRContentTransformer {

    public MCRJDOMContent transform(MCRContent source) throws IOException {
        String input = source.asString();
        input = fixMissingEntryKeys(input);
        BibtexFile bibtexFile = parse(input);
        Element collection = new MCRBibTeXFileTransformer().transform(bibtexFile);
        return new MCRJDOMContent(collection);
    }

    private Pattern MISSING_KEYS_PATTERN = Pattern.compile("(@[a-zA-Z0-9]+\\s*\\{)(\\s*[a-zA-Z0-9]+\\s*\\=)");

    private String fixMissingEntryKeys(String input) {
        StringBuffer sb = new StringBuffer();
        int i = 0;

        Matcher m = MISSING_KEYS_PATTERN.matcher(input);
        while (m.find()) {
            String entryKey = "key" + (++i);
            m.appendReplacement(sb, m.group(1) + entryKey + ", " + m.group(2));
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private BibtexFile parse(String input) throws UnsupportedEncodingException, IOException {
        BibtexFile bibtexFile = new BibtexFile();
        BibtexParser parser = new BibtexParser(false);
        parser.setMultipleFieldValuesPolicy(BibtexMultipleFieldValuesPolicy.KEEP_ALL);
        try {
            parser.parse(bibtexFile, new StringReader(input));
        } catch (ParseException ex) {
            MCRMessageLogger.logMessage(ex.toString());
        }
        return bibtexFile;
    }

}
