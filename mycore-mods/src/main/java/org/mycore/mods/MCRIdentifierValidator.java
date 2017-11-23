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

package org.mycore.mods;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIdentifierValidator {

    private static final int ISBN13_LENGTH = 13;

    private static final int ISBN13_WITH_DELIM_LENGTH = 17;

    public static boolean validate(final String type, String value) {
        if (value.trim().length() == 0) {
            //do not check 'required' here
            return true;
        }
        switch (type) {
            case "isbn":
                return checkISBN(value);
            case "doi":
                return checkDOI(value);
            default:
                return true;
        }
    }

    private static boolean checkDOI(String value) {
        return value.startsWith("10.") && value.contains("/");
    }

    private static boolean checkISBN(String value) {
        if (value.length() != ISBN13_WITH_DELIM_LENGTH) {
            //'-' missing
            return false;
        }
        value = value.replaceAll("-", "");
        value = value.replace('x', 'X');
        // ISBN- 13
        if (value.length() == ISBN13_LENGTH) {
            int checkSum = 0;
            int digit = 0;
            for (int i = 0; i < ISBN13_LENGTH; ++i) {
                if (value.charAt(i) == 'X')
                    digit = 10;
                else
                    digit = Character.digit(value.charAt(i), 10);
                if (i % 2 == 1)
                    digit *= 3;
                checkSum += digit;
            }
            if (checkSum % 10 == 0)
                return true;
        }
        return false;
    }
}
