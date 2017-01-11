/*
 * 
 * $Revision: 21346 $ $Date: 2011-06-30 14:53:10 +0200 (Do, 30. Jun 2011) $
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

package org.mycore.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.datamodel.ifs.MCROldFile;

/**
 * Get technical metadata from supported file. 
 * 
 * @author Ren\u00E9 Adler (Eagle)
 * 
 */
@SuppressWarnings("deprecation")
public class MCRMediaParser {
    private static final Logger LOGGER = LogManager.getLogger(MCRMediaParser.class);

    private static MCRConfiguration config = MCRConfiguration.instance();

    private static List<MCRMediaParser> parsers;

    public MCRMediaParser() {
        if (parsers == null) {
            parsers = new ArrayList<MCRMediaParser>();

            Map<String, String> supportedParsers = config.getPropertiesMap("MCR.Media.Parser.");
            for (String name : supportedParsers.keySet()) {
                try {
                    LOGGER.info("instantiate Parser \"" + config.getString(name) + "\"...");
                    MCRMediaParser parser = config.getInstanceOf(name, (String) null);
                    parsers.add(parser);
                } catch (Throwable ex) {
                    LOGGER.warn("Couldn't instantiate Parser \"" + config.getString(name) + "\" because "
                        + ex.getMessage() + ".");
                }
            }
        }
    }

    public boolean isValid() {
        return true;
    }

    public void close() {
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(File file) {
        return parsers.stream().anyMatch(parser -> parser.isValid() && parser.isFileSupported(file));
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(MCROldFile file) {
        return parsers.stream().anyMatch(parser -> parser.isValid() && parser.isFileSupported(file));
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(org.mycore.datamodel.ifs.MCRFile file) {
        return parsers.stream().anyMatch(parser -> parser.isValid() && parser.isFileSupported(file));
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(MCRFileReader file) {
        return parsers.stream().anyMatch(parser -> parser.isValid() && parser.isFileSupported(file));
    }

    /**
     * Parse the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse(File file) throws Exception {
        MCRMediaObject media = null;

        for (MCRMediaParser parser : parsers) {
            if (parser.isValid() && parser.isFileSupported(file)) {
                try {
                    media = parser.parse(file);
                    if (media != null)
                        break;
                } catch (Exception ex) {
                    LOGGER.warn(ex);
                }
            }
        }

        return media;
    }

    /**
     * Parse the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse(MCROldFile file) throws Exception {
        MCRMediaObject media = null;

        for (MCRMediaParser parser : parsers) {
            if (parser.isValid() && parser.isFileSupported(file)) {
                try {
                    media = parser.parse(file);
                    if (media != null)
                        break;
                } catch (Exception ex) {
                    LOGGER.warn(ex);
                }
            }
        }

        return media;
    }

    /**
     * Parse the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse(org.mycore.datamodel.ifs.MCRFile file) throws Exception {
        MCRMediaObject media = null;

        for (MCRMediaParser parser : parsers) {
            if (parser.isValid() && parser.isFileSupported(file)) {
                try {
                    media = parser.parse(file);
                    if (media != null)
                        break;
                } catch (Exception ex) {
                    LOGGER.warn(ex);
                }
            }
        }

        return media;
    }

    /**
     * Parse the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse(MCRFileReader file) throws Exception {
        MCRMediaObject media = null;

        for (MCRMediaParser parser : parsers) {
            if (parser.isValid() && parser.isFileSupported(file)) {
                try {
                    media = parser.parse(file);
                    if (media != null)
                        break;
                } catch (Exception ex) {
                    LOGGER.warn(ex);
                }
            }
        }

        return media;
    }

    protected File toFile(MCROldFile file) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");

        return new File(storeURI + "/" + file.getStorageID());
    }

    protected File toFile(org.mycore.datamodel.ifs.MCRFile file) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");

        return new File(storeURI + "/" + file.getStorageID());
    }

    protected File toFile(MCRFileReader file) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");

        return new File(storeURI + "/" + file.getStorageID());
    }

    /**
     * Take a Snapshot from a supported MediaObject.
     * 
     * @param media
     *              the MediaObject
     * @param seek
     *              position to take a snapshot
     * @param maxWidth
     *              maximum output width
     * @param maxHeight
     *              maximum output height
     * @param keepAspect
     *              set to keep aspect ratio
     * @return
     *              the snapshot
     */
    public synchronized static byte[] getThumbnail(MCRMediaObject media, long seek, int maxWidth, int maxHeight,
        boolean keepAspect) throws Exception {
        if (media.hasThumbnailSupport()) {
            byte[] thumb = media.getThumbnail(media, seek, maxWidth, maxHeight, keepAspect);

            return thumb;
        } else {
            throw new Exception("The " + media.getClass().getName() + " hasn't support for getThumbnail");
        }
    }

    /**
     * Take a Snapshot from a supported MediaObject.
     * 
     * @return
     *              the snapshot
     * @see #getThumbnail( MCRMediaObject , long, int, int, boolean )
     */
    public synchronized static byte[] getThumbnail(MCRMediaObject media) throws Exception {
        long seek = media.type == MCRMediaObject.MediaType.VIDEO ? (media.getMaxSeekPosition() / 1000) / 2 : 0;
        return getThumbnail(media, seek, 0, 0, true);
    }

    /**
     * Take multiple Snapshots from a supported MediaObject.
     * 
     * @param steps
     *              count of steps to take a snapshot
     * @return an ArrayList of snapshots  
     * @see #getThumbnail( MCRMediaObject , long, int, int, boolean )
     */
    public synchronized static ArrayList<byte[]> getThumbnail(MCRMediaObject media, int steps) throws Exception {
        ArrayList<byte[]> thumbs = new ArrayList<byte[]>();

        for (int c = 0; c < steps; c++) {
            long seek = (media.getMaxSeekPosition() / steps) * c;
            if (media.type == MCRMediaObject.MediaType.VIDEO)
                seek = ((seek + 5000) < media.getMaxSeekPosition() ? seek + 5000 : seek) / 1000;
            byte[] thumb = getThumbnail(media, seek, 0, 0, true);
            thumbs.add(thumb);
        }

        return thumbs;
    }
}
