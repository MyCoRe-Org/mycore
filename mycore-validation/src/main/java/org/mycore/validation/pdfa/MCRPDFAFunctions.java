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
package org.mycore.validation.pdfa;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.profiles.RuleId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * MCRPdfAFunctions is a utility class that uses MCRPdfAValidator to validate multiple PDF files and generates an XML
 * document with detailed information about the validation results for each file.<br>
 * The class provides a method, 'getResults', which takes a directory path and an object ID as input. It iterates
 * through all PDF files in the specified directory, validates them using MCRPdfAValidator, and records the
 * validation results in an XML format. The resulting XML document has a hierarchical structure as follows:
 * <p>
 *     <ul>
 *
 * <li> &lt;derivate&gt;: Root element representing the object with the given ID.
 * <li> &lt;file&gt;: Element representing each PDF file, containing its name and PDF/A flavor.
 * <li> &lt;failed&gt;: Element representing each failed check in the validation, containing the test number, clause,
 * specification, and a link to the official VeraPDF error documentation (depending on the error).
 *     </ul>
 * <p>
 * The XML document presents detailed information about each PDF file's validation, including the number of failed
 * checks and links to the official VeraPDF error documentation for further reference. The structure allows users to
 * easily analyze and address validation issues in the PDF files.
 *
 * @author Antonia Friedrich
 */
public class MCRPDFAFunctions {

    private static final MCRPDFAValidator PDF_A_VALIDATOR = new MCRPDFAValidator();

    /**
     * Retrieves PDF validation results for files in a given directory and generates an XML document
     * containing the validation outcomes.
     *
     * @param dir      The path to the directory containing the PDF files to validate.
     * @param objectId An identifier for the validation process or target.
     * @return A Document object representing an XML structure containing validation results, including
     * both successful validations and errors encountered.
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
     * @throws IOException                  If an I/O error occurs while processing the files.
     */
    public static Document getResults(Path dir, String objectId) throws ParserConfigurationException, IOException {
        List<PDFAValidationResult> results = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".pdf")) {
                    try {
                        results.add(new PDFAValidationResult(dir.relativize(file).toString(),
                            PDF_A_VALIDATOR.validate(file), null));
                    } catch (MCRPDFAValidationException e) {
                        results.add(new PDFAValidationResult(dir.relativize(file).toString(),
                            null, e));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return createXML(objectId, results);
    }

    /**
     * Creates an XML Document representing the validation results for the PDF files.
     *
     * @param objectId The ID of the object for which the validation results are generated.
     * @param results  A map containing the validation results for each PDF file.
     * @return An XML Document representing the validation results.
     * @throws ParserConfigurationException If there is a configuration issue with the XML document builder.
     */
    private static Document createXML(String objectId, List<PDFAValidationResult> results)
        throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element derivateElement = createDerivateElement(document, objectId);
        document.appendChild(derivateElement);
        if (results != null) {
            for (PDFAValidationResult result : results) {
                createXMLTag(derivateElement, document, result);
            }
        }
        return document;
    }

    /**
     * Creates an XML tag for a PDF file with its validation results.
     *
     * @param derivateElement The parent element to which the file element is added.
     * @param document        The XML document being constructed.
     * @param resultEntry     A map entry containing the file name and its validation result.
     */
    private static void createXMLTag(Element derivateElement, Document document,
        PDFAValidationResult resultEntry) {
        String fileName = resultEntry.name();
        ValidationResult result = resultEntry.result();
        if (result != null) {
            Element fileElement = createFileElement(document, fileName, result);
            derivateElement.appendChild(fileElement);
        } else {
            Element fileElement = document.createElement("file");
            fileElement.setAttribute("name", fileName);
            derivateElement.appendChild(fileElement);
            fileElement.setAttribute("flavour", "Validation Error");
            List<Element> exceptions = createExceptionElements(document, resultEntry.exception());
            Element exceptionElements = document.createElement("exceptions");
            for (Element exception : exceptions) {
                exceptionElements.appendChild(exception);
            }
            fileElement.appendChild(exceptionElements);
        }
    }
    /**
     * Creates a list of XML elements that represent the hierarchy of exceptions and their details.
     * Each exception is represented as an XML element containing information about its class,
     * message, and stack trace. The method processes the main throwable and any nested causes
     * or suppressed exceptions.
     *
     * @param document  The XML {@link Document} to which the exception elements will belong.
     *                  This document is used to create elements representing each exception.
     * @param throwable The root {@link Throwable} to be processed. This throwable and its
     *                  causal chain (including suppressed exceptions) are converted into
     *                  XML elements.
     * @return A list of XML {@link Element} objects, each representing a throwable in the
     *         exception chain. Each element includes the exception class, message, and stack trace.
     */

    private static List<Element> createExceptionElements(Document document, Throwable throwable) {
        Queue<Throwable> exceptionsQueue = new LinkedList<>();
        return buildExceptionTree(document, throwable, exceptionsQueue);
    }

    /**
     * Recursive helper method to build the exception tree.
     */
    private static List<Element> buildExceptionTree(Document document, Throwable currentException,
        Queue<Throwable> exceptionsQueue) {
        List<Element> exceptionElementList = new ArrayList<>();
        if (currentException == null || exceptionsQueue.contains(currentException)) {
            return exceptionElementList;
        }
        exceptionsQueue.add(currentException);
        Element exceptionElement = document.createElement("exception");
        Element classElement = document.createElement("class");
        classElement.setAttribute("name", currentException.getClass().getName());
        exceptionElement.appendChild(classElement);

        if (currentException.getMessage() != null) {
            Element messageElement = document.createElement("message");
            messageElement.setAttribute("message", currentException.getMessage());
            exceptionElement.appendChild(messageElement);
        }

        Element stackTraceElement = document.createElement("stackTrace");
        for (StackTraceElement ste : currentException.getStackTrace()) {
            Element frameElement = document.createElement("frame");
            frameElement.setAttribute("text", ste.toString());
            stackTraceElement.appendChild(frameElement);
        }
        exceptionElement.appendChild(stackTraceElement);

        Throwable[] suppressedExceptions = currentException.getSuppressed();
        if (suppressedExceptions.length > 0) {
            Element suppressedElement = document.createElement("suppressed");
            for (Throwable suppressed : suppressedExceptions) {
                List<Element> suppressedElements = buildExceptionTree(document, suppressed, exceptionsQueue);
                for (Element suppressedChild : suppressedElements) {
                    suppressedElement.appendChild(suppressedChild);
                }
            }
            exceptionElement.appendChild(suppressedElement);
        }

        Throwable cause = currentException.getCause();
        if (cause != null && !cause.equals(currentException)) {
            Element causeElement = document.createElement("cause");
            List<Element> causeElements = buildExceptionTree(document, cause, exceptionsQueue);
            for (Element causeChild : causeElements) {
                causeElement.appendChild(causeChild);
            }
            exceptionElement.appendChild(causeElement);
        }

        exceptionElementList.add(exceptionElement);

        return exceptionElementList;
    }

    /**
     * Creates an XML element representing the "derivate" with an ID attribute.
     *
     * @param document The XML document being constructed.
     * @param objectId The ID of the object for which the validation results are generated.
     * @return The "derivate" XML element.
     */
    private static Element createDerivateElement(Document document, String objectId) {
        Element derivateElement = document.createElement("derivate");
        derivateElement.setAttribute("id", objectId);
        return derivateElement;
    }

    /**
     * Creates an XML element representing a "file" with its attributes and "failed" tags for each failed check.
     *
     * @param document The XML document being constructed.
     * @param fileName The name of the PDF file.
     * @param result   The validation result for the PDF file.
     * @return The "file" XML element.
     */
    private static Element createFileElement(Document document, String fileName, ValidationResult result) {
        Element fileElement = document.createElement("file");
        fileElement.setAttribute("name", fileName);
        fileElement.setAttribute("flavour", result.getPDFAFlavour().toString());

        result.getFailedChecks().keySet().stream()
            .map(rid -> createFailedElement(document, rid))
            .forEach(fileElement::appendChild);

        return fileElement;
    }

    /**
     * Creates an XML element representing a "failed" check with its attributes.
     *
     * @param document The XML document being constructed.
     * @param ruleId   The RuleId containing information about the failed check.
     * @return The "failed" XML element.
     */
    private static Element createFailedElement(Document document, RuleId ruleId) {
        Element failedElement = document.createElement("failed");
        failedElement.setAttribute("testNumber", String.valueOf(ruleId.getTestNumber()));
        failedElement.setAttribute("clause", ruleId.getClause());
        failedElement.setAttribute("specification", ruleId.getSpecification().toString());
        failedElement.setAttribute("Link", getLink(ruleId));
        return failedElement;
    }

    /**
     * Generates a link to an external veraPDF wiki page for a failed check based on the given RuleId.
     * The link is constructed using the specification, clause, and test number of the failed check.
     *
     * @param ruleId The RuleId object representing the failed check.
     * @return The link to the external veraPDF wiki page for the failed check, or an empty string if no link
     * should be returned.
     */
    private static String getLink(RuleId ruleId) {
        PDFAFlavour.Specification specification = ruleId.getSpecification();
        String firstPart = switch (specification) {
            case ISO_14289_1 -> "PDFUA-Part-1-rules";
            case ISO_19005_4 -> "PDFA-Part-4-rules";
            case ISO_19005_2, ISO_19005_3 -> "PDFA-Parts-2-and-3-rules";
            case ISO_19005_1 -> "PDFA-Part-1-rules";
            default -> "";
        };

        if (firstPart.isEmpty()) {
            return "";
        }

        String secondPart = ruleId.getClause().replaceAll("\\.", "");
        return "https://github.com/veraPDF/veraPDF-validation-profiles/wiki/" + firstPart + "#rule-" + secondPart + "-"
            + ruleId.getTestNumber();
    }

    private record PDFAValidationResult(
        String name,
        ValidationResult result,
        Throwable exception) {
    }
}
