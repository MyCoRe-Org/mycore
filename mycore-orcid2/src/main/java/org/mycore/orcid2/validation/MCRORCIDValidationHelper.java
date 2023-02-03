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

package org.mycore.orcid2.validation;

import org.mycore.orcid2.user.MCRORCIDCredentials;

/**
 * Provides validation methods.
 */
public class MCRORCIDValidationHelper {

    /**
     * Validates MCRORCIDCredentials
     * 
     * @param credentials MCRORCIDCredentials
     * @return true if credentials are valid
     */
    public static boolean validateCredentials(MCRORCIDCredentials credentials) {
        final String orcid = credentials.getORCID();
        if (orcid == null || !validateORCID(orcid)) {
            return false;
        }
        final String accessToken = credentials.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        return true;
    }


    /**
     * Validates an ORCID iD
     * 
     * @param orcid ORCID iD
     * @return true if ORCID iD is valid
     */
    public static boolean validateORCID(String orcid) {
        if (orcid.length() < 16) {
            return false;
        }
        final char checkDigit = orcid.charAt(orcid.length() - 1);
        final String rawORCID = orcid.substring(0, orcid.length() - 1).replaceAll("\\D+", "");
        if (rawORCID.length() != 15) {
            return false;
        }
        return checkDigit == generateCheckDigit(rawORCID);
    }

    /**
     * Generates check digit as per ISO 7064 11,2.
     * 
     * @param baseDigits base digits
     * @return check digit
     */
    private static char generateCheckDigit(String baseDigits) {
        int total = 0;
        for (int i = 0; i < baseDigits.length(); i++) {
            int digit = Character.getNumericValue(baseDigits.charAt(i));
            total = (total + digit) * 2;
        }
        int remainder = total % 11;
        int result = (12 - remainder) % 11;
        return result == 10 ? 'X' : (char) (result + '0');
    }
}
