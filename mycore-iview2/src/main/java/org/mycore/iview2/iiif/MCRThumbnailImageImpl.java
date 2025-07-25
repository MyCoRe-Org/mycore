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

package org.mycore.iview2.iiif;

import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;
import org.mycore.iview2.backend.MCRTileInfo;

public class MCRThumbnailImageImpl extends MCRIVIEWIIIFImageImpl {

    protected static final String DERIVATE_TYPES = "Derivate.Types";

    private List<String> derivateTypes;

    public MCRThumbnailImageImpl(String implName) {
        super(implName);
        derivateTypes = MCRConfiguration2.splitValue(getProperties().get(DERIVATE_TYPES)).distinct().toList();
    }

    @Override
    protected MCRTileInfo createTileInfo(String id) throws MCRIIIFImageNotFoundException {
        MCRObjectID mcrID = calculateMCRObjectID(id).orElseThrow(() -> new MCRIIIFImageNotFoundException(id));
        if (mcrID.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
            MCRDerivate mcrDer = MCRMetadataManager.retrieveMCRDerivate(mcrID);
            return new MCRTileInfo(mcrID.toString(), mcrDer.getDerivate().getInternals().getMainDoc(), null);
        } else {
            Optional<MCRTileInfo> tileInfoForMCRObject = createTileInfoForMCRObject(mcrID);
            if (tileInfoForMCRObject.isPresent()) {
                return tileInfoForMCRObject.get();
            }
            Optional<MCRTileInfo> tileInfoForDerivateLink = createTileInfoForDerivateLink(mcrID);
            if (tileInfoForDerivateLink.isPresent()) {
                return tileInfoForDerivateLink.get();
            }
        }
        throw new MCRIIIFImageNotFoundException(id);
    }

    @Override
    protected boolean checkPermission(String identifier, MCRTileInfo tileInfo) {
        Optional<MCRObjectID> mcrObjId = calculateMCRObjectID(identifier);
        if (mcrObjId.isPresent()) {
            return MCRAccessManager.checkPermission(mcrObjId.get(), MCRAccessManager.PERMISSION_PREVIEW) ||
                MCRAccessManager.checkPermission(tileInfo.derivate(), MCRAccessManager.PERMISSION_VIEW) ||
                MCRAccessManager.checkPermission(tileInfo.derivate(), MCRAccessManager.PERMISSION_READ);
        }
        return false;
    }

    /**
     * Builds a MyCoRe Object ID from the given IIIF image id parameter.
     * <p>
     * Subclasses may override it.
     *
     * @param id the IIIF image id parameter
     * @return an Optional containing the MyCoRe ID
     */
    protected Optional<MCRObjectID> calculateMCRObjectID(String id) {
        return MCRObjectID.isValid(id) ? Optional.of(MCRObjectID.getInstance(id)) : Optional.empty();
    }

    private Optional<MCRTileInfo> createTileInfoForMCRObject(MCRObjectID mcrID) {
        MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrID);
        for (String derType : derivateTypes) {
            for (MCRMetaEnrichedLinkID derLink : mcrObj.getStructure().getDerivates()) {
                if (derLink.getClassifications().stream()
                    .map(MCRCategoryID::toString)
                    .anyMatch(derType::equals)) {
                    final String maindoc = derLink.getMainDoc();
                    if (maindoc != null) {
                        Optional<MCRTileInfo> tileInfoForFile = createTileInfoForFile(derLink.getXLinkHref(), maindoc);
                        if (tileInfoForFile.isPresent()) {
                            return tileInfoForFile;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<MCRTileInfo> createTileInfoForDerivateLink(MCRObjectID mcrID) {
        Collection<String> linkedImages = MCRLinkTableManager.getInstance()
            .getDestinationOf(mcrID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE_LINK);
        for (String linkedImage : linkedImages) {
            MCRObjectID deriID = MCRObjectID.getInstance(
                linkedImage.substring(0, linkedImage.indexOf('/')));
            MCRDerivate deri = MCRMetadataManager.retrieveMCRDerivate(deriID);
            if (deri.getDerivate().getClassifications().stream()
                .map(classi -> classi.getClassId() + ":" + classi.getCategId())
                .anyMatch(derivateTypes::contains)) {
                Optional<MCRTileInfo> tileInfoForFile = createTileInfoForFile(deriID.toString(),
                    linkedImage.substring(linkedImage.indexOf('/') + 1));
                if (tileInfoForFile.isPresent()) {
                    return tileInfoForFile;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<MCRTileInfo> createTileInfoForFile(String derID, String file) {
        final MCRTileInfo mcrTileInfo = new MCRTileInfo(derID, file, null);
        return Optional.of(mcrTileInfo)
            .filter(t -> this.tileFileProvider.getTileFile(t).filter(Files::exists).isPresent());
    }
}
