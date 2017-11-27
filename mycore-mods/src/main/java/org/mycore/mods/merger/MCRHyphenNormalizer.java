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

package org.mycore.mods.merger;

/**
 * Normalizes the different variants of hyphens in a given input text to a simple "minus" character.
 *
 * @author Frank L\u00FCtzenkirchen
 **/
public class MCRHyphenNormalizer {

    private static final char HYPHEN_NORM = '-';

    private static final char[] HYPHEN_VARIANTS = { '\u002D', '\u2010', '\u2011', '\u2012', '\u2013', '\u2015',
        '\u2212', '\u2E3B', '\uFE58', '\uFE63', };

    /**
     * Normalizes the different variants of hyphens in a given input text to a simple "minus" character.
     **/
    public String normalize(String input) {
        for (char hypenVariant : HYPHEN_VARIANTS) {
            input = input.replace(hypenVariant, HYPHEN_NORM);
        }
        return input;
    }
}
