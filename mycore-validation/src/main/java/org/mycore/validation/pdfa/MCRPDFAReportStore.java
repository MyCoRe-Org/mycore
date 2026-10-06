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
package org.mycore.validation.pdfa;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoreManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Stores one PDF/A validation report per PDF file below the slot directory of its derivate.
 * <p>
 * Reports are named after the PDF file they describe, with {@code .xml} appended. The path relative to the derivate
 * root is preserved, so identically named PDF files in different directories do not collide, and appending rather
 * than replacing the extension keeps the mapping injective: {@code chapter.pdf} and {@code chapter.PDF}, which can
 * coexist on a case sensitive store, would both claim {@code chapter.xml}.
 * <p>
 * Stores are separated by {@link MCRObjectID#getBase()} like the IFS2 content stores are, so that numeric derivate
 * IDs of different projects cannot collide either:
 * <p>
 * {@code <BaseDir>/<project>/derivate/<slots>/<derivate ID>/<PDF path>.xml}
 * <p>
 * Instances are created reflectively by {@link MCRStoreManager}, use {@link #obtainInstance(MCRObjectID)}.
 */
public final class MCRPDFAReportStore extends MCRStore {

    static final String CONFIG_PREFIX = "MCR.PDFA.Report.";

    private static final String STORE_ID_PREFIX = "PDFA_";

    private static final String XML_SUFFIX = ".xml";

    /**
     * Returns the report store responsible for the given derivate.
     *
     * @param derivateId the ID of the derivate
     * @return the report store
     */
    public static MCRPDFAReportStore obtainInstance(MCRObjectID derivateId) {
        String base = derivateId.getBase();
        String storeId = STORE_ID_PREFIX + base;
        return MCRStoreManager.computeStoreIfAbsent(storeId, () -> buildStore(storeId, base));
    }

    private static MCRPDFAReportStore buildStore(String storeId, String base) {
        String baseDir = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "BaseDir")
            + File.separatorChar + base.replace("_", File.separator);
        MCRStoreConfig config = new Config(storeId, baseDir, base + "_", getSlotLayout());
        try {
            return MCRStoreManager.buildStore(config, MCRPDFAReportStore.class);
        } catch (ReflectiveOperationException e) {
            throw new MCRConfigurationException("Could not create PDF/A report store with ID " + storeId, e);
        }
    }

    private static String getSlotLayout() {
        return MCRConfiguration2.getString(CONFIG_PREFIX + "SlotLayout").orElseGet(() -> {
            String baseId = "a_a";
            int patternLength = MCRObjectID.formatID(baseId, 1).length() - baseId.length() - "_".length();
            return patternLength - 4 + "-2-2";
        });
    }

    /**
     * Returns the directory holding all reports of the given derivate. The directory need not exist.
     *
     * @param derivateId the ID of the derivate
     * @return the report directory
     */
    public Path getReportDirectory(MCRObjectID derivateId) {
        return getSlot(derivateId.getNumberAsInteger());
    }

    /**
     * Returns the report file describing a single PDF file. The file need not exist.
     *
     * @param derivateId the ID of the derivate
     * @param path       the path of the PDF file, relative to the derivate root
     * @return the report file
     * @throws IllegalArgumentException if the given path does not point into the derivate
     */
    public Path getReportFile(MCRObjectID derivateId, String path) {
        Path directory = getReportDirectory(derivateId);
        Path report = directory.resolve(toReportPath(path)).normalize();
        if (!report.startsWith(directory)) {
            throw new IllegalArgumentException("Not a derivate relative path: " + path);
        }
        return report;
    }

    private static String toReportPath(String path) {
        return (path.startsWith("/") ? path.substring(1) : path) + XML_SUFFIX;
    }

    /**
     * Writes the report of a single PDF file, creating missing directories on the way. An already stored report is
     * replaced atomically where the file system supports it, so that a report is never read half written.
     *
     * @param derivateId the ID of the derivate
     * @param path       the path of the PDF file, relative to the derivate root
     * @param report     the report to write
     * @throws IOException if the report cannot be written
     */
    public void writeReport(MCRObjectID derivateId, String path, MCRContent report) throws IOException {
        Path target = getReportFile(derivateId, path);
        Path directory = target.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, target.getFileName().toString(), ".tmp");
        try {
            report.sendTo(temporary, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Deletes all reports of the given derivate, if any are stored.
     *
     * @param derivateId the ID of the derivate
     * @throws IOException if the reports cannot be deleted
     */
    public void deleteReports(MCRObjectID derivateId) throws IOException {
        int slot = derivateId.getNumberAsInteger();
        if (exists(slot)) {
            delete(slot);
        }
    }

    private record Config(String id, String baseDir, String prefix, String slotLayout) implements MCRStoreConfig {

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getBaseDir() {
            return baseDir;
        }

        @Override
        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getSlotLayout() {
            return slotLayout;
        }
    }
}
