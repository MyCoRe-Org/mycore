/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.media.services;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.media.MCRAudioObject;
import org.mycore.media.MCRMediaObject;
import org.mycore.media.MCRMediaParser;
import org.mycore.media.MCRVideoObject;

/**
 * This class implements the AudioVideoExtender functions and reads 
 * technical metadata from stored files. 
 * Currently only used within MILESS projects.
 * 
 * <code>
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.Class  set with this class to use
 * </code>
 * 
 * @author Ren\u00E9 Adler (Eagle)
 * @version $Revision$ $Date$
 */
public class MCRMediaAVExtender extends MCRAudioVideoExtender {
    /** The logger */
    private final static Logger LOGGER = LogManager.getLogger(MCRMediaAVExtender.class);

    /** The asset file this extender belongs to */
    protected MCRFileReader file;

    protected MCRMediaObject media;

    /**
     * Creates a new MCRAudioVideoExtender. The instance has to be initialized
     * by invoking init() before it can be used.
     */
    public MCRMediaAVExtender() {
    }

    /**
     * Initializes this AudioVideoExtender and gets technical metadata from the
     * server that holds the streaming asset. Subclasses must override this
     * method!
     * 
     * @param file
     *            the MCRFile that this extender belongs to
     */
    public void init(MCRFileReader file) throws MCRPersistenceException {
        this.file = file;

        try {
            StringTokenizer tok = new StringTokenizer(file.getID(), "_");
            tok.nextToken();
            String derivateID = tok.nextToken();

            org.jdom2.Document mediaXML = null;
            LOGGER.info("Get metadata for file " + file.getStoreID() + "@" + file.getStorageID() + "...");
            try {
                mediaXML = MCRMediaIFSTools.getMetadataFromStore(derivateID, file.getPath());
            } catch (Throwable e) {
                LOGGER.warn(e.getMessage());
            }

            if (mediaXML != null) {
                LOGGER.info("use metadata from Metadata Store.");
                this.media = MCRMediaObject.buildFromXML(mediaXML.getRootElement());
            } else {
                LOGGER.info("get metadata from stored file.");
                MCRMediaParser mparser = new MCRMediaParser();
                this.media = mparser.parse(file);
            }

            LOGGER.debug(media);
        } catch (Exception ex) {
            LOGGER.warn(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Returns the Media Object.
     * 
     * @return the Media Object
     */
    public MCRMediaObject getMediaObject() {
        return media;
    }

    /**
     * Returns the Video Object if is a Video file.
     * 
     * @return the Video Object or null
     */
    public MCRVideoObject getVideoObject() {
        return (isVideo() ? (MCRVideoObject) media : null);
    }

    /**
     * Returns the Audio Object if is a Audio file.
     * 
     * @return the Audio Object or null
     */
    public MCRAudioObject getAudioObject() {
        return (isAudio() ? (MCRAudioObject) media : null);
    }

    /**
     * Checks if Media Object is a Video.
     * 
     * @return bool
     */
    public boolean isVideo() {
        return (media != null && media.getType() == MCRMediaObject.MediaType.VIDEO);
    }

    /**
     * Checks if Media Object is a Audio.
     * 
     * @return bool
     */
    public boolean isAudio() {
        return (media != null && media.getType() == MCRMediaObject.MediaType.AUDIO);
    }

    /**
     * Returns the ID of the content type of this asset
     * 
     * @return the ID of the content type of this asset
     */
    public String getContentTypeID() {
        if (media.getMimeType() != null) {
            try {
                LOGGER.info("Try to detect file content type by mime type '" + media.getMimeType() + "'...");
                MCRFileContentType cType = MCRFileContentTypeFactory.getTypeByMimeType(media.getMimeType());
                contentTypeID = cType.getID();
            } catch (Throwable ex) {
                LOGGER.info("Try to detect file content type with extension '" + file.getExtension() + "'...");
                try {
                    MCRFileContentType cType = MCRFileContentTypeFactory.getType(file.getExtension());
                    contentTypeID = cType.getID();
                } catch (Throwable ex2) {
                    LOGGER.info("Try to detect file content type with format '" + media.getFormat() + "'...");
                    contentTypeID = detectContentTypeIDByFormat(media.getFormat());
                    if (contentTypeID == null) {
                        LOGGER.info("Try to detect file content type with subformat '"
                            + (isVideo() ? getVideoObject().getSubFormat() : getAudioObject().getSubFormat()) + "'...");
                        contentTypeID = detectContentTypeIDByFormat(
                            isVideo() ? getVideoObject().getSubFormat() : getAudioObject().getSubFormat());
                        if (contentTypeID == null) {
                            contentTypeID = file.getContentTypeID();
                        }
                    }
                }
            }
        } else {
            LOGGER.info("Try to detect file content type with extension '" + file.getExtension() + "'...");
            try {
                MCRFileContentType cType = MCRFileContentTypeFactory.getType(file.getExtension());
                contentTypeID = cType.getID();
            } catch (Throwable ex2) {
                LOGGER.info("Try to detect file content type with format '" + media.getFormat() + "'...");
                contentTypeID = detectContentTypeIDByFormat(media.getFormat());
                if (contentTypeID == null) {
                    LOGGER.info("Try to detect file content type with subformat '"
                        + (isVideo() ? getVideoObject().getSubFormat() : getAudioObject().getSubFormat()) + "'...");
                    contentTypeID = detectContentTypeIDByFormat(
                        isVideo() ? getVideoObject().getSubFormat() : getAudioObject().getSubFormat());
                    if (contentTypeID == null) {
                        contentTypeID = file.getContentTypeID();
                    }
                }
            }
        }

        return contentTypeID;
    }

    /**
     * Returns the ID of the content type witch detected by parsing format string.
     * 
     * @param can be the format of the media container or the subformat of stream.
     * @return the ID of the content type
     */
    private String detectContentTypeIDByFormat(String format) {
        String cType = null;

        if (format.contains("MPEG Layer 3")) {
            cType = "mp3";
        } else if (format.contains("MPEG")) {
            cType = "mpegvid";
        } else if (format.contains("RealVideo")) {
            cType = "realvid";
        } else if (format.contains("RealAudio")) {
            cType = "realaud";
        } else if (format.contains("Flash Video")) {
            cType = "flv";
        } else if (format.contains("Wave File")) {
            cType = "wav";
        }

        return cType;
    }

    @Override
    public MCRContent getPlayerStarter(String startPos, String stopPos) throws IOException {
        return null;
    }

}
