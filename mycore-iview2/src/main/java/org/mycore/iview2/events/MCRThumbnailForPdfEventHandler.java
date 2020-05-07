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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.iview2.backend.MCRDefaultTileFileProvider;
import org.mycore.iview2.backend.MCRPDFThumbnailJobAction;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueue;

/**
 * This event handler creates iview2 files for title pages in PDFs which can be
 * used as thumbnails in IIIF API
 *
 * @author Robert Stephan
 */
public class MCRThumbnailForPdfEventHandler extends MCREventHandlerBase {

    public static final MCRDefaultTileFileProvider TILE_FILE_PROVIDER = new MCRDefaultTileFileProvider();

    private static final Logger LOGGER = LogManager.getLogger(MCRThumbnailForPdfEventHandler.class);

    private static final MCRJobQueue PDF_THUMBNAIL_JOB_QUEUE = initializeJobQueue();

    private static final List<String> DERIVATE_TYPES_FOR_CONTENT = MCRConfiguration2
        .getOrThrow("MCRIIIFImage.Iview.ThumbnailForPdfEventHandler.Derivate.Types",
            MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

    private static MCRJobQueue initializeJobQueue() {
        LOGGER.info("Initializing jobQueue for PDF Thumbnail generation!");
        return MCRJobQueue.getInstance(MCRPDFThumbnailJobAction.class);
    }

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
            final MCRJob job = new MCRJob(MCRPDFThumbnailJobAction.class);
            job.setParameter(MCRPDFThumbnailJobAction.DERIVATE_PARAMETER, der.getId().toString());
            PDF_THUMBNAIL_JOB_QUEUE.add(job);
        }
    }

    private void deleteThumbnail(MCRDerivate der) {
        String mainDoc = der.getDerivate().getInternals().getMainDoc();
        if (mainDoc != null && mainDoc.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            MCRTileInfo tileInfo = new MCRTileInfo(der.getId().toString(), mainDoc, null);
            Optional<Path> oPIview2File = TILE_FILE_PROVIDER.getTileFile(tileInfo);
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
            if (DERIVATE_TYPES_FOR_CONTENT.contains(classid)) {
                String mainDoc = der.getDerivate().getInternals().getMainDoc();
                return (mainDoc != null && mainDoc.toLowerCase(Locale.ROOT).endsWith(".pdf"));
            }
        }
        return false;
    }
}
