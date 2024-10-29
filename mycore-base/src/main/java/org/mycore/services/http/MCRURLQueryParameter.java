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

package org.mycore.services.http;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A record representing a query parameter.
 *
 * @see <a href="https://www.w3.org/TR/2014/REC-html5-20141028/forms.html#url-encoded-form-data">W3C recomandation</a>
 * @param name query parameter name, null is allowed but transformed to empty String
 * @param value query parameter name, null is allowed but transformed to empty String
 */
public record MCRURLQueryParameter(String name, String value) {

    public MCRURLQueryParameter {
        name = (name == null ? "" : name);
        value = (value == null ? "" : value);
    }

    /**
     * Creates a new instance from an encoded string
     * <p>
     * If string contains a "=" (U+003D) character, then let name be the substring of string from the start of string
     * up to but excluding its first "=" (U+003D) character, and let value be the substring from the first character,
     * if any, after the first "=" (U+003D) character up to the end of string.
     * If the first "=" (U+003D) character is the first character, then name will be the empty string.
     * If it is the last character, then value will be the empty string.
     * <p>
     * Otherwise, string contains no "=" (U+003D) characters.
     * Let name have the value of string and let value be the empty string.
     *
     * @param encodedString encoded string as stated above
     * @return a record with name and value as determined by above algorithm
     */
    public static MCRURLQueryParameter ofEncodedString(String encodedString) {
        String[] parts = encodedString.split("=");
        switch (parts.length) {
            case 1:
                return new MCRURLQueryParameter(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), "");
            case 2:
                return new MCRURLQueryParameter(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            default:
                throw new IllegalArgumentException("Could not parse parameter: " + encodedString);
        }
    }

    /**
     * Returnes an encoded String as described in {@link #ofEncodedString(String)}.
     * <p>
     * {@snippet :
     * MCRURLQueryParameter inst1 = new MCRURLQueryParameter(name, value);
     * String encodedString = inst1.toEncodedString();
     * MCRURLQueryParameter inst2 = MCRURLQueryParameter.ofEncodedString(encodedString);
     * boolean allwaysTrue = inst1.equals(inst2);
     *}
     * @return encoded String as definied for <code>application/x-www-form-urlencoded</code>
     */
    public String toEncodedString() {
        if (value().isEmpty()) {
            return URLEncoder.encode(this.name(), StandardCharsets.UTF_8);
        }
        return URLEncoder.encode(this.name(), StandardCharsets.UTF_8) + "="
            + URLEncoder.encode(this.value(), StandardCharsets.UTF_8);
    }

    /**
     * Parses a <code>query</code> string as defined for <code>application/x-www-form-urlencoded</code>
     *
     * @param queryString The encoded string, may start with a '?'
     * @return A list of parameters
     */
    public static List<MCRURLQueryParameter> parse(String queryString) {
        String[] parts = removeQuestionMark(queryString).split("&");
        return Stream.of(parts)
            .map(MCRURLQueryParameter::ofEncodedString)
            .collect(Collectors.toUnmodifiableList());
    }

    private static String removeQuestionMark(String queryString) {
        return !queryString.isEmpty() && queryString.charAt(0) == '?' ? queryString.substring(1) : queryString;
    }

    /**
     * Encodes a list of parameters as a query component.
     * The returned query component String is encoded as required but does not start with the question mark ('?').
     *
     * @param parameters list of parameters
     * @return The encoded string, empty if parameters are empty
     * @throws NullPointerException if parameters are empty or contains null values
     */
    public static String toQueryString(List<MCRURLQueryParameter> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        return parameters.stream()
            .map(MCRURLQueryParameter::toEncodedString)
            .collect(Collectors.joining("&"));
    }
}
