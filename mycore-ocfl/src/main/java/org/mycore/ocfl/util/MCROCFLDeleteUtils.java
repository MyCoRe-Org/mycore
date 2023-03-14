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

package org.mycore.ocfl.util;

// actually increases readability
import static org.mycore.common.config.MCRConfiguration2.getBoolean;
import static org.mycore.common.config.MCRConfiguration2.getString;

import java.util.Optional;

import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Utility Class to decide when to keep or drop the history of elements
 * @author Tobias Lenhardt [Hammer1279]
 */
public final class MCROCFLDeleteUtils {

    public static final String PROPERTY_PREFIX = "MCR.OCFL.dropHistory.";
    private static final String PP = PROPERTY_PREFIX;

    private MCROCFLDeleteUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean checkPurgeObject(MCRObjectID mcrid) {
        return checkPurgeObject(mcrid, MCROCFLObjectIDPrefixHelper.MCROBJECT);
    }

    public static boolean checkPurgeDerivate(MCRObjectID mcrid) {
        return checkPurgeObject(mcrid, MCROCFLObjectIDPrefixHelper.MCRDERIVATE);
    }

    public static boolean checkPurgeObject(MCRObjectID mcrid, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge = getBoolean(PP.substring(0, PP.length() - 1)).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + "preMatch")).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType).orElse(doPurge);
        doPurge = getBoolean(PP + mcrid.getProjectId()).orElse(doPurge);
        doPurge = getBoolean(PP + mcrid.getTypeId()).orElse(doPurge);
        doPurge = getBoolean(PP + mcrid.getBase()).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + ocflType + ".preMatch")).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + mcrid.getProjectId()).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + mcrid.getTypeId()).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + mcrid.getBase()).orElse(doPurge);
        doPurge = getBoolean(PP + mcrid).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + mcrid).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + "postMatch")).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + ocflType + ".postMatch")).orElse(doPurge);
        return doPurge;
    }

    public static boolean checkPurgeClass(MCRCategoryID mcrid) {
        return checkPurgeClass(mcrid, MCROCFLObjectIDPrefixHelper.CLASSIFICATION);
    }

    public static boolean checkPurgeClass(MCRCategoryID mcrid, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge = getBoolean(PP.substring(0, PP.length() - 1)).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + "preMatch")).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + ocflType + ".preMatch")).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType).orElse(doPurge);
        doPurge = getBoolean(PP + mcrid.getRootID()).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + mcrid.getRootID()).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + "postMatch")).orElse(doPurge);
        doPurge = regexMatcher(mcrid.toString(), getString(PP + ocflType + ".postMatch")).orElse(doPurge);
        return doPurge;
    }

    public static boolean checkPurgeUser(String userID) {
        return checkPurgeUser(userID, MCROCFLObjectIDPrefixHelper.USER);
    }

    public static boolean checkPurgeUser(String userID, String prefix) {
        String ocflType = prefix.replace(":", "");

        boolean doPurge = false;
        doPurge = getBoolean(PP.substring(0, PP.length() - 1)).orElse(doPurge);
        doPurge = regexMatcher(userID, getString(PP + "preMatch")).orElse(doPurge);
        doPurge = regexMatcher(userID, getString(PP + ocflType + ".preMatch")).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType).orElse(doPurge);
        doPurge = getBoolean(PP + userID).orElse(doPurge);
        doPurge = getBoolean(PP + ocflType + "." + userID).orElse(doPurge);
        doPurge = regexMatcher(userID, getString(PP + "postMatch")).orElse(doPurge);
        doPurge = regexMatcher(userID, getString(PP + ocflType + ".postMatch")).orElse(doPurge);
        return doPurge;
    }

    /**
     * Provides a Optional boolean if pattern is defined, otherwise returns empty Optional
     * @param toTest String to run pattern against
     * @param pattern pattern to test with or null
     * @return empty Optional if pattern is null, otherwise Optional Boolean of match result
     */
    public static Optional<Boolean> regexMatcher(String toTest, Optional<String> pattern) {
        if (pattern.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toTest.matches(pattern.get()));
    }
}
