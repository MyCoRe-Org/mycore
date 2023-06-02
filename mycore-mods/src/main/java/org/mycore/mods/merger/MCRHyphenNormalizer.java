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

import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normalizes the different variants of hyphens in a given input text to the given character; defaults
 * to a simple "minus".
 *
 * @author Frank Lützenkirchen
 **/
public class MCRHyphenNormalizer {

    private static final char HYPHEN_MINUS = '\u002D';

    private static final char SOFT_HYPHEN = '\u00AD';

    private static final char ARMENIAN_HYPHEN = '\u058A';

    private static final char HEBREW_PUNCTUATION_MAQAF = '\u05BE';

    private static final char HYPHEN = '\u2010';

    private static final char NON_BREAKING_HYPHEN = '\u2011';

    private static final char FIGURE_DASH = '\u2012';

    private static final char EN_DASH = '\u2013';

    private static final char EM_DASH = '\u2014';

    private static final char HORIZONTAL_BAR = '\u2015';

    private static final char MINUS_SIGN = '\u2212';

    private static final char TWO_EM_DASH = '\u2E3A';

    private static final char THREE_EM_DASH = '\u2E3B';

    private static final char SMALL_EM_DASH = '\uFE58';

    private static final char SMALL_HYPHEN_MINUS = '\uFE63';

    private static final char FULLWIDTH_HYPHEN_MINUS = '\uFF0D';

    private static final char[] ALL_HYPHEN_VARIANTS = {HYPHEN_MINUS, SOFT_HYPHEN, ARMENIAN_HYPHEN,
            HEBREW_PUNCTUATION_MAQAF, HYPHEN, NON_BREAKING_HYPHEN, FIGURE_DASH, EN_DASH, EM_DASH, HORIZONTAL_BAR,
            MINUS_SIGN, TWO_EM_DASH, THREE_EM_DASH, SMALL_EM_DASH, SMALL_HYPHEN_MINUS, FULLWIDTH_HYPHEN_MINUS};

    private static final String ALL_HYPHEN_VARIANTS_REGEX =
            new String(ALL_HYPHEN_VARIANTS)
                    .chars()
                    .mapToObj(variant -> Pattern.quote(Character.toString((char) variant)))
                    .collect(Collectors.joining("", "[", "]"));

    private static final Pattern ALL_HYPHEN_VARIANTS_PATTERN = Pattern.compile(ALL_HYPHEN_VARIANTS_REGEX);

    /**
     * Normalizes the different variants of hyphens in a given input text to a simple "minus" character.
     **/
    public String normalize(String input) {
        return normalizeHyphen(input);
    }

    /**
     * Normalizes the different variants of hyphens in a given input text to a simple "minus" character.
     **/
    public static String normalizeHyphen(String input) {
        return normalizeHyphen(input, HYPHEN_MINUS);
    }

    /**
     * Normalizes the different variants of hyphens in a given input text to the given character.
     **/
    public static String normalizeHyphen(String input, char replacement) {
        return ALL_HYPHEN_VARIANTS_PATTERN.matcher(input).replaceAll(Character.toString(replacement));
    }

}
