package org.mycore.iview2.backend;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

                    MCRImage mcrImage = MCRImage.getInstance(pImg, tileInfo.getDerivate(), tileInfo.getImagePath());
                    mcrImage.setTileDir(MCRIView2Tools.getTileDir());
                    mcrImage.tile();
                } finally {
                    Files.deleteIfExists(pImg);
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

    @Override
    public void rollback() {

    }
}
