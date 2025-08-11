/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
package org.mycore.datamodel.classifications2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;
import org.mycore.common.config.MCRConfiguration2;

public final class MCRClassificationMappingUtil {

    private MCRClassificationMappingUtil() {
    }

    /**
     * Replaces a specific pattern in an XPath with the value of a matching property. Placeholders in the
     * property value are substituted with the specific values given in an XPath. It is possible to use
     * multiple patterns per XPath.<p>
     * Syntax:<p>
     * {pattern:&lt;name of property&gt;(&lt;comma-separated list of values&gt;)}<p>
     * (when there are no values, use empty parenthesis)<p>
     * Ex.:<p>
     * <b>Input XPath:</b> {pattern:genre(article)} and not(mods:relatedItem[@type='host'])<p>
     * <b>Property:</b> MCR.Category.XPathMapping.Pattern.genre=mods:genre[substring-after(@valueURI,'#')='{0}']<p>
     * <b>Substituted XPath:</b> mods:genre[substring-after(@valueURI,'#')='article']
     * and not(mods:relatedItem[@type='host'])
     * <br><br>
     * Additionally, it is possible to use OR-operators in the patterns. This creates multiple XPath-expressions
     * after pattern-replacement that are connected via 'or'. To consider all possible combinations of replacement
     * values and logical precedence, parenthesis are put around sub-expressions connected through 'or'.<p>
     * Ex.:<p>
     * <b>Input XPath:</b> {pattern:genre(article|blog_entry)} and not(mods:relatedItem[@type='host'])<p>
     * <b>Property:</b> MCR.Category.XPathMapping.Pattern.genre=mods:genre[substring-after(@valueURI,'#')='{0}']<p>
     * <b>Substituted XPath:</b> (mods:genre[substring-after(@valueURI,'#')='article'] or
     * mods:genre[substring-after(@valueURI,'#')='blog_entry']) and not(mods:relatedItem[@type='host'])
     *
     * @param xPath the XPath containing a pattern to substitute
     * @return the resolved xPath
     */
    public static String replacePattern(String xPath) {
        String updatedXPath = xPath;
        final Pattern pattern = Pattern.compile("\\{pattern:([^(}]*)\\(?([^)]*)\\)?}");
        Matcher matcher = pattern.matcher(updatedXPath);
        while (matcher.find()) {
            String patternName = matcher.group(1);
            String placeholderText = MCRConfiguration2
                .getSubPropertiesMap("MCR.Category.XPathMapping.Pattern.").get(patternName);
            if (placeholderText != null) {
                if (!matcher.group(2).isEmpty()) { // if there are values to substitute
                    String[] placeholderValues = matcher.group(2).split(",");

                    String[][] splitValues = new String[placeholderValues.length][];
                    for (int i = 0; i < placeholderValues.length; i++) {
                        splitValues[i] = placeholderValues[i].split("\\|");
                    }

                    List<Map<String, String>> substitutionMapList = new ArrayList<>();
                    generateCombination(splitValues, 0, new HashMap<>(), substitutionMapList);

                    List<String> substitutes = new ArrayList<>();
                    for (Map<String, String> map : substitutionMapList) {
                        StringSubstitutor sub = new StringSubstitutor(map, "{", "}");
                        String substitute = sub.replace(placeholderText);
                        substitutes.add("(" + substitute + ")");
                    }

                    String updatedSubXPath = "(" + String.join(" or ", substitutes) + ")";
                    updatedXPath = updatedXPath.substring(0, matcher.start()) + updatedSubXPath +
                        updatedXPath.substring(matcher.end());

                } else {
                    updatedXPath = updatedXPath.substring(0, matcher.start()) + placeholderText +
                        updatedXPath.substring(matcher.end());
                }
            } else {
                break; // break while-loop for unconfigured patterns
            }
            matcher = pattern.matcher(updatedXPath);
        }
        return updatedXPath;
    }

    /**
     * Generates all possible combinations of values from the given 2-dimensional array of strings.
     * Each combination is stored in a map where the key is the index of the original array
     * and the value is one of the strings obtained from splitting the original values.
     * <br><br>
     * This method uses recursion to build combinations. It explores each value at the current
     * index and recursively processes the next index, backtracking after exploring each value.
     *
     * @param splitValues A 2-dimensional array of strings where each sub-array contains the values to be combined
     * @param index The current index in the splitValues array being processed, needs to start at 0
     * @param currentMap A map that accumulates the current combination of values being built
     * @param results A list that stores all generated combinations represented as maps
     */
    private static void generateCombination(String[][] splitValues, int index, Map<String, String> currentMap,
        List<Map<String, String>> results) {
        if (index >= splitValues.length) {
            results.add(new HashMap<>(currentMap));
            return;
        }

        for (String value : splitValues[index]) {
            currentMap.put(String.valueOf(index), value);
            generateCombination(splitValues, index + 1, currentMap, results);
            currentMap.remove(String.valueOf(index));
        }
    }

}
