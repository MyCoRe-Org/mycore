package org.mycore.iview2.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTileEventHandler;
import org.mycore.imagetiler.MCRTiledPictureProps;

/**
 * A slave thread of {@link MCRImageTiler}
 *
 * This class can be extended. Any extending class should provide and implementation for {@link #getMCRImage()}.
 * To get the extending class invoked, one need to define a MyCoRe property, which defaults to:
 * <code>MCR.Module-iview2.MCRTilingActionImpl=org.mycore.iview2.services.MCRTilingAction</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTilingAction implements Runnable {
    private static Logger LOGGER = LogManager.getLogger(MCRTilingAction.class);

    protected MCRTileJob tileJob = null;

    public MCRTilingAction(MCRTileJob image) {
        this.tileJob = image;
    }

    /**
     * takes a {@link MCRTileJob} and tiles the referenced {@link MCRImage} instance.
     *
     * Also this updates tileJob properties of {@link MCRTileJob} in the database.
     */
    public void run() {
        tileJob.setStart(new Date());
        MCRImage image;
        Path tileDir = MCRIView2Tools.getTileDir();
        try {
            image = getMCRImage();
            image.setTileDir(tileDir);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving image for job: " + tileJob, e);
            return;
        }
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        final Session session = MCRHIBConnection.instance().getSession();
        Transaction transaction = null;
        try {
            MCRTileEventHandler tileEventHandler = new MCRTileEventHandler() {
                Transaction transaction;

                @Override
                public void preImageReaderCreated() {
                    transaction = session.beginTransaction();
                }

                @Override
                public void postImageReaderCreated() {
                    session.clear(); //beside tileJob, no write access so far
                    if (transaction.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                        transaction.commit();
                    }
                }

            };
            try {
                MCRTiledPictureProps picProps = image.tile(tileEventHandler);
                tileJob.setFinished(new Date());
                tileJob.setStatus(MCRJobState.FINISHED);
                tileJob.setHeight(picProps.getHeight());
                tileJob.setWidth(picProps.getWidth());
                tileJob.setTiles(picProps.getTilesCount());
                tileJob.setZoomLevel(picProps.getZoomlevel());
            } catch (IOException e) {
                LOGGER.error("IOException occured while tiling a queued picture", e);
                throw e;
            }
            transaction = session.beginTransaction();
            session.update(tileJob);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null && transaction.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                transaction.rollback();
            }
            try {
                Files.deleteIfExists(MCRImage.getTiledFile(tileDir,tileJob.getDerivate(), tileJob.getPath()));
            } catch (IOException e1) {
                LOGGER.error("Could not delete tile file after error!", e);
            }

        } finally {
            session.close();
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }

    /**
     * @return MCRImage instance based on the information provided by {@link #tileJob}
     * @throws IOException thrown by {@link MCRImage#getInstance(Path, String, String)}
     */
    protected MCRImage getMCRImage() throws IOException {
        MCRPath file = MCRPath.getPath(tileJob.getDerivate(), tileJob.getPath());
        return MCRImage.getInstance(file, file.getOwner(), file.getOwnerRelativePath());
    }

    @Override
    public String toString() {
        if (tileJob == null) {
            return "unassigned tiling action";
        }
        return tileJob.toString();
    }

}
