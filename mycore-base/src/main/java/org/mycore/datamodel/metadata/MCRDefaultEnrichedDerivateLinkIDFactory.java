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

package org.mycore.datamodel.metadata;

import java.util.stream.Collectors;

public class MCRDefaultEnrichedDerivateLinkIDFactory extends MCRMetaEnrichedLinkIDFactory {

    @Override
    public MCREditableMetaEnrichedLinkID getDerivateLink(MCRDerivate der) {
        final MCREditableMetaEnrichedLinkID derivateLinkID = getEmptyLinkID();
        final String mainDoc = der.getDerivate().getInternals().getMainDoc();

        derivateLinkID.setReference(der.getId().toString(), null, null);
        derivateLinkID.setSubTag("derobject");

        final int order = der.getOrder();
        derivateLinkID.setOrder(order);

        if (mainDoc != null) {
            derivateLinkID.setMainDoc(mainDoc);
        }

        derivateLinkID.setTitles(der.getDerivate().getTitles());
        derivateLinkID.setClassifications(
            der.getDerivate().getClassifications().stream()
                .map(metaClass -> metaClass.category)
                .collect(Collectors.toList()));

        return derivateLinkID;
    }
}
