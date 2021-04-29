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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.iview2.frontend.MCRPDFTools;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

public class MCRPDFThumbnailJobAction extends MCRJobAction {

    public static final String DERIVATE_PARAMETER = "derivate";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRPDFThumbnailJobAction() {
    }

    public MCRPDFThumbnailJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return getClass().getName();
    }

    @Override
    public void execute() {
        final String derivateIDString = this.job.getParameter(DERIVATE_PARAMETER);
        final MCRObjectID derivateID = MCRObjectID.getInstance(derivateIDString);
        final MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        MCRTileInfo tileInfo = new MCRTileInfo(derivate.getId().toString(),
            derivate.getDerivate().getInternals().getMainDoc(), null);
        if (tileInfo.getImagePath().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            try {
                Path p = MCRPath.getPath(tileInfo.getDerivate(), tileInfo.getImagePath());
                BufferedImage bImage = MCRPDFTools.getThumbnail(-1, p, false);
                Path pImg = Files.createTempFile("MyCoRe-Thumbnail-", ".png");
                try (OutputStream os = Files.newOutputStream(pImg)) {
                    ImageIO.write(bImage, "png", os);
                }
                try {
                    MCRImage mcrImage = MCRImage.getInstance(pImg, tileInfo.getDerivate(), tileInfo.getImagePath());
                    mcrImage.setTileDir(MCRIView2Tools.getTileDir());
                    mcrImage.tile();
                } catch (Exception e) {
                    LOGGER.error("Error creating tiles for thumbnail of PDF: " + pImg.toString(), e);
                } finally {
                    //delete file
                    try (InputStream is = Files.newInputStream(pImg, StandardOpenOption.DELETE_ON_CLOSE)) {
                        is.read();
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error creating thumbnail for PDF", e);
            }
        }
    }

    @Override
    public void rollback() {
        // do need to rollback
    }
}
