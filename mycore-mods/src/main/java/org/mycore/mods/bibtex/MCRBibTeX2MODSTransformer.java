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

import java.io.IOException;
import java.io.StringReader;
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
 * Transforms BibTeX source code to JDOM MODS elements.
 * Output is a mods:modsCollection.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBibTeX2MODSTransformer extends MCRContentTransformer {

    private static final Pattern MISSING_KEYS_PATTERN = Pattern
        .compile("(@[a-zA-Z0-9]+\\s*\\{)(\\s*[a-zA-Z0-9]+\\s*\\=)");

    @Override
    public MCRJDOMContent transform(MCRContent source) throws IOException {
        String input = source.asString();
        input = fixMissingEntryKeys(input);
        BibtexFile bibtexFile = parse(input);
        Element collection = new MCRBibTeXFileTransformer().transform(bibtexFile);
        return new MCRJDOMContent(collection);
    }

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

    private BibtexFile parse(String input) throws IOException {
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
