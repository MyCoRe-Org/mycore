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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.datamodel.ifs.MCROldFile;

/**
 * Parse the output of the Helix ViewSource HTML page and create the 
 * depend MediaObject. 
 * 
 * It is only used as fallback if the MediaInfoParser fails.
 * 
 * @author Ren\u00E9 Adler (Eagle)
 *
 */
@SuppressWarnings("deprecation")
public class MCRMediaViewSourceParser extends MCRMediaParser {
    private static final String[] supportedFileExts = { "ogg", "ogm", "avi", "wav", "mpeg", "mpg", "vob", "mp4", "mpgv",
        "mpv", "m1v",
        "m2v", "mp2", "mp3", "asf", "wma", "wmv", "qt", "mov", "rm", "rmvb", "ra", "flv", "f4v", "flac", "dat", "w64" };

    private static final Logger LOGGER = LogManager.getLogger(MCRMediaViewSourceParser.class);

    private static MCRMediaViewSourceParser instance = new MCRMediaViewSourceParser();

    private static MCRConfiguration config = MCRConfiguration.instance();

    public static MCRMediaViewSourceParser getInstance() {
        return instance;
    }

    private MCRMediaViewSourceParser() {
    }

    /**
     * Checks if ViewSource parser is valid.
     * 
     * @return boolean if is vaild
     */
    public boolean isValid() {
        //TODO: check if ViewSource url is set
        return true;
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(String fileName) {
        if (fileName.endsWith(".")) {
            return false;
        }

        int pos = fileName.lastIndexOf(".");
        if (pos != -1) {
            String ext = fileName.substring(pos + 1);

            for (String sExt : supportedFileExts) {
                if (!sExt.equals(ext))
                    continue;
                return isValid();
            }
        }

        return false;
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(File file) {
        return isFileSupported(file.getPath());
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(MCROldFile file) {
        return isFileSupported(toFile(file));
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(org.mycore.datamodel.ifs.MCRFile file) {
        return isFileSupported(toFile(file));
    }

    /**
     * Checks if given file is supported.
     * 
     * @return boolean if true
     */
    public boolean isFileSupported(MCRFileReader file) {
        return isFileSupported(toFile(file));
    }

    public synchronized MCRMediaObject parse(File file) throws Exception {
        throw new Exception("File is'n supported by ViewSource Parser");
    }

    public synchronized MCRMediaObject parse(MCROldFile file) throws Exception {
        return setFileInfo(parse(buildViewSourceURL(file)), toFile(file));
    }

    public synchronized MCRMediaObject parse(org.mycore.datamodel.ifs.MCRFile file) throws Exception {
        return setFileInfo(parse(buildViewSourceURL(file)), toFile(file));
    }

    public synchronized MCRMediaObject parse(MCRFileReader file) throws Exception {
        return setFileInfo(parse(buildViewSourceURL(file)), toFile(file));
    }

    private MCRMediaObject setFileInfo(MCRMediaObject media, File file) {
        media.fileName = file.getName();
        String path = file.getAbsolutePath();
        media.folderName = path.substring(path.indexOf(file.getName()));

        return media;
    }

    /**
     * Parse MediaInfrom of the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    private synchronized MCRMediaObject parse(String vsURL) throws Exception {
        MCRMediaObject media = new MCRMediaObject();

        LOGGER.info("parse " + vsURL + "...");

        String data = getMetadata(vsURL);

        String startKTag = "<strong>";
        String endKTag = "</strong>";
        String endVTags[] = { "<br>", "</font>" };

        String step = "general";

        try {
            MCRAudioObject audio = null;

            StringTokenizer st = new StringTokenizer(data, "\n\r");
            while (st.hasMoreTokens()) {
                String line = st.nextToken().trim();

                String key = null;
                if (line.contains(startKTag) && line.contains(endKTag)) {
                    key = line.substring(line.indexOf(startKTag) + startKTag.length(), line.indexOf(endKTag)).trim();
                }

                if (key != null) {
                    String value = line.substring(line.indexOf(endKTag) + endKTag.length());
                    for (String endVTag : endVTags) {
                        if (value.contains(endVTag)) {
                            value = value.substring(0, value.indexOf(endVTag)).trim();
                            break;
                        }
                    }

                    if (key.startsWith("Stream:")) {
                        if (key.equals("Stream:"))
                            step = "general";
                        else if ("video stream".equals(value.toLowerCase()))
                            step = "video";
                        else if ("avi stream".equals(value.toLowerCase()))
                            step = "video";
                        else if ("audio stream".equals(value.toLowerCase()))
                            step = "audio";

                        if ("video".equals(step)) {
                            if (media.type == null) {
                                media.type = MCRMediaObject.MediaType.VIDEO;
                                media = (MCRVideoObject) media.clone();
                            } else if (media.type == MCRMediaObject.MediaType.AUDIO) {
                                audio = (MCRAudioObject) media.clone();
                                media = (MCRVideoObject) media.clone();
                                media.type = MCRMediaObject.MediaType.VIDEO;
                                audio.parentContainer = media;
                            }
                        } else if ("audio".equals(step)) {
                            if (media.type == null) {
                                media.type = MCRMediaObject.MediaType.AUDIO;
                                media = (MCRAudioObject) media.clone();
                                audio = (MCRAudioObject) media;
                            } else if (media.type == MCRMediaObject.MediaType.VIDEO) {
                                audio = new MCRAudioObject(media);
                                audio.duration = media.duration;
                                ((MCRVideoObject) media).audioCodes.add(audio);
                            }
                        }
                    }

                    LOGGER.debug(key + " " + value);

                    if ("general".equals(step)) {
                        if ("MIME type:".equals(key)) {
                            if (value.startsWith("video"))
                                step = "video";
                            else if (value.startsWith("audio"))
                                step = "audio";

                            if ("video".equals(step)) {
                                if (media.type == null) {
                                    media.type = MCRMediaObject.MediaType.VIDEO;
                                    media = (MCRVideoObject) media.clone();
                                } else if (media.type == MCRMediaObject.MediaType.AUDIO) {
                                    audio = (MCRAudioObject) media.clone();
                                    media = (MCRVideoObject) media.clone();
                                    media.type = MCRMediaObject.MediaType.VIDEO;
                                    audio.parentContainer = media;
                                }
                            } else if ("audio".equals(step)) {
                                if (media.type == null) {
                                    media.type = MCRMediaObject.MediaType.AUDIO;
                                    media.mimeType = value;
                                    media = (MCRAudioObject) media.clone();
                                    audio = (MCRAudioObject) media;
                                } else if (media.type == MCRMediaObject.MediaType.VIDEO) {
                                    audio = new MCRAudioObject(media);
                                    audio.duration = media.duration;
                                    ((MCRVideoObject) media).audioCodes.add(audio);
                                }
                            }
                        } else if (key.equals("Stream:")) {
                            media.formatFull = (value.contains(" -") ? value.substring(0, value.indexOf(" -")) : value);
                            if (media.formatFull.contains(" Stream"))
                                media.formatFull = media.format = media.formatFull.substring(0,
                                    media.formatFull.indexOf(" Stream"));
                        } else if ("File Name:".equals(key))
                            media.fileName = value;
                        else if ("File Size:".equals(key))
                            media.fileSize = Long
                                .parseLong((value.substring(0, value.indexOf(" "))).replaceAll(",", ""));
                        else if ("Duration:".equals(key)) {
                            StringTokenizer st1 = new StringTokenizer(value, ":.");
                            int durationMinutes = Integer.parseInt(st1.nextToken());
                            int durationSeconds = Integer.parseInt(st1.nextToken());
                            int durationMilSeconds = Integer.parseInt(st1.nextToken());

                            media.duration = durationMinutes * 3600000 + durationSeconds * 60000 + durationMilSeconds;
                        } else if ("Avg Bit Rate:".equals(key)) {
                            media.bitRate = Math.round(1024 * Float.valueOf(value.substring(0, value.indexOf(" "))));
                        } else if (key.equals("Title:") || key.equals("Author:") || key.equals("Copyright:")) {

                            if (media.tags == null)
                                media.tags = new MCRMediaTagObject();

                            //Container Infos
                            if (key.equals("Title:") && value.length() != 0)
                                media.tags.title = value;
                            else if (key.equals("Author:") && value.length() != 0)
                                media.tags.performer = value;
                            else if (key.equals("Copyright:") && value.length() != 0)
                                media.tags.comment = value;
                        }
                    } else if ("video".equals(step)) {
                        if ("MIME type:".equals(key)) {
                            if (media.mimeType == null)
                                media.mimeType = value;
                        } else if ("Avg Stream Bit Rate:".equals(key)) {
                            ((MCRVideoObject) media).streamBitRate = Math.round(1024 * Float
                                .valueOf(value.substring(0, value.indexOf(" "))));
                        } else if ("Dimensions:".equals(key)) {
                            StringTokenizer st1 = new StringTokenizer(value, "x");
                            ((MCRVideoObject) media).width = Integer.parseInt(st1.nextToken());
                            ((MCRVideoObject) media).height = Integer.parseInt(st1.nextToken());
                        } else if ("Encoded Frame Rate:".equals(key)) {
                            StringTokenizer st1 = new StringTokenizer(value.substring(0, value.indexOf("fps")), " ,");

                            while (st1.hasMoreTokens()) {
                                float fvalue = Float.valueOf(st1.nextToken());
                                ((MCRVideoObject) media).frameRate = Math.max(((MCRVideoObject) media).frameRate,
                                    fvalue);
                            }
                        } else if ("Video Codec:".equals(key)) {
                            if (value.contains("(")) {
                                ((MCRVideoObject) media).subFormatFull = value.substring(value.indexOf("(") + 1,
                                    value.indexOf(")"));
                                if (((MCRVideoObject) media).subFormatFull.contains(" ")) {
                                    StringTokenizer st1 = new StringTokenizer(((MCRVideoObject) media).subFormatFull,
                                        " ");
                                    ((MCRVideoObject) media).subFormat = st1.nextToken();
                                    ((MCRVideoObject) media).subFormatVersion = st1.nextToken();
                                }
                            }
                        }
                    } else if ("audio".equals(step)) {
                        if ("MIME type:".equals(key)) {
                            if (value.startsWith("video")) {
                                step = "video";
                                media = new MCRVideoObject();
                                media.format = audio.format;
                                media.formatFull = audio.formatFull;
                                media.fileName = audio.fileName;
                                media.fileSize = audio.fileSize;
                                media.duration = audio.duration;
                                media.bitRate = audio.bitRate;
                                media.mimeType = value;

                                ((MCRVideoObject) media).audioCodes.add(audio);
                                audio.parentContainer = media;
                            } else if (audio.mimeType == null)
                                audio.mimeType = value;
                        } else if ("Avg Stream Bit Rate:".equals(key)) {
                            audio.streamBitRate = Math
                                .round(1024 * Float.valueOf(value.substring(0, value.indexOf(" "))));
                        } else if ("Max Stream Bit Rate:".equals(key) && audio.streamBitRate == 0) {
                            audio.streamBitRate = Math
                                .round(1024 * Float.valueOf(value.substring(0, value.indexOf(" "))));
                        } else if ("Audio Codec:".equals(key)) {
                            if (value.contains("(")) {
                                audio.subFormatFull = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
                                if (audio.subFormatFull.contains(" ")) {
                                    StringTokenizer st1 = new StringTokenizer(audio.subFormatFull, " ");
                                    audio.subFormat = st1.nextToken();
                                    audio.subFormatVersion = st1.nextToken();
                                }

                                if (value.contains("Khz")) {
                                    audio.samplingRate = Integer
                                        .parseInt(value.substring(value.indexOf(")") + 1, value.indexOf("Khz"))
                                            .trim());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new Exception(e.getMessage());
        }

        return media;
    }

    /**
     * Helper method that reads all data from an URLConnection input stream and
     * forwards it to the given output stream.
     * 
     * @param connection
     *            the URLConnection to get the InputStream from
     * @param out
     *            the OutputStream to write the bytes to
     */
    protected void forwardData(URLConnection connection, OutputStream out) throws IOException {
        try (InputStream in = connection.getInputStream()) {
            IOUtils.copy(in, out);
            out.close();
        }
    }

    /**
     * Helper method that creates a URLConnection to a given URL and wraps
     * possible IOException or MalformedURLExceptions
     * 
     * @param url
     *            the URL to connect to
     */
    protected URLConnection getConnection(String url) throws MCRPersistenceException {
        try {
            return new URL(url).openConnection();
        } catch (MalformedURLException exc) {
            String msg = "Malformed Audio/Video Store URL: " + url;
            throw new MCRConfigurationException(msg, exc);
        } catch (IOException exc) {
            String msg = "Could not get connection to Audio/Video Store URL: " + url;
            throw new MCRPersistenceException(msg, exc);
        }
    }

    /**
     * Helper method that connects to the given URL and returns the response as
     * a String
     * 
     * @param url
     *            the URL to connect to
     * @return the response content as a String
     */
    protected String getMetadata(String url) throws MCRPersistenceException {
        try {
            URLConnection connection = getConnection(url);
            connection.setConnectTimeout(getConnectTimeout());
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            forwardData(connection, out);

            return new String(out.toByteArray());
        } catch (IOException exc) {
            String msg = "Could not get metadata from Audio/Video Store URL: " + url;
            throw new MCRPersistenceException(msg, exc);
        }
    }

    protected static int getConnectTimeout() {
        return MCRConfiguration.instance().getInt("MCR.Media.ConnectTimeout", 1000);
    }

    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCROldFile file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL(MCROldFile file) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL", null);

        return baseMetadata == null ? null : baseMetadata + file.getStorageID();
    }

    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCROldFile file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL(org.mycore.datamodel.ifs.MCRFile file) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL");

        return baseMetadata == null ? null : baseMetadata + file.getStorageID();
    }

    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCRFileReader file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL(MCRFileReader file) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL");

        return baseMetadata == null ? null : baseMetadata + file.getStorageID();
    }
}
