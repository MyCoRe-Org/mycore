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

package org.mycore.mods.merger;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

/**
 * Compares and merges mods:name elements
 *
 * @author Frank Lützenkirchen
 */
public class MCRNameMerger extends MCRMerger {

    private String familyName;

    private Set<String> givenNames = new HashSet<>();

    private Set<String> initials = new HashSet<>();

    private Set<String> allNames = new HashSet<>();

    public void setElement(Element element) {
        super.setElement(element);

        setFromNameParts(element);

        if (familyName == null) {
            setFromDisplayForm(element);
        }
    }

    private void setFromDisplayForm(Element element) {
        String displayForm = element.getChildTextTrim("displayForm", MCRConstants.MODS_NAMESPACE);
        if (displayForm != null) {
            setFromCombinedName(displayForm.replaceAll("\\s+", " "));
        }
    }

    private void setFromNameParts(Element modsName) {
        for (Element namePart : modsName.getChildren("namePart", MCRConstants.MODS_NAMESPACE)) {
            String type = namePart.getAttributeValue("type");
            String nameFragment = namePart.getText().replaceAll("\\p{Zs}+", " ");

            if ("family".equals(type)) {
                setFamilyName(nameFragment);
            } else if ("given".equals(type)) {
                addGivenNames(nameFragment);
                addInitials(nameFragment);
                allNames.addAll(initials);
            } else if ("date".equals(type)) {
                continue;
            } else if ("termsOfAddress".equals(type)) {
                continue;
            } else if ("personal".equals(modsName.getAttributeValue("type"))) {
                setFromCombinedName(nameFragment);
            } else {
                setFamilyName(nameFragment);
            }
        }
    }

    private void setFromCombinedName(String nameFragment) {
        if (isEmptyOrNull(nameFragment)) {
            return;
        }
        if (nameFragment.contains(",")) {
            String[] parts = nameFragment.split(",");
            setFamilyNameAndRest(parts[0].trim(), parts[1].trim());
        } else if (nameFragment.contains(" ")) {
            int pos = nameFragment.lastIndexOf(' ');
            setFamilyNameAndRest(nameFragment.substring(pos), nameFragment.substring(0, pos));
        } else {
            setFamilyName(nameFragment);
        }
    }

    private void setFamilyNameAndRest(String familyName, String rest) {
        setFamilyName(familyName);
        addGivenNames(rest);
        addInitials(rest);
        allNames.addAll(initials);
    }

    private void setFamilyName(String nameFragment) {
        if (isEmptyOrNull(nameFragment)) {
            return;
        }
        this.familyName = normalize(nameFragment);
        allNames.add(normalize(nameFragment));
    }

    private void addGivenNames(String nameFragment) {
        if (isEmptyOrNull(nameFragment)) {
            return;
        }
        for (String token : nameFragment.split("\\s")) {
            String normalizedToken = normalize(token);
            if (normalizedToken.length() > 1 && !isEmptyOrNull(normalizedToken)) {
                givenNames.add(normalizedToken);
                allNames.add(normalizedToken);
            }
        }
    }

    private void addInitials(String nameFragment) {
        if (isEmptyOrNull(nameFragment)) {
            return;
        }
        for (String token : nameFragment.split("\\s")) {
            String normalizedToken = normalize(token);
            if (!isEmptyOrNull(normalizedToken)) {
                initials.add(normalizedToken.substring(0, 1));
            }
        }
    }

    private boolean isEmptyOrNull(String nameFragment) {
        return nameFragment == null || normalize(nameFragment).isEmpty();
    }

    private String normalize(String nameFragment) {
        String text = nameFragment.toLowerCase(Locale.getDefault());
        text = MCRHyphenNormalizer.normalizeHyphen(text, ' ');
        // canonical decomposition, then remove accents
        text = Normalizer.normalize(text, Form.NFD).replaceAll("\\p{M}", "");
        text = text.replace("ue", "u").replace("oe", "o").replace("ae", "a").replace("ß", "s").replace("ss", "s");
        text = text.replaceAll("[^a-z0-9]\\s]", ""); //remove all non-alphabetic characters
        text = text.replaceAll("\\p{Punct}", " ").trim(); // remove all punctuation
        text = text.replaceAll("\\s+", " "); // normalize whitespace
        return text.trim();
    }

    @Override
    public boolean isProbablySameAs(MCRMerger e) {
        if (!(e instanceof MCRNameMerger other)) {
            return false;
        }

        if (this.allNames.equals(other.allNames)) {
            return true;
        } else if (!Objects.equals(familyName, other.familyName)) {
            return false;
        } else if (initials.isEmpty() && other.initials.isEmpty()) {
            return true; // same family name, no given name, no initals, then assumed same
        } else if (!haveAtLeastOneCommon(this.initials, other.initials)) {
            return false;
        } else if (this.givenNames.isEmpty() || other.givenNames.isEmpty()) {
            return true;
        } else {
            return haveAtLeastOneCommon(this.givenNames, other.givenNames);
        }
    }

    private boolean haveAtLeastOneCommon(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return !intersection.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(familyName);

        sb.append('|');
        for (String givenName : givenNames) {
            sb.append(givenName).append(',');
        }
        if (!givenNames.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }

        sb.append('|');
        for (String initial : initials) {
            sb.append(initial).append(',');
        }
        if (!initials.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        super.mergeFrom(other);
        // if there is family name after merge, prefer that and remove untyped name part
        if (!getNodes("mods:namePart[@type='family']").isEmpty()) {
            List<Element> namePartsWithoutType = getNodes("mods:namePart[not(@type)]");
            if (!namePartsWithoutType.isEmpty()) {
                namePartsWithoutType.get(0).detach();
            }
        }
    }
}
