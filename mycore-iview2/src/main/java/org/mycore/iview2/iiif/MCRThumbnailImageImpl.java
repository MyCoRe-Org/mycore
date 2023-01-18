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

package org.mycore.iview2.iiif;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.mycore.access.MCRAccessManager;
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

    private static Set<String> derivateTypes;

    public MCRThumbnailImageImpl(String implName) {
        super(implName);
        derivateTypes = new HashSet<String>();
        derivateTypes.addAll(Arrays.asList(getProperties().get(DERIVATE_TYPES).split(",")));
    }

    @Override
    protected MCRTileInfo createTileInfo(String id) throws MCRIIIFImageNotFoundException {
        if (!MCRObjectID.isValid(id)) {
            throw new MCRIIIFImageNotFoundException(id);
        }

        MCRObjectID mcrID = MCRObjectID.getInstance(id);
        if (mcrID.getTypeId().equals("derivate")) {
            MCRDerivate mcrDer = MCRMetadataManager.retrieveMCRDerivate(mcrID);
            return new MCRTileInfo(mcrID.toString(), mcrDer.getDerivate().getInternals().getMainDoc(), null);
        } else {
            MCRObject mcrObj = MCRMetadataManager.retrieveMCRObject(mcrID);
            for (String type : derivateTypes) {
                for (MCRMetaEnrichedLinkID derLink : mcrObj.getStructure().getDerivates()) {
                    if (derLink.getClassifications().stream()
                        .map(MCRCategoryID::toString)
                        .anyMatch(type::equals)) {
                        final String maindoc = derLink.getMainDoc();
                        if (maindoc != null) {
                            Optional<MCRTileInfo> tileInfoForFile = createTileInfoForFile(derLink.getXLinkHref(),
                                maindoc);
                            if (tileInfoForFile.isPresent()) {
                                return tileInfoForFile.get();
                            }
                        }
                    }
                }
            }
            Optional<String> linkedImage = MCRLinkTableManager.instance()
                .getDestinationOf(mcrID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE_LINK).stream().findFirst();
            if (linkedImage.isPresent()) {
                MCRObjectID deriID = MCRObjectID.getInstance(
                    linkedImage.get().substring(0, linkedImage.get().indexOf("/")));
                MCRDerivate deri = MCRMetadataManager.retrieveMCRDerivate(deriID);
                if (deri.getDerivate().getClassifications().stream()
                    .map(classi -> classi.getClassId() + ":" + classi.getCategId())
                    .anyMatch(derivateTypes::contains)) {
                    Optional<MCRTileInfo> tileInfoForFile = createTileInfoForFile(
                        deriID.toString(),
                        linkedImage.get().substring(linkedImage.get().indexOf("/") + 1));
                    if (tileInfoForFile.isPresent()) {
                        return tileInfoForFile.get();
                    }
                }

            }
        }
        throw new MCRIIIFImageNotFoundException(id);
    }

    @Override
    protected boolean checkPermission(String identifier, MCRTileInfo tileInfo) {
        return MCRAccessManager.checkPermission(identifier, MCRAccessManager.PERMISSION_PREVIEW) ||
            MCRAccessManager.checkPermission(tileInfo.getDerivate(), MCRAccessManager.PERMISSION_VIEW) ||
            MCRAccessManager.checkPermission(tileInfo.getDerivate(), MCRAccessManager.PERMISSION_READ);
    }

    private Optional<MCRTileInfo> createTileInfoForFile(String derID, String file) {
        final MCRTileInfo mcrTileInfo = new MCRTileInfo(derID, file, null);
        final Optional<Path> tileFile = this.tileFileProvider.getTileFile(mcrTileInfo);
        if (tileFile.isPresent() && Files.exists(tileFile.get())) {
            return Optional.of(mcrTileInfo);
        }
        return Optional.empty();
    }
}
