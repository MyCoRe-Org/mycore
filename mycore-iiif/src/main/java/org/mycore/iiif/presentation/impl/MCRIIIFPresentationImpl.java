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

package org.mycore.iiif.presentation.impl;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

public abstract class MCRIIIFPresentationImpl {

    private static final String MCR_IIIF_PRESENTATION_CONFIG_PREFIX = "MCR.IIIFPresentation.";

    protected static final String DEFAULT_PROPERTY_PREFIX = "MCR.Default.IIIFPresentation.";

    private final String implName;

    public MCRIIIFPresentationImpl(final String implName) {
        this.implName = implName;
    }

    public static MCRIIIFPresentationImpl obtainInstance(String implName) {

        String checkedImplName = (implName == null || implName.isBlank())
            ? MCRConfiguration2.getStringOrThrow("MCR.IIIFPresentation.Default")
            : implName;

        String implPropertyName = MCR_IIIF_PRESENTATION_CONFIG_PREFIX + checkedImplName;
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRIIIFPresentationImpl.class, implPropertyName);

    }

    public String getImplName() {
        return implName;
    }

    /**
     * For consistency and security reasons it may become necessary to
     * to cleanup the identifier, which is an otherwise unchecked URL path parameter.
     * <p>
     * Subclasses may override.
     *
     * @param identifier - the IIIF manifest identifier, which should be normalized
     * @return the normalized identifier
     */
    public String normalizeIdentifier(String identifier) {
        return identifier;
    }

    public abstract MCRIIIFManifest getManifest(String id);

    public static abstract class FactoryBase {

        public String implName;

        @MCRPostConstruction(MCRPostConstruction.Value.TRAILING_NAME)
        public void setImplName(String implName) {
            this.implName = implName;
        }

    }

}
