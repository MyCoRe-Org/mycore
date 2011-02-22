package org.mycore.iview2.services;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.imagetiler.MCRImage;
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
    protected MCRTileJob tileJob = null;

    private static SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static Logger LOGGER = Logger.getLogger(MCRTilingAction.class);

    /**
     * takes a {@link MCRTileJob} and tiles the referenced {@link MCRImage} instance.
     * 
     * Also this updates tileJob properties of {@link MCRTileJob} in the database.
     */
    public void run() {
        MCRSession mcrSession=MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = session.beginTransaction();
        try {
            MCRImage image = getMCRImage();
            image.setTileDir(MCRIView2Tools.getTileDir());
            MCRTiledPictureProps picProps = new MCRTiledPictureProps();
            tileJob.setStart(new Date());
            try {
                picProps = image.tile();
                tileJob.setFinished(new Date());
                tileJob.setStatus(MCRJobState.FINISHED);
                tileJob.setHeight(picProps.getHeight());
                tileJob.setWidth(picProps.getWidth());
                tileJob.setTiles(picProps.getTilesCount());
                tileJob.setZoomLevel(picProps.getZoomlevel());
            } catch (IOException e) {
                LOGGER.error("IOException occured while tiling a queued picture", e);
            }
            session.update(tileJob);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null) {
                transaction.rollback();
            }
        } finally {
            session.close();
            MCRSessionMgr.releaseCurrentSession();
            mcrSession.close();
        }
    }

    /**
     * @return MCRImage instance based on the information provided by {@link #tileJob}
     * @throws IOException thrown by {@link MCRImage#getInstance(java.io.File, String, String)}
     */
    protected MCRImage getMCRImage() throws IOException {
        MCRFile file = MCRIView2Tools.getMCRFile(tileJob.getDerivate(), tileJob.getPath());
        MCRImage imgTiler = MCRImage.getInstance(MCRIView2Tools.getFile(file), file.getOwnerID(), file.getAbsolutePath());
        return imgTiler;
    }

    public MCRTilingAction(MCRTileJob image) {
        this.tileJob = image;
    }
}
