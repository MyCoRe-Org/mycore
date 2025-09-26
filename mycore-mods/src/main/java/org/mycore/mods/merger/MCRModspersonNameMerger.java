package org.mycore.mods.merger;

import java.util.Objects;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

/**
 * Compares and merges mods:name elements. Extends MCRNameMerger and provides
 * additional functionality:
 * <ol>
 *     <li>It can handle mods:alternativeName and considers them when determining
 *     if two names are probably the same.</li>
 *     <li>If first name is the same and one of the family names contains the other,
 *     the names are considered as "probablySameAs".
 *     This takes double-barreled names (Doppelnamen) into account.</li>
 * </ol>
 */
public class MCRModspersonNameMerger extends MCRNameMerger {

    private static final String ALTERNATIVE_NAME = "alternativeName";

    private static final String TYPE = "type";

    private static final String NAME_PART = "namePart";

    @Override
    public boolean isProbablySameAs(MCRMerger e) {
        if (!(e instanceof MCRNameMerger other)) {
            return false;
        }

        if (haveContradictingNameIds(this.nameIds, other.nameIds)) {
            return false;
        } else if (this.allNames.equals(other.allNames)) {
            return true;
        } else if (Objects.equals(familyName, other.familyName)) {
            if (initials.isEmpty() && other.initials.isEmpty()) {
                return true; // same family name, no given name, no initals, then assumed same
            } else if (!haveAtLeastOneCommon(this.initials, other.initials)) {
                return false;
            } else if (this.givenNames.isEmpty() || other.givenNames.isEmpty()) {
                return true;
            } else {
                return haveAtLeastOneCommon(this.givenNames, other.givenNames);
            }
        } else {
            // double-barreled name with same given names assumes same
            return this.givenNames.equals(other.givenNames) &&
                (this.familyName.contains(other.familyName) || other.familyName.contains(this.familyName));
        }
    }

    /**
     * Checks if this merger has an alternativeName-element that is
     * {@link MCRNameMerger#isProbablySameAs(MCRMerger) probably the same as} the other given merger.
     * @param other the other merger
     * @return returns true if the other merger is also a {@link MCRNameMerger} and if this merger has
     * an alternative name that matches the other
     */
    public boolean hasAlternativeNameSameAs(MCRMerger other) {
        if (!(other instanceof MCRNameMerger)) {
            return false;
        }
        return this.element.getChildren(ALTERNATIVE_NAME, MCRConstants.MODS_NAMESPACE)
            .stream()
            .map(MCRMergerFactory::buildFrom)
            .anyMatch(altMerger -> altMerger.isProbablySameAs(other));
    }

    /**
     * Merges the contents of the element wrapped by the other merger into a new alternativeName element
     * in the element wrapped by this merger. Should only be called if this.isProbablySameAs(other).
     * The alternative name is only added if the two names are not exactly the same and if the
     * alternative name doesn't yet exist in the element wrapped by this merger.
     * Only the family name and given names are merged into the alternativeName element.
     * @param e the other merger
     */
    public void mergeAsAlternativeName(MCRMerger e) {
        if (!(e instanceof MCRNameMerger other)) {
            return;
        }
        if (this.allNames.equals(other.allNames)) {
            return;
        }
        if (this.hasAlternativeNameSameAs(e)) {
            return;
        }
        Element alternativeName = new Element(ALTERNATIVE_NAME, MCRConstants.MODS_NAMESPACE);

        other.element.getChildren(NAME_PART, MCRConstants.MODS_NAMESPACE)
            .stream()
            .filter(namePart -> "given".equals(namePart.getAttributeValue(TYPE)))
            .forEach(namePart -> {
                Element altGivenName = new Element(NAME_PART, MCRConstants.MODS_NAMESPACE)
                    .setAttribute(TYPE, "given");
                altGivenName.addContent(namePart.getText());
                alternativeName.addContent(altGivenName);
            });

        Element altFamilyName = new Element(NAME_PART, MCRConstants.MODS_NAMESPACE)
            .setAttribute(TYPE, "family");
        Element familyName = other.element.getChildren(NAME_PART, MCRConstants.MODS_NAMESPACE).stream()
            .filter(namePart -> "family".equals(namePart.getAttributeValue(TYPE)))
            .findFirst()
            .orElse(null);
        altFamilyName.addContent(familyName != null ? familyName.getText() : null);
        alternativeName.addContent(altFamilyName);

        this.element.addContent(alternativeName);

    }
}
