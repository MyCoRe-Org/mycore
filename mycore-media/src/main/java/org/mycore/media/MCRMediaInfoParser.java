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
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.datamodel.ifs.MCROldFile;
import org.mycore.frontend.cli.MCRExternalProcess;

import com.sun.jna.Platform;

/**
 * This Class implements a parser of various files using the MediaInfo 
 * library.
 * <br>
 * To get this working you need some libraries like MediaInfo and ffmpeg.
 * <br>
 * On <b>Windows or Mac</b> you can cross your fingers all needed libraries came 
 * with the module.
 * <br>
 * If you using an <b>*nix system</b> follow the link and install MediaInfo Lib
 * and the Zen Lib.
 * <br>
 * You need also installed the ffmpeg libraries and the 
 * application for your system. 
 * 
 * @see <a href="http://mediainfo.sourceforge.net/">MediaInfo</a>
 * 
 * @author Ren\u00E9 Adler (Eagle)
 *
 */
@SuppressWarnings("deprecation")
public class MCRMediaInfoParser extends MCRMediaParser {
    private static final String[] supportedFileExts = { "mkv", "mka", "mks", "ogg", "ogm", "avi", "wav", "mpeg", "mpg",
        "vob", "mp4",
        "mpgv", "mpv", "m1v", "m2v", "mp2", "mp3", "asf", "wma", "wmv", "qt", "mov", "rm", "rmvb", "ra", "ifo", "ac3",
        "dts", "aac",
        "ape", "mac", "flv", "f4v", "flac", "dat", "aiff", "aifc", "au", "iff", "paf", "sd2", "irca", "w64", "mat",
        "pvf", "xi", "sds",
        "avr" };

    private static final Logger LOGGER = LogManager.getLogger(MCRMediaInfoParser.class);

    private static final NativeLibExporter libExporter = NativeLibExporter.getInstance();

    private static MCRMediaInfoParser instance = new MCRMediaInfoParser();

    private MediaInfo MI;

    static {
        //export ffmpeg app to classpath
        try {
            if (Platform.isMac())
                libExporter.exportLibrary("lib/darwin/ffmpeg");
            else if (Platform.isWindows())
                libExporter.exportLibrary("lib/win32/ffmpeg.exe");
            else if (Platform.isLinux() && !isFFMpegInstalled())
                LOGGER.warn("Please install ffmpeg on your system.");
        } catch (Throwable e) {
            LOGGER.warn("Couldn't export ffmpeg to classpath!");
        }
    }

    public static MCRMediaInfoParser getInstance() {
        return instance;
    }

    private MCRMediaInfoParser() {
        MI = new MediaInfo();

        if (MI.isValid()) {
            MI.Option("Complete", "1");
            MI.Option("Language", "raw");
        }
    }

    /**
     * Checks if MediaInfo library is valid.
     * 
     * @return boolean if is vaild
     */
    public boolean isValid() {
        return MI.isValid();
    }

    /**
     * Close MediaInfo library. 
     */
    public void close() {
        try {
            MI.finalize();
        } catch (Throwable e) {
        }
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

    /**
     * Parse MediaInfo of the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse(File file) throws Exception {
        if (!file.exists())
            throw new IOException("File \"" + file.getName() + "\" doesn't exists!");

        MCRMediaObject media = new MCRMediaObject();

        if (libExporter.isValid() && MI.isValid() && MI.Open(file.getAbsolutePath()) > 0) {
            LOGGER.info("parse " + file.getName() + "...");

            try {
                String info = MI.Inform();
                MediaInfo.StreamKind step = MediaInfo.StreamKind.General;

                media.fileName = file.getName();
                media.fileSize = file.length();
                media.folderName = (file.getAbsolutePath()).replace(file.getName(), "");

                if (info != null) {
                    MCRAudioObject audio = null;
                    StringTokenizer st = new StringTokenizer(info, "\n\r");
                    while (st.hasMoreTokens()) {
                        String line = st.nextToken().trim();
                        LOGGER.debug(line);

                        if (line.equals("Video")) {
                            step = MediaInfo.StreamKind.Video;
                            if (media.type == null) {
                                media.type = MCRMediaObject.MediaType.VIDEO;
                                media = (MCRVideoObject) media.clone();
                            }
                        } else if (line.equals("Audio") || line.startsWith("Audio #")) {
                            step = MediaInfo.StreamKind.Audio;
                            if (media.type == null) {
                                media.type = MCRMediaObject.MediaType.AUDIO;
                                media = (MCRAudioObject) media.clone();
                                audio = (MCRAudioObject) media;
                            } else if (media.type == MCRMediaObject.MediaType.VIDEO) {
                                audio = new MCRAudioObject(media);
                                ((MCRVideoObject) media).audioCodes.add(audio);
                            }
                        } else if (line.equals("Text") || line.startsWith("Text #")) {
                            step = MediaInfo.StreamKind.Text;
                            if (media.type == null)
                                media.type = MCRMediaObject.MediaType.TEXT;
                        } else if (line.equals("Menu") || line.startsWith("Menu #")) {
                            step = MediaInfo.StreamKind.Menu;
                        } else if (line.equals("Chapters")) {
                            step = MediaInfo.StreamKind.Chapters;
                        } else if (line.equals("Image")) {
                            step = MediaInfo.StreamKind.Image;
                            if (media.type == null) {
                                media.type = MCRMediaObject.MediaType.IMAGE;
                                media = (MCRImageObject) media.clone();
                            }
                        }

                        int point = line.indexOf(":");
                        if (point > -1) {
                            String key = line.substring(0, point).trim();
                            String ovalue = line.substring(point + 1).trim();
                            String value = ovalue.toLowerCase();

                            if (step == MediaInfo.StreamKind.General) {
                                if (key.equals("Format"))
                                    media.format = ovalue;
                                else if (key.equals("Format/Info"))
                                    media.formatFull = ovalue;
                                else if (key.equals("InternetMediaType"))
                                    media.mimeType = value;
                                else if (key.equals("Duration"))
                                    media.duration = Integer.parseInt(value);
                                else if (key.equals("OverallBitRate"))
                                    media.bitRate = Integer.parseInt(value);
                                else if (key.equals("Encoded_Library"))
                                    media.encoderStr = ovalue;
                                else if (key.equals("Title") || key.equals("Album") || key.equals("Track")
                                    || key.equals("Track/Position")
                                    || key.equals("Performer") || key.equals("Genre") || key.equals("Recorded_Date")
                                    || key.equals("Comment")) {

                                    if (media.tags == null)
                                        media.tags = new MCRMediaTagObject();

                                    //Container Infos
                                    if (key.equals("Title"))
                                        media.tags.title = ovalue;
                                    else if (key.equals("Album"))
                                        media.tags.album = ovalue;
                                    else if (key.equals("Track"))
                                        media.tags.trackName = ovalue;
                                    else if (key.equals("Track/Position"))
                                        media.tags.trackPosition = Integer.parseInt(value);
                                    else if (key.equals("Performer"))
                                        media.tags.performer = ovalue;
                                    else if (key.equals("Genre"))
                                        media.tags.genre = ovalue;
                                    else if (key.equals("Recorded_Date"))
                                        media.tags.recordDate = ovalue;
                                    else if (key.equals("Comment"))
                                        media.tags.comment = ovalue;
                                }
                            } else if (step == MediaInfo.StreamKind.Video) {
                                if (key.equals("Format")) {
                                    ((MCRVideoObject) media).subFormat = ovalue;
                                    ((MCRVideoObject) media).subFormatFull = ovalue;
                                } else if (key.equals("Format/Info"))
                                    ((MCRVideoObject) media).subFormatFull = ovalue;
                                else if (key.equals("Format_Version")) {
                                    ((MCRVideoObject) media).subFormatVersion = ovalue;
                                    ((MCRVideoObject) media).subFormatFull += " " + ovalue;
                                } else if (key.equals("Format_Profile")) {
                                    ((MCRVideoObject) media).subFormatProfile = ovalue;
                                    ((MCRVideoObject) media).subFormatFull += " " + ovalue;
                                } else if (key.equals("CodecID"))
                                    ((MCRVideoObject) media).codecID = value;
                                else if (key.equals("Codec"))
                                    ((MCRVideoObject) media).codec = ovalue;
                                else if (key.equals("Codec/Info"))
                                    ((MCRVideoObject) media).codecFull = ovalue;
                                else if (key.equals("CodecID/Url") || key.equals("Codec/Url"))
                                    ((MCRVideoObject) media).codecURL = ovalue;
                                else if (key.equals("Encoded_Library/String"))
                                    media.encoderStr = ovalue;
                                else if (key.equals("BitRate") || key.equals("BitRate_Nominal"))
                                    ((MCRVideoObject) media).streamBitRate = Integer.parseInt(value);
                                else if (key.equals("Width"))
                                    ((MCRVideoObject) media).width = Integer.parseInt(value);
                                else if (key.equals("Height"))
                                    ((MCRVideoObject) media).height = Integer.parseInt(value);
                                else if (key.equals("Resolution"))
                                    ((MCRVideoObject) media).resolution = Integer.parseInt(value);
                                else if (key.equals("DisplayAspectRatio/String"))
                                    ((MCRVideoObject) media).aspectRatio = value;
                                else if (key.equals("FrameRate"))
                                    ((MCRVideoObject) media).frameRate = Float.parseFloat(value);
                            } else if (step == MediaInfo.StreamKind.Audio && audio != null) {
                                if (key.equals("Format")) {
                                    audio.subFormat = ovalue;
                                    audio.subFormatFull = ovalue;
                                } else if (key.equals("Format/Info"))
                                    audio.subFormatFull = ovalue;
                                else if (key.equals("Format_Version")) {
                                    audio.subFormatVersion = ovalue;
                                    audio.subFormatFull += " " + ovalue;
                                } else if (key.equals("Format_Profile")) {
                                    audio.subFormatProfile = ovalue;
                                    audio.subFormatFull += " " + ovalue;
                                } else if (key.equals("CodecID"))
                                    audio.codecID = value;
                                else if (key.equals("Codec"))
                                    audio.codec = ovalue;
                                else if (key.equals("Codec/Info"))
                                    audio.codecFull = ovalue;
                                else if (key.equals("CodecID/Url") || key.equals("Codec/Url"))
                                    audio.codecURL = ovalue;
                                else if (key.equals("Encoded_Library/String"))
                                    audio.encoderStr = ovalue;
                                else if (key.equals("Duration"))
                                    audio.duration = Integer.parseInt(value);
                                else if (key.equals("BitRate"))
                                    audio.streamBitRate = Integer.parseInt(value);
                                else if (key.equals("BitRate_Mode"))
                                    audio.streamBitRateMode = ovalue;
                                else if (key.equals("Channel(s)"))
                                    audio.channels = Integer.parseInt(value);
                                else if (key.equals("SamplingRate"))
                                    audio.samplingRate = Integer.parseInt(value);
                                else if (key.equals("Language"))
                                    audio.language = value;
                            } else if (step == MediaInfo.StreamKind.Image) {
                                //TODO: use Sanselan to get metadata like EXIF
                                if (key.equals("Format"))
                                    ((MCRImageObject) media).subFormat = ovalue;
                                else if (key.equals("Format_Settings_Matrix"))
                                    ((MCRImageObject) media).subFormatFull = ovalue;
                                else if (key.equals("CodecID"))
                                    ((MCRImageObject) media).codecID = value;
                                else if (key.equals("Codec"))
                                    ((MCRImageObject) media).codec = ovalue;
                                else if (key.equals("Codec/Info"))
                                    ((MCRImageObject) media).codecFull = ovalue;
                                else if (key.equals("CodecID/Url") || key.equals("Codec/Url"))
                                    ((MCRImageObject) media).codecURL = ovalue;
                                else if (key.equals("Encoded_Library/String"))
                                    media.encoderStr = ovalue;
                                else if (key.equals("Width"))
                                    ((MCRImageObject) media).width = Integer.parseInt(value);
                                else if (key.equals("Height"))
                                    ((MCRImageObject) media).height = Integer.parseInt(value);
                                else if (key.equals("Resolution"))
                                    ((MCRImageObject) media).resolution = Integer.parseInt(value);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                throw new Exception(e.getMessage());
            } finally {
                MI.Close();
            }
        } else {
            LOGGER.error("Couldn't initalize MediaInfo.");
            throw new Exception("Couldn't initalize MediaInfo.");
        }

        return media;
    }

    public synchronized MCRMediaObject parse(MCROldFile file) throws Exception {
        return parse(toFile(file));
    }

    public synchronized MCRMediaObject parse(org.mycore.datamodel.ifs.MCRFile file) throws Exception {
        return parse(toFile(file));
    }

    public synchronized MCRMediaObject parse(MCRFileReader file) throws Exception {
        return parse(toFile(file));
    }

    /**
     * Checks if ffmpeg is installed btw. exported to classpath.
     * 
     * @return boolean if is
     */
    private static boolean isFFMpegInstalled() {
        try {
            return new MCRExternalProcess("ffmpeg").run() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
