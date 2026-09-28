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

import static org.mycore.validation.pdfa.MCRPDFAReportStore.CONFIG_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRDOMContent;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueueManager;
import org.mycore.services.queuedjob.MCRJobStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Keeps the persisted PDF/A validation reports of a derivate in sync with its PDF files.
 * <p>
 * Validation itself is expensive and therefore never performed while a page is rendered. Instead reports are
 * produced by {@link MCRPDFAValidationJobAction} and read back by {@link MCRPDFAReportResolver}. A report is
 * considered up to date as long as it records both the last modified time of the PDF file it describes and the
 * version of the veraPDF library that is currently in use.
 * <p>
 * Reports are only kept for derivates whose parent objects are in an eligible state. Whether an object is eligible
 * is decided by its classification links alone, so that objects created directly in an excluded state are treated
 * like objects that were moved into one.
 */
public final class MCRPDFAReportManager {

    static final String PDF_SUFFIX = ".pdf";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String STATE_CLASSIFICATION_PROPERTY = "MCR.Metadata.Service.State.Classification.ID";

    private static final String DEFAULT_STATE_CLASSIFICATION = "state";

    private static final String ATTR_LAST_MODIFIED = "lastModified";

    private static final String ATTR_VALIDATOR = "validator";

    /** Guards report writes against concurrent cleanup, so that a late job cannot resurrect removed reports. */
    private static final Object REPORT_LOCK = new Object();

    private MCRPDFAReportManager() {
    }

    /**
     * Returns whether reports may be kept for the given derivate, that is whether none of its parent objects is in
     * an excluded state. Only classification links are inspected, no metadata is read.
     * <p>
     * The excluded states are configured as plain category IDs of the state classification, whose root ID is taken
     * from {@code MCR.Metadata.Service.State.Classification.ID}, just like the state of an object itself.
     *
     * @param derivateId the ID of the derivate
     * @return {@code true} if reports may be kept
     */
    public static boolean isEligible(MCRObjectID derivateId) {
        Collection<String> parents = MCRLinkTableManager.getInstance()
            .getSourceOf(derivateId, MCRLinkTableManager.ENTRY_TYPE_DERIVATE);
        MCRCategLinkService linkService = MCRCategLinkServiceFactory.obtainInstance();
        List<MCRCategoryID> excludedStates = getExcludedStates();
        return !parents.isEmpty() && parents.stream()
            .map(MCRObjectID::getInstance)
            .map(MCRCategLinkReference::new)
            .noneMatch(reference -> excludedStates.stream()
                .anyMatch(state -> linkService.isInCategory(reference, state)));
    }

    /**
     * Returns whether the given object is in an excluded state, based on the state it carries itself. Use this for
     * an object that is currently being created or updated, as its classification links are not yet up to date.
     *
     * @param object the object
     * @return {@code true} if the object is in an excluded state
     */
    public static boolean isExcluded(MCRObject object) {
        MCRCategoryID state = object.getService().getState();
        return state != null && getExcludedStates().contains(state);
    }

    /**
     * Returns whether reports are removed when an object enters an excluded state.
     *
     * @return {@code true} if reports are removed
     */
    public static boolean isDeleteOnExcludedState() {
        return MCRConfiguration2.getBoolean(CONFIG_PREFIX + "DeleteOnExcludedState").orElse(true);
    }

    private static List<MCRCategoryID> getExcludedStates() {
        String classificationId = MCRConfiguration2.getString(STATE_CLASSIFICATION_PROPERTY)
            .orElse(DEFAULT_STATE_CLASSIFICATION);
        return MCRConfiguration2.getString(CONFIG_PREFIX + "ExcludedStates").stream()
            .flatMap(MCRConfiguration2::splitValue)
            .map(state -> new MCRCategoryID(classificationId, state))
            .toList();
    }

    /**
     * Returns the paths of all PDF files of the given derivate, relative to its root directory.
     *
     * @param derivateId the ID of the derivate
     * @return the derivate relative paths of all PDF files
     */
    public static List<String> listPDFFiles(MCRObjectID derivateId) {
        MCRPath root = MCRPath.getRootPath(derivateId.toString());
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                .map(root::relativize)
                .map(Path::toString)
                .filter(path -> path.toLowerCase(Locale.ROOT).endsWith(PDF_SUFFIX))
                .sorted()
                .toList();
        } catch (NoSuchFileException e) {
            return List.of();
        } catch (IOException e) {
            throw new MCRException("Could not list PDF files of " + derivateId, e);
        }
    }

    /**
     * Returns a summary of the validation state of all PDF files of the given derivate and schedules validation of
     * every file that has no up to date report. No PDF file is parsed, so this is cheap enough to be called while a
     * page is rendered.
     *
     * @param derivateId the ID of the derivate
     * @return a document with a {@code <derivate>} root element, holding one {@code <file>} element per PDF file
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
     */
    public static Document getReportSummary(MCRObjectID derivateId) throws ParserConfigurationException {
        Document summary = newDocumentBuilderFactory().newDocumentBuilder().newDocument();
        Element root = summary.createElement("derivate");
        root.setAttribute("id", derivateId.toString());
        summary.appendChild(root);
        if (!isEligible(derivateId)) {
            return summary;
        }
        for (String file : listPDFFiles(derivateId)) {
            Optional<Element> report = findCurrentReport(derivateId, file);
            if (report.isPresent()) {
                root.appendChild(summary.importNode(report.get(), true));
            } else {
                Element pending = summary.createElement("file");
                pending.setAttribute("name", file);
                pending.setAttribute("status", "pending");
                root.appendChild(pending);
                scheduleValidation(derivateId, file);
            }
        }
        return summary;
    }

    /**
     * Adds a validation job for the given PDF file, unless an unprocessed job for it already exists.
     *
     * @param derivateId the ID of the derivate
     * @param file       the path of the PDF file, relative to the derivate root
     */
    public static void scheduleValidation(MCRObjectID derivateId, String file) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(MCRPDFAValidationJobAction.DERIVATE_ID_PARAMETER, derivateId.toString());
        parameters.put(MCRPDFAValidationJobAction.PATH_PARAMETER, file);
        MCRJobQueueManager queueManager = MCRJobQueueManager.getInstance();
        if (queueManager.getJobDAO().getJobCount(MCRPDFAValidationJobAction.class, parameters,
            List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR)) > 0) {
            LOGGER.debug("PDF/A validation of {}/{} is already scheduled.", derivateId, file);
            return;
        }
        MCRJob job = new MCRJob(MCRPDFAValidationJobAction.class);
        job.setParameters(parameters);
        if (queueManager.getJobQueue(MCRPDFAValidationJobAction.class).offer(job)) {
            LOGGER.info("Scheduled PDF/A validation of {}/{}.", derivateId, file);
        } else {
            LOGGER.warn("Could not schedule PDF/A validation of {}/{}, the job queue is not running.",
                derivateId, file);
        }
    }

    /**
     * Adds validation jobs for all PDF files of the given derivate that have no up to date report.
     *
     * @param derivateId the ID of the derivate
     */
    public static void scheduleMissingValidations(MCRObjectID derivateId) {
        if (!isEligible(derivateId)) {
            return;
        }
        listPDFFiles(derivateId).stream()
            .filter(file -> findCurrentReport(derivateId, file).isEmpty())
            .forEach(file -> scheduleValidation(derivateId, file));
    }

    /**
     * Validates a single PDF file and stores its report. Eligibility is checked before validation and again before
     * the report is written, so that a report is never stored for an object that meanwhile entered an excluded
     * state. If the PDF file changed while it was validated, the outdated result is discarded.
     *
     * @param derivateId the ID of the derivate
     * @param file       the path of the PDF file, relative to the derivate root
     * @throws IOException if the PDF file cannot be read or the report cannot be written
     */
    public static void validate(MCRObjectID derivateId, String file) throws IOException {
        if (!isEligible(derivateId)) {
            LOGGER.info("Skipping PDF/A validation of {}/{}, object is in an excluded state.", derivateId, file);
            return;
        }
        MCRPath pdf = MCRPath.getPath(derivateId.toString(), file);
        if (!Files.isRegularFile(pdf)) {
            deleteReport(derivateId, file);
            return;
        }
        String lastModified = getLastModified(pdf);
        Document report;
        try {
            report = MCRPDFAFunctions.getResult(pdf, file);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
        if (!lastModified.equals(getLastModified(pdf))) {
            LOGGER.info("Discarding PDF/A validation of {}/{}, the file changed while it was validated.",
                derivateId, file);
            return;
        }
        Element root = report.getDocumentElement();
        root.setAttribute(ATTR_LAST_MODIFIED, lastModified);
        root.setAttribute(ATTR_VALIDATOR, MCRPDFAValidator.getVersion());
        synchronized (REPORT_LOCK) {
            if (!isEligible(derivateId)) {
                LOGGER.info("Dropping PDF/A report of {}/{}, object entered an excluded state.", derivateId, file);
                return;
            }
            MCRPDFAReportStore.obtainInstance(derivateId)
                .writeReport(derivateId, file, new MCRDOMContent(report));
        }
        LOGGER.info("Stored PDF/A report of {}/{}.", derivateId, file);
    }

    /**
     * Returns the stored report of the given PDF file, if it is up to date.
     *
     * @param derivateId the ID of the derivate
     * @param file       the path of the PDF file, relative to the derivate root
     * @return the {@code <file>} element of the report or {@link Optional#empty()} if there is no current report
     */
    public static Optional<Element> findCurrentReport(MCRObjectID derivateId, String file) {
        Path report = MCRPDFAReportStore.obtainInstance(derivateId).getReportFile(derivateId, file);
        if (!Files.isRegularFile(report)) {
            LOGGER.debug("No PDF/A report of {}/{} stored at {}.", () -> derivateId, () -> file, () -> report);
            return Optional.empty();
        }
        try {
            Element root = parse(report).getDocumentElement();
            String storedValidator = root.getAttribute(ATTR_VALIDATOR);
            String validator = MCRPDFAValidator.getVersion();
            if (!Objects.equals(storedValidator, validator)) {
                LOGGER.info("PDF/A report of {}/{} was produced by veraPDF {}, but {} is in use.",
                    derivateId, file, storedValidator, validator);
                return Optional.empty();
            }
            String storedLastModified = root.getAttribute(ATTR_LAST_MODIFIED);
            String lastModified = getLastModified(MCRPath.getPath(derivateId.toString(), file));
            if (!Objects.equals(storedLastModified, lastModified)) {
                LOGGER.info("PDF/A report of {}/{} describes the file as of {}, but it was last modified at {}.",
                    derivateId, file, storedLastModified, lastModified);
                return Optional.empty();
            }
            LOGGER.debug("Using PDF/A report of {}/{}.", derivateId, file);
            return Optional.of(root);
        } catch (IOException e) {
            LOGGER.warn(() -> "Ignoring unreadable PDF/A report " + report, e);
            return Optional.empty();
        }
    }

    /**
     * Deletes the report of a single PDF file.
     *
     * @param derivateId the ID of the derivate
     * @param file       the path of the PDF file, relative to the derivate root
     */
    public static void deleteReport(MCRObjectID derivateId, String file) {
        Path report = MCRPDFAReportStore.obtainInstance(derivateId).getReportFile(derivateId, file);
        synchronized (REPORT_LOCK) {
            try {
                Files.deleteIfExists(report);
            } catch (IOException e) {
                LOGGER.error(() -> "Could not delete PDF/A report " + report, e);
            }
        }
    }

    /**
     * Deletes all reports of the given derivate.
     *
     * @param derivateId the ID of the derivate
     */
    public static void deleteReports(MCRObjectID derivateId) {
        MCRPDFAReportStore store = MCRPDFAReportStore.obtainInstance(derivateId);
        synchronized (REPORT_LOCK) {
            try {
                store.deleteReports(derivateId);
            } catch (IOException e) {
                LOGGER.error(() -> "Could not delete PDF/A reports of " + derivateId, e);
            }
        }
    }

    private static String getLastModified(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toInstant().toString();
    }

    private static Document parse(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return newDocumentBuilderFactory().newDocumentBuilder().parse(input);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
