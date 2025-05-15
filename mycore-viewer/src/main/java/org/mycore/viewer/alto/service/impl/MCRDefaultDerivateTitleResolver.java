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

package org.mycore.viewer.alto.service.impl;

import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.viewer.alto.service.MCRDerivateTitleResolver;

public class MCRDefaultDerivateTitleResolver implements MCRDerivateTitleResolver {

    @Override
    public String resolveTitle(String derivateIDString) {
        MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDString);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        return derivate.getDerivate().getTitles().stream()
                .map(MCRMetaLangText::getText)
                .findFirst()
                .orElse(derivateID.toString());
    }
}
