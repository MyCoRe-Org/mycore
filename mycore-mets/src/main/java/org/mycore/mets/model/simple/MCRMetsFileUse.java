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

package org.mycore.mets.model.simple;

import java.util.Locale;

import org.mycore.datamodel.niofs.MCRPath;

public enum MCRMetsFileUse {

    ALTO("ALTO"), TRANSLATION("TRANSLATION"), TRANSCRIPTION("TRANSCRIPTION"), MASTER("MASTER"), DEFAULT("DEFAULT");

    MCRMetsFileUse(final String use) {
        this.use = use;
    }

    private String use;

    @Override
    public String toString() {
        return this.use;
    }

    public static MCRMetsFileUse of(final String value) {
        try {
            return MCRMetsFileUse.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    /**
     * Returns the @USE for mets:fileGrp for the given path.
     *
     * @param path the path to the file
     * @return the mets file use
     */
    public static MCRMetsFileUse get(MCRPath path) {
        String href = path.subpathComplete().toString();
        return get(href);
    }

    /**
     * Returns the @USE for mets:fileGrp for the given href.
     *
     * @param href the path to the file
     * @return the mets file use
     */
    public static MCRMetsFileUse get(final String href) {
        if (href.startsWith("tei/" + TRANSLATION.toLowercase())) {
            return MCRMetsFileUse.TRANSLATION;
        } else if (href.startsWith("tei/" + TRANSCRIPTION.toLowercase())) {
            return MCRMetsFileUse.TRANSCRIPTION;
        } else if (href.startsWith("alto/")) {
            return MCRMetsFileUse.ALTO;
        }
        return MCRMetsFileUse.MASTER;
    }

    /**
     * Returns the mets:file/@ID prefix for the given path. The prefix is determined by the folder the file is stored.
     *
     * @param path the path to get the prefix of
     * @return the id prefix
     */
    public static String getIdPrefix(final MCRPath path) {
        String href = path.subpathComplete().toString();
        return getIdPrefix(href);
    }

    /**
     * Returns the mets:file/@ID prefix for the given href. The prefix is determined by the folder the file is stored.
     *
     * @param href the href to get the prefix of
     * @return the id prefix
     */
    public static String getIdPrefix(final String href) {
        MCRMetsFileUse fileUse = get(href);
        if (MCRMetsFileUse.TRANSLATION.equals(fileUse)) {
            String lang = href.replaceAll("tei/" + TRANSLATION.toLowercase() + ".", "").substring(0, 2);
            return TRANSLATION.toLowercase() + "_" + lang;
        }
        return fileUse.toLowercase();
    }

    private String toLowercase() {
        return this.use.toLowerCase(Locale.ROOT);
    }

}
