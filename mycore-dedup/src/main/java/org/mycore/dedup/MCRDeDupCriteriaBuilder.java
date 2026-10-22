package org.mycore.dedup;

import org.jdom2.Element;

import org.jdom2.filter.Filters;

import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.mods.merger.MCRHyphenNormalizer;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdom2.Document;
import static org.mycore.common.MCRConstants.MODS_NAMESPACE;


/**
 * Builds deduplication criteria from publication metadata fields or its MODS representation
 *
 *
 */
public class MCRDeDupCriteriaBuilder {

    private final Set<String> xPaths;

    public MCRDeDupCriteriaBuilder() {
        this.xPaths = getCriteriaFromProperties();
    }

    public MCRDeDupCriteriaBuilder(Set<String> xPaths) {
        this.xPaths = Set.copyOf(xPaths);
    }

    public MCRDeDupCriterion buildFromIdentifier(String type, String value) {
        return new MCRDeDupCriterion("identifier", type + ":" + value.replaceAll("-", ""));
    }

    public MCRDeDupCriterion buildFromTitleAuthor(String title, String author) {
        return new MCRDeDupCriterion("ta", author + ": " + title);
    }

    public Set<MCRDeDupCriterion> buildFromMyCoreObject(Element element) {
        return xPaths.stream()
                .flatMap(xPath -> {
                    String effectiveXPath = xPath.contains("relatedItem")
                            ? (xPath.startsWith(".") ? xPath : ".//" + xPath)
                            : ".//*[not(ancestor::mods:relatedItem)]/" + (xPath.startsWith(".") ?
                            xPath.substring(1) : xPath);
                    return extractFieldValues(element, effectiveXPath).stream()
                            .map(val -> val.replaceAll("[-/\\.]", ""))
                            .map(val -> new MCRDeDupCriterion(xPath, val));
                })
                .collect(Collectors.toSet());
    }

    private Set<String> extractFieldValues(Element el, String xPath) {
        XPathExpression<Element> expr = XPathFactory.instance()
                .compile(xPath, Filters.element(), null, List.of(MODS_NAMESPACE));
        return expr.evaluate(el).stream()
                .map(Element::getTextTrim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> getCriteriaFromProperties() {
        return Arrays.stream(MCRConfiguration2.getStringOrThrow("MCR.Dedup.Criteria").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

     Set<MCRDeDupCriterion> extractCriteriaFromDocument(Document document) {
        return buildFromMyCoreObject(document.getRootElement());
    }

    public static String normalizeSpecialChars(String inputText) {
        String text = inputText.toLowerCase(Locale.ROOT);
        text = new MCRHyphenNormalizer().normalize(text).replace("-", " ");
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        text = text.replace("ue", "u")
                .replace("oe", "o")
                .replace("ae", "a")
                .replace("ß", "s")
                .replace("ss", "s")
                .replace("-", "")
                .replace("/", "")
                .replace(".", "");
        text = text.replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\p{Punct}", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }
}
