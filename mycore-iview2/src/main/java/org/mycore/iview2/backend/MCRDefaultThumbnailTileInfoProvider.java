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

package org.mycore.iview2.backend;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRDefaultThumbnailTileInfoProvider implements MCRThumbnailTileInfoProvider {

    private static final List<String> DERIVATE_TYPES_THUMBNAILS = MCRConfiguration2
        .getOrThrow("MCR.IIIFImage.Iview.Thumbnail.Derivate.Types", MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

    @Override
    public Optional<MCRTileInfo> getThumbnailFileInfo(String id) {
        if (!MCRObjectID.isValid(id)) {
            return Optional.empty();
        }

        MCRObjectID mcrID = MCRObjectID.getInstance(id);
        MCRDerivate mcrDer = null;
        if (mcrID.getTypeId().equals("derivate")) {
            mcrDer = MCRMetadataManager.retrieveMCRDerivate(mcrID);
            return Optional.of(new MCRTileInfo(mcrDer.getId().toString(),
                mcrDer.getDerivate().getInternals().getMainDoc(), null));
        } else {
            MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrID);
            for (MCRMetaEnrichedLinkID derLink : mcrObj.getStructure().getDerivates()) {
                final Element derLinkXML = derLink.createXML();
                final boolean typeMatching = Optional.ofNullable(derLinkXML.getChild("classification"))
                    .map(c -> c.getAttributeValue("classid") + ":" + c.getAttributeValue("categid"))
                    .filter(DERIVATE_TYPES_THUMBNAILS::contains)
                    .isPresent();

                if (typeMatching) {
                    final String maindoc = derLinkXML.getChildTextTrim("maindoc");
                    if (maindoc != null) {
                        return Optional.of(new MCRTileInfo(derLink.getXLinkHref(), maindoc, null));
                    }
                }
            }

            return Optional.empty();
        }
    }
}
