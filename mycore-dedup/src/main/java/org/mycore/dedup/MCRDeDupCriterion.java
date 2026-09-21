package org.mycore.dedup;

import org.apache.commons.codec.digest.DigestUtils;
import org.jdom2.Element;

import static org.mycore.dedup.MCRDeDupCriteriaBuilder.normalizeSpecialChars;
import static org.mycore.mods.merger.MCRTextNormalizer.normalizeText;


/**
 * Represents a criterion to decide equality of publications.
 * When two publications result in equal MCRDeDupCriterion, they may be duplicates.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDeDupCriterion {
    /** Indicates the type of criterion, e.g. title, identifier, shelfmark */
    private String type;

    /** The value of the criterion, e.g. a normalized title or identifier */
    private String value;

    /** A hash built over type and value. Two criteria are equal if their hash values are equal. */
    private String key;

    private boolean usedInMatch;

    /**
     * Builds a new deduplication criterion.
     *
     * @param type indicates the type of criterion, e.g. title, identifier, shelfmark
     * @param value the value of the criterion, e.g. a normalized title or identifier
     */
    public MCRDeDupCriterion(String type, String value) {
            this.type = normalizeText(normalizeSpecialChars(type));
            this.value = normalizeText(normalizeSpecialChars(value));
            this.key = DigestUtils.md2Hex(this.type + ":" + this.value);
    }

    /**
     * Returns the type of criterion, e.g. title, identifier, shelfmark
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the value of the criterion, e.g. a normalized title or identifier
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a hash key built over type and value of this criterion.
     * Two criteria are equal if their hash values are equal
     */
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MCRDeDupCriterion ? this.key.equals(((MCRDeDupCriterion) other).key) : false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key + " => " + type + ":\"" + value + "\"";
    }

    /**
     * Returns an XML representation of this criterion, used in deduplication reports
     */
    public Element toXML() {
        Element element = new Element("dedup");
        element.setText(value);
        element.setAttribute("key", key);
        element.setAttribute("type", type);
        return element;
    }


    public boolean isUsedInMatch() {
        return usedInMatch;
    }


    public void markAsUsedInMatch() {
        this.usedInMatch = true;
    }
}
