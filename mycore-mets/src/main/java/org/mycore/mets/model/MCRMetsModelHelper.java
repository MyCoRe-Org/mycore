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

package org.mycore.mets.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

public class MCRMetsModelHelper {

    public static final String TRANSLATION_USE = "TEI.TRANSLATION";

    public static final String TRANSCRIPTION_USE = "TEI.TRANSCRIPTION";

    public static final String ALTO_USE = "ALTO";

    public static final String MASTER_USE = "MASTER";

    protected static final String ALLOWED_TRANSLATION_PROPERTY = "MCR.METS.Allowed.Translation.Subfolder";

    private static final Set<String> ALLOWED_TRANSLATION = MCRConfiguration2.getString(ALLOWED_TRANSLATION_PROPERTY)
        .map(folders -> folders.split(","))
        .map(Arrays::asList)
        .map(HashSet::new)
        .map(Set.class::cast)
        .orElse(Collections.emptySet());

    private static final Logger LOGGER = LogManager.getLogger();

    public static Optional<String> getUseForHref(final String href) {
        final String hrefWithoutSlash = href.startsWith("/") ? href.substring(1) : href;
        final int lastFolderPosition = hrefWithoutSlash.lastIndexOf("/");
        final String path = (lastFolderPosition == -1) ? "" : hrefWithoutSlash.substring(0, lastFolderPosition);

        if (path.startsWith("tei/")) {
            return handleTEI(hrefWithoutSlash, path);
        } else if (hrefWithoutSlash.startsWith("alto/")) {
            return Optional.of(ALTO_USE);
        }
        return Optional.of(MASTER_USE);
    }

    private static Optional<String> handleTEI(String hrefWithoutSlash, String path) {
        final String[] pathParts = path.split("/");
        if (pathParts.length == 1) {
            LOGGER.warn("Could not detect the file group of " + hrefWithoutSlash);
            return Optional.empty();
        }
        final String teiType = pathParts[1];
        if ("transcription".equals(teiType)) {
            return Optional.of(TRANSCRIPTION_USE);
        } else if (teiType.startsWith("translation.")) {
            final String translation = teiType.split("[.]", 2)[1];
            if (!ALLOWED_TRANSLATION.contains(translation)) {
                LOGGER.warn(
                    "Can not detect file group because " + translation + " is not in list of allowed translations "
                        + ALLOWED_TRANSLATION_PROPERTY);
                return Optional.empty();
            }
            return Optional.of(TRANSLATION_USE + "." + translation.toUpperCase(Locale.ROOT));
        } else {
            return Optional.empty();
        }
    }

}
