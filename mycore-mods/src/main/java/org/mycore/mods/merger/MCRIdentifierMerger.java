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

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Locale;

import org.jdom2.Element;

/**
 * Compares and merges mods:identifier elements.
 * Two identifiers are assumed to be the same when they are equals, neglecting any hyphens.
 * At merge, the identifier containing hyphens wins, because it is regarded prettier ;-)
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRIdentifierMerger extends MCRMerger {

    @Override
    public void setElement(Element element) {
        super.setElement(element);
    }

    private String getType() {
        return this.element.getAttributeValue("type", "");
    }

    private String getSimplifiedID() {
        return URLDecoder.decode(this.element.getTextNormalize().toLowerCase(Locale.ENGLISH), StandardCharsets.UTF_8)
            .replace("-", "");
    }

    @Override
    public boolean isProbablySameAs(MCRMerger other) {
        if (!(other instanceof MCRIdentifierMerger)) {
            return false;
        }

        MCRIdentifierMerger oid = (MCRIdentifierMerger) other;
        return this.getType().equals(oid.getType())
            && this.getSimplifiedID().equals(oid.getSimplifiedID());
    }

    @Override
    public void mergeFrom(MCRMerger other) {
        if (!this.element.getText().contains("-") && other.element.getText().contains("-")) {
            this.element.setText(other.element.getText());
        }
    }
}
