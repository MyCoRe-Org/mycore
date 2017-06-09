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

package org.mycore.mods.merger;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

/**
 * Normalizes text to be fault tolerant when matching for duplicates.
 * Accents, umlauts, case are normalized. Punctuation and non-alphabetic/non-digit characters are removed. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTextNormalizer {

    public static String normalizeText(String text) {
        text = text.toLowerCase(Locale.getDefault());
        text = new MCRHyphenNormalizer().normalize(text).replace("-", " ");
        text = Normalizer.normalize(text, Form.NFD).replaceAll("\\p{M}", ""); //canonical decomposition, remove accents
        text = text.replace("ue", "u").replace("oe", "o").replace("ae", "a").replace("ß", "s").replace("ss", "s");
        text = text.replaceAll("[^a-z0-9]\\s]", ""); //remove all non-alphabetic characters
        // text = text.replaceAll("\\b.{1,3}\\b", " ").trim(); // remove all words with fewer than four characters
        text = text.replaceAll("\\p{Punct}", " ").trim(); // remove all punctuation
        text = text.replaceAll("\\s+", " "); // normalize whitespace
        return text;
    }
}
