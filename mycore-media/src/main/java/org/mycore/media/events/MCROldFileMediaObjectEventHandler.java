package org.mycore.media.events;

import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.ifs.MCROldFile;
import org.mycore.media.MCRMediaObject;
import org.mycore.media.MCRMediaParser;
import org.mycore.media.services.MCRMediaIFSTools;

@SuppressWarnings("deprecation")
public class MCROldFileMediaObjectEventHandler implements MCREventHandler {
    private static Logger LOGGER = LogManager.getLogger(MCREventManager.class);

    public void doHandleEvent(MCREvent evt) throws MCRException {
        MCROldFile file = (MCROldFile) (evt.get("file"));
        if (file.getSize() < 1)
            return; // do not handle empty files, they have no StoreID!

        if (MCREvent.UPDATE_EVENT.equals(evt.getEventType())) {
            try {
                MCRMediaParser mparser = new MCRMediaParser();

                if (mparser.isFileSupported(file)) {
                    MCRMediaObject media = mparser.parse(file);

                    LOGGER.debug(media);
                    deleteMetadata(file);
                    storeMetadata(media, file);

                    if (media.hasThumbnailSupport()) {
                        deleteThumbnail(file);
                        storeThumbnail(media, file);
                    }
                }

                mparser.close();
            } catch (Exception ex) {
                LOGGER.warn(ex.getMessage());
            }
        } else if (MCREvent.DELETE_EVENT.equals(evt.getEventType())) {
            deleteMetadata(file);
            deleteThumbnail(file);
        }
    }

    public void undoHandleEvent(MCREvent evt) throws MCRException {
        MCROldFile file = (MCROldFile) (evt.get("file"));
        if (file.getSize() < 1)
            return; // do not handle empty files, they have no StoreID!
        deleteMetadata(file);
        deleteThumbnail(file);
    }

    private void deleteMetadata(MCROldFile file) throws MCRException {
        StringTokenizer tok = new StringTokenizer(file.getOwnerID(), "_");
        tok.nextToken();
        String derivateID = tok.nextToken();

        try {
            MCRMediaIFSTools.deleteMetadata(derivateID, file.getPath());
        } catch (Exception ex) {
            throw new MCRException("Couldn' delete MediaObject metadata. Because: " + ex.getMessage(), ex);
        }
    }

    private void storeMetadata(Object media, MCROldFile file) throws MCRException {
        StringTokenizer tok = new StringTokenizer(file.getOwnerID(), "_");
        tok.nextToken();
        String derivateID = tok.nextToken();

        try {
            MCRMediaIFSTools.storeMetadata(((MCRMediaObject) media).toXML(), derivateID, file.getPath());
        } catch (Exception ex) {
            throw new MCRException("Couldn' store MediaObject metadata. Because: " + ex.getMessage(), ex);
        }
    }

    private void deleteThumbnail(MCROldFile file) throws MCRException {
        StringTokenizer tok = new StringTokenizer(file.getOwnerID(), "_");
        tok.nextToken();
        String derivateID = tok.nextToken();

        try {
            MCRMediaIFSTools.deleteThumbnail(derivateID, file.getPath());
        } catch (Exception ex) {
            throw new MCRException("Couldn't delete thumbnail. Because: " + ex.getMessage(), ex);
        }
    }

    private void storeThumbnail(Object media, MCROldFile file) throws MCRException {
        StringTokenizer tok = new StringTokenizer(file.getOwnerID(), "_");
        tok.nextToken();
        String derivateID = tok.nextToken();

        try {
            byte[] thumb = MCRMediaParser.getThumbnail((MCRMediaObject) media);

            MCRMediaIFSTools.storeThumbnail(thumb, derivateID, file.getPath());
        } catch (Exception ex) {
            throw new MCRException("Couldn't store thumbnail. Because: " + ex.getMessage(), ex);
        }
    }
}
