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

package org.mycore.frontend.fileupload;

import java.io.File;
import java.nio.CharBuffer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;

/**
 * Common helper class for all services handling file upload.
 * 
 * @author Frank Lützenkirchen
 * 
 * @version $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 */
public abstract class MCRUploadHelper {

    private static final Logger LOGGER = LogManager.getLogger(MCRUploadHelper.class);

    private static final Pattern PATH_SEPERATOR = Pattern.compile(Pattern.quote(File.separator.replace('\\', '/')));

    /**
     * reserved URI characters should not be in uploaded filenames. See RFC3986,
     * Section 2.2
     */
    private static final String reserverdCharacters = new String(
        new char[] { ':', '?', '%', '#', '[', ']', '@', '!', '$', '&', '\'', '(',
            ')', '*', ',', ';', '=', '\'', '+' });

    private static final String WINDOWS_RESERVED_CHARS = "<>:\"|?*";

    private static final String[] RESERVED_NAMES = { "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8",
        "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con", "nul", "prn", "aux" };

    private static final int SAVE_LENGTH = Stream.of(RESERVED_NAMES).mapToInt(String::length).max().getAsInt();

    /**
     * checks if path contains reserved URI characters or path starts or ends with whitespace. There are some characters
     * that are maybe allowed in file names but are reserved in URIs.
     * 
     * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.2">RFC3986, Section 2.2</a>
     * @param path
     *            complete path name
     * @throws MCRException
     *             if path contains reserved character
     */
    public static void checkPathName(String path) throws MCRException {
        if (path.contains("../") || path.contains("..\\")) {
            throw new MCRException("File path " + path + " may not contain \"../\".");
        }
        splitPath(path).forEach(pathElement -> {
            checkNotEmpty(path, pathElement);
            checkOnlyDots(path, pathElement);
            checkTrimmed(pathElement);
            checkEndsWithDot(pathElement);
            checkReservedNames(pathElement);
            checkInvalidCharacters(pathElement);
        });
    }

    private static Stream<String> splitPath(String path) {
        return PATH_SEPERATOR.splitAsStream(path);
    }

    private static void checkNotEmpty(String path, String pathElement) {
        if (pathElement.isEmpty()) {
            throw new MCRException("Path " + path + " contains empty path elements.");
        }
    }

    private static void checkOnlyDots(String path, String pathElement) {
        if (!pathElement.chars().filter(c -> c != '.').findAny().isPresent()) {
            throw new MCRException("Path " + path + " contains invalid path element: " + pathElement);
        }
    }

    private static void checkTrimmed(String pathElement) {
        if (pathElement.trim().length() != pathElement.length()) {
            throw new MCRException(
                "Path element '" + pathElement + "' may not start or end with whitespace character.");
        }
    }

    private static void checkEndsWithDot(String pathElement) {
        if (pathElement.charAt(pathElement.length() - 1) == '.') {
            throw new MCRException("Path element " + pathElement + " may not end with '.'");
        }
    }

    private static void checkReservedNames(String pathElement) {
        if (pathElement.length() <= SAVE_LENGTH) {
            String lcPathElement = pathElement.toLowerCase(Locale.ROOT);
            if (Stream.of(RESERVED_NAMES).anyMatch(lcPathElement::equals)) {
                throw new MCRException("Path element " + pathElement + " is an illegal Windows file name.");
            }
        }
    }

    private static void checkInvalidCharacters(String pathElement) {
        if (getOSIllegalCharacterStream(pathElement).findAny().isPresent()) {
            throw new MCRException("Path element " + pathElement + " contains illegal characters: "
                + getOSIllegalCharacterStream(pathElement)
                    .mapToObj(Character::toChars)
                    .map(CharBuffer::wrap)
                    .collect(Collectors.joining("', '", "'", "'")));
        }
    }

    private static IntStream getOSIllegalCharacterStream(String path) {
        //https://msdn.microsoft.com/en-us/library/aa365247.aspx
        return path
            .chars()
            .filter(
                c -> c < '\u0020' || WINDOWS_RESERVED_CHARS.indexOf(c) != -1 || reserverdCharacters.indexOf(c) != -1);
    }

    static String getFileName(String path) {
        int pos = Math.max(path.lastIndexOf('\\'), path.lastIndexOf("/"));
        return path.substring(pos + 1);
    }

    static Transaction startTransaction() {
        LOGGER.debug("Starting transaction");
        return MCRHIBConnection.instance().getSession().beginTransaction();
    }

    static void commitTransaction(Transaction tx) {
        LOGGER.debug("Committing transaction");
        if (tx != null) {
            tx.commit();
        } else {
            LOGGER.error("Cannot commit transaction. Transaction is null.");
        }
    }

    static void rollbackAnRethrow(Transaction tx, Exception e) throws Exception {
        LOGGER.debug("Rolling back transaction");
        if (tx != null) {
            tx.rollback();
        } else {
            LOGGER.error("Error while rolling back transaction. Transaction is null.");
        }
        throw e;
    }

}
