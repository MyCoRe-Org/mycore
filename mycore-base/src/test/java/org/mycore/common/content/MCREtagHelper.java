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

package org.mycore.common.content;

class MCREtagHelper {

    static boolean isValidEtag(String etag) {
        if (etag == null || etag.isEmpty()) {
            return false;
        }
        //support weak etags
        if (etag.startsWith("W/")) {
            etag = etag.substring(2);
        }
        // ETag must be enclosed in double quotes
        if (!(etag.startsWith("\"") && etag.endsWith("\""))) {
            return false;
        }
        // Remove the surrounding quotes for further validation
        String etagValue = etag.substring(1, etag.length() - 1);
        // ETag must not contain control characters or whitespace
        for (char c : etagValue.toCharArray()) {
            if (Character.isISOControl(c) || Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
