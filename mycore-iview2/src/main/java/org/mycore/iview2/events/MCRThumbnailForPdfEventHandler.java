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

package org.mycore.iview2.events;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.backend.MCRDefaultTileFileProvider;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.iview2.frontend.MCRPDFTools;
import org.mycore.iview2.services.MCRIView2Tools;

/**
 * This event handler creates iview2 files for title pages in PDFs which can be
 * used as thumbnails in IIIF API
 * 
 * @author Robert Stephan
 */
public class MCRThumbnailForPdfEventHandler extends MCREventHandlerBase {

    private static Logger LOGGER = LogManager.getLogger(MCRThumbnailForPdfEventHandler.class);

    private static List<String> derivateTypesForContent = MCRConfiguration2
            .getOrThrow("MCRIIIFImage.Iview.ThumbnailForPdfEventHandler.Derivate.Types",
                    MCRConfiguration2::splitValue)
            .collect(Collectors.toList());

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        updateThumbnail(der);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        updateThumbnail(der);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        updateThumbnail(der);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        deleteThumbnail(der);
    }

    private void updateThumbnail(MCRDerivate der) {
        deleteThumbnail(der);
        if (isQualifyingDerivate(der)) {
            MCRTileInfo tileInfo = new MCRTileInfo(der.getId().toString(),
                    der.getDerivate().getInternals().getMainDoc(), null);
            if (tileInfo.getImagePath().toLowerCase().endsWith(".pdf")) {
                try {
                    Path p = MCRPath.getPath(tileInfo.getDerivate(), tileInfo.getImagePath());
                    BufferedImage bImage = MCRPDFTools.getThumbnail(1024, p, false);
                    Path pImg = Files.createTempFile("MyCoRe-Thumbnail-", ".png");
                    try (OutputStream os = Files.newOutputStream(pImg, StandardOpenOption.DELETE_ON_CLOSE)) {
                        ImageIO.write(bImage, "png", os);

                        MCRImage mcrImage = MCRImage.getInstance(pImg, tileInfo.getDerivate(), tileInfo.getImagePath());
                        mcrImage.setTileDir(MCRIView2Tools.getTileDir());
                        mcrImage.tile();
                    }
                    // RS: throws java.nio.file.AccessDeniedException, when immediately called after
                    // tile()
                    // used OpenOption.DELETE_ON_CLOSE
                    // Files.deleteIfExists(pImg);
                } catch (IOException e) {
                    LOGGER.error("Error creating thumbnail for PDF", e);
                }
            }
        }
    }

    private void deleteThumbnail(MCRDerivate der) {
        String mainDoc = der.getDerivate().getInternals().getMainDoc();
        if (mainDoc != null && mainDoc.toLowerCase().endsWith(".pdf")) {
            MCRDefaultTileFileProvider tileFileProvider = new MCRDefaultTileFileProvider();
            MCRTileInfo tileInfo = new MCRTileInfo(der.getId().toString(), mainDoc, null);
            Optional<Path> oPIview2File = tileFileProvider.getTileFile(tileInfo);
            if (oPIview2File.isPresent()) {
                try {
                    Files.deleteIfExists(oPIview2File.get());
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    private boolean isQualifyingDerivate(MCRDerivate der) {
        for (MCRMetaClassification c : der.getDerivate().getClassifications()) {
            String classid = c.getClassId() + ":" + c.getCategId();
            if (derivateTypesForContent.contains(classid)) {
                String mainDoc = der.getDerivate().getInternals().getMainDoc();
                return (mainDoc != null && mainDoc.toLowerCase().endsWith(".pdf"));
            }
        }
        return false;
    }
}
