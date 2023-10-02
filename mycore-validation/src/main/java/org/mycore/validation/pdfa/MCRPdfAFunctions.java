package org.mycore.validation.pdfa;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mycore.common.MCRException;
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
public class MCRPdfAFunctions {

    private static final MCRPdfAValidator PDF_A_VALIDATOR = new MCRPdfAValidator();

    /**
     * Retrieves PDF validation results for files in a given directory and generates an XML document
     * containing the validation outcomes.
     *
     * @param dir      The path to the directory containing the PDF files to validate.
     * @param objectId An identifier for the validation process or target.
     * @return A Document object representing an XML structure containing validation results.
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created.
     * @throws IOException                  If an I/O error occurs while processing the files.
     * @throws MCRException                 If there is an issue with PDF validation or file processing.
     */
    public static Document getResults(Path dir, String objectId) throws ParserConfigurationException, IOException {
        Map<String, ValidationResult> results = new HashMap<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".pdf")) {
                    try {
                        results.put(dir.relativize(file).toString(), PDF_A_VALIDATOR.validate(file));
                    } catch (MCRPdfAValidationException e) {
                        throw new MCRException(e);
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
    private static Document createXML(String objectId, Map<String, ValidationResult> results)
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element derivateElement = createDerivateElement(document, objectId);
        document.appendChild(derivateElement);
        for (Map.Entry<String, ValidationResult> resultEntry : results.entrySet()) {
            createXMLTag(derivateElement, document, resultEntry);
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
                                     Map.Entry<String, ValidationResult> resultEntry) {
        String fileName = resultEntry.getKey();
        ValidationResult result = resultEntry.getValue();
        Element fileElement = createFileElement(document, fileName, result);
        derivateElement.appendChild(fileElement);
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
}
