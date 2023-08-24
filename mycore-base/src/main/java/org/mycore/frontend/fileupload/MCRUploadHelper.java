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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRRuleAccessInterface;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;

import jakarta.persistence.EntityTransaction;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Common helper class for all services handling file upload.
 * 
 * @author Frank Lützenkirchen
 * 
 */
public abstract class MCRUploadHelper {

    private static final Logger LOGGER = LogManager.getLogger(MCRUploadHelper.class);

    private static final Pattern PATH_SEPERATOR = Pattern.compile(Pattern.quote(File.separator.replace('\\', '/')));

    public static final String FILE_NAME_PATTERN_PROPERTY = "MCR.FileUpload.FileNamePattern";

    public static final String FILE_NAME_PATTERN = MCRConfiguration2.getStringOrThrow(FILE_NAME_PATTERN_PROPERTY);

    public static final Predicate<String> FILE_NAME_PREDICATE = Pattern.compile(FILE_NAME_PATTERN).asMatchPredicate();

    /**
     * reserved URI characters should not be in uploaded filenames. See RFC3986,
     * Section 2.2 and 7.3
     */
    private static final String RESERVERD_CHARACTERS = new String(
        new char[] { ':', '?', '%', '#', '[', ']', '@', '!', '$', '&', '\'', '(',
                ')', '*', ',', ';', '=', '\'', '+', '\\' });

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
     * @param checkFilePattern
     *            checks if the last path element matches the pattern
     *            defined in the property {@link #FILE_NAME_PATTERN_PROPERTY}
     * @throws MCRException
     *             if path contains reserved character
     */
    public static void checkPathName(String path, boolean checkFilePattern) throws MCRException {
        List<String> pathParts = splitPath(path).collect(Collectors.toList());
        pathParts.forEach(pathElement -> {
            checkNotEmpty(path, pathElement);
            checkOnlyDots(path, pathElement);
            checkTrimmed(pathElement);
            checkEndsWithDot(pathElement);
            checkReservedNames(pathElement);
            checkInvalidCharacters(pathElement);
        });
        String actualFileName = pathParts.get(pathParts.size() - 1);
        if (checkFilePattern && !FILE_NAME_PREDICATE.test(actualFileName)) {
            throw new MCRException(
                "File name does not match " + FILE_NAME_PATTERN + " defined in " + FILE_NAME_PATTERN_PROPERTY + "!");
        }
    }

    /**
     * see {@link #checkPathName(String, boolean)} checkFilePattern=true
     */
    public static void checkPathName(String path) throws MCRException {
        checkPathName(path, true);
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
                c -> c < '\u0020' || WINDOWS_RESERVED_CHARS.indexOf(c) != -1 || RESERVERD_CHARACTERS.indexOf(c) != -1);
    }

    static String getFileName(String path) {
        int pos = Math.max(path.lastIndexOf('\\'), path.lastIndexOf("/"));
        return path.substring(pos + 1);
    }

    static EntityTransaction startTransaction() {
        LOGGER.debug("Starting transaction");
        final EntityTransaction transaction = MCREntityManagerProvider.getCurrentEntityManager().getTransaction();
        transaction.begin();
        return transaction;
    }

    static void commitTransaction(EntityTransaction tx) {
        LOGGER.debug("Committing transaction");
        if (tx != null) {
            tx.commit();
        } else {
            LOGGER.error("Cannot commit transaction. Transaction is null.");
        }
    }

    static void rollbackAnRethrow(EntityTransaction tx, Exception e) throws Exception {
        LOGGER.debug("Rolling back transaction");
        if (tx != null) {
            tx.rollback();
        } else {
            LOGGER.error("Error while rolling back transaction. Transaction is null.");
        }
        throw e;
    }

    public static MCRObjectID getNewCreateDerivateID(MCRObjectID objId) {
        String projectID = objId.getProjectId();
        return MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(projectID + "_derivate");
    }

    public static MCRDerivate createDerivate(MCRObjectID objectID, List<MCRMetaClassification> classifications)
        throws MCRPersistenceException, MCRAccessException {

        MCRObjectID derivateID = getNewCreateDerivateID(objectID);
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(derivateID);
        derivate.getDerivate().getClassifications().addAll(classifications);

        String schema = MCRConfiguration2.getString("MCR.Metadata.Config.derivate")
            .orElse("datamodel-derivate.xml")
            .replaceAll(".xml", ".xsd");
        derivate.setSchema(schema);

        MCRMetaLinkID linkId = new MCRMetaLinkID();
        linkId.setSubTag("linkmeta");
        linkId.setReference(objectID, null, null);
        derivate.getDerivate().setLinkMeta(linkId);

        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);

        MCRMetadataManager.create(derivate);

        setDefaultPermissions(derivateID);

        return derivate;
    }

    public static void setDefaultPermissions(MCRObjectID derivateID) {
        if (MCRConfiguration2.getBoolean("MCR.Access.AddDerivateDefaultRule").orElse(true)) {
            MCRRuleAccessInterface aclImpl = MCRAccessManager.getAccessImpl();
            Collection<String> configuredPermissions = aclImpl.getAccessPermissionsFromConfiguration();
            for (String permission : configuredPermissions) {
                MCRAccessManager.addRule(derivateID, permission, MCRAccessManager.getTrueRule(),
                    "default derivate rule");
            }
        }
    }

    /**
     * Gets the classifications from the given string.
     * @param classifications the string
     * @return the classifications
     */
    public static List<MCRMetaClassification> getClassifications(String classifications) {
        if (classifications == null || classifications.isBlank()) {
            return Collections.emptyList();
        }
        final MCRCategoryDAO dao = MCRCategoryDAOFactory.getInstance();
        final List<MCRCategoryID> categoryIDS = Stream.of(classifications)
            .filter(Objects::nonNull)
            .filter(Predicate.not(String::isBlank))
            .flatMap(c -> Stream.of(c.split(",")))
            .filter(Predicate.not(String::isBlank))
            .filter(cv -> cv.contains(":"))
            .map(classValString -> {
                final String[] split = classValString.split(":");
                return new MCRCategoryID(split[0], split[1]);
            }).collect(Collectors.toList());

        if (!categoryIDS.stream().allMatch(dao::exist)) {
            final String parsedIDS = categoryIDS.stream().map(Object::toString).collect(Collectors.joining(","));
            throw new MCRException(String.format(Locale.ROOT, "One of the Categories \"%s\" was not found", parsedIDS));
        }

        return categoryIDS.stream()
            .map(category -> new MCRMetaClassification("classification", 0, null, category.getRootID(),
                category.getID()))
            .collect(Collectors.toList());
    }

}
