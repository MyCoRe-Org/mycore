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

package org.mycore.media;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public boolean isFileSupported(org.mycore.datamodel.ifs.MCRFile file) {
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
            LOGGER.info("parse {}...", file.getName());

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
                                switch (key) {
                                    case "Format":
                                        media.format = ovalue;
                                        break;
                                    case "Format/Info":
                                        media.formatFull = ovalue;
                                        break;
                                    case "InternetMediaType":
                                        media.mimeType = value;
                                        break;
                                    case "Duration":
                                        media.duration = Integer.parseInt(value);
                                        break;
                                    case "OverallBitRate":
                                        media.bitRate = Integer.parseInt(value);
                                        break;
                                    case "Encoded_Library":
                                        media.encoderStr = ovalue;
                                        break;
                                    case "Title":
                                    case "Album":
                                    case "Track":
                                    case "Track/Position":
                                    case "Performer":
                                    case "Genre":
                                    case "Recorded_Date":
                                    case "Comment":

                                        if (media.tags == null)
                                            media.tags = new MCRMediaTagObject();

                                        //Container Infos
                                        switch (key) {
                                            case "Title":
                                                media.tags.title = ovalue;
                                                break;
                                            case "Album":
                                                media.tags.album = ovalue;
                                                break;
                                            case "Track":
                                                media.tags.trackName = ovalue;
                                                break;
                                            case "Track/Position":
                                                media.tags.trackPosition = Integer.parseInt(value);
                                                break;
                                            case "Performer":
                                                media.tags.performer = ovalue;
                                                break;
                                            case "Genre":
                                                media.tags.genre = ovalue;
                                                break;
                                            case "Recorded_Date":
                                                media.tags.recordDate = ovalue;
                                                break;
                                            case "Comment":
                                                media.tags.comment = ovalue;
                                                break;
                                        }
                                        break;
                                }
                            } else if (step == MediaInfo.StreamKind.Video) {
                                switch (key) {
                                    case "Format":
                                        ((MCRVideoObject) media).subFormat = ovalue;
                                        ((MCRVideoObject) media).subFormatFull = ovalue;
                                        break;
                                    case "Format/Info":
                                        ((MCRVideoObject) media).subFormatFull = ovalue;
                                        break;
                                    case "Format_Version":
                                        ((MCRVideoObject) media).subFormatVersion = ovalue;
                                        ((MCRVideoObject) media).subFormatFull += " " + ovalue;
                                        break;
                                    case "Format_Profile":
                                        ((MCRVideoObject) media).subFormatProfile = ovalue;
                                        ((MCRVideoObject) media).subFormatFull += " " + ovalue;
                                        break;
                                    case "CodecID":
                                        ((MCRVideoObject) media).codecID = value;
                                        break;
                                    case "Codec":
                                        ((MCRVideoObject) media).codec = ovalue;
                                        break;
                                    case "Codec/Info":
                                        ((MCRVideoObject) media).codecFull = ovalue;
                                        break;
                                    case "CodecID/Url":
                                    case "Codec/Url":
                                        ((MCRVideoObject) media).codecURL = ovalue;
                                        break;
                                    case "Encoded_Library/String":
                                        media.encoderStr = ovalue;
                                        break;
                                    case "BitRate":
                                    case "BitRate_Nominal":
                                        ((MCRVideoObject) media).streamBitRate = Integer.parseInt(value);
                                        break;
                                    case "Width":
                                        ((MCRVideoObject) media).width = Integer.parseInt(value);
                                        break;
                                    case "Height":
                                        ((MCRVideoObject) media).height = Integer.parseInt(value);
                                        break;
                                    case "Resolution":
                                        ((MCRVideoObject) media).resolution = Integer.parseInt(value);
                                        break;
                                    case "DisplayAspectRatio/String":
                                        ((MCRVideoObject) media).aspectRatio = value;
                                        break;
                                    case "FrameRate":
                                        ((MCRVideoObject) media).frameRate = Float.parseFloat(value);
                                        break;
                                }
                            } else if (step == MediaInfo.StreamKind.Audio && audio != null) {
                                switch (key) {
                                    case "Format":
                                        audio.subFormat = ovalue;
                                        audio.subFormatFull = ovalue;
                                        break;
                                    case "Format/Info":
                                        audio.subFormatFull = ovalue;
                                        break;
                                    case "Format_Version":
                                        audio.subFormatVersion = ovalue;
                                        audio.subFormatFull += " " + ovalue;
                                        break;
                                    case "Format_Profile":
                                        audio.subFormatProfile = ovalue;
                                        audio.subFormatFull += " " + ovalue;
                                        break;
                                    case "CodecID":
                                        audio.codecID = value;
                                        break;
                                    case "Codec":
                                        audio.codec = ovalue;
                                        break;
                                    case "Codec/Info":
                                        audio.codecFull = ovalue;
                                        break;
                                    case "CodecID/Url":
                                    case "Codec/Url":
                                        audio.codecURL = ovalue;
                                        break;
                                    case "Encoded_Library/String":
                                        audio.encoderStr = ovalue;
                                        break;
                                    case "Duration":
                                        audio.duration = Integer.parseInt(value);
                                        break;
                                    case "BitRate":
                                        audio.streamBitRate = Integer.parseInt(value);
                                        break;
                                    case "BitRate_Mode":
                                        audio.streamBitRateMode = ovalue;
                                        break;
                                    case "Channel(s)":
                                        audio.channels = Integer.parseInt(value);
                                        break;
                                    case "SamplingRate":
                                        audio.samplingRate = Integer.parseInt(value);
                                        break;
                                    case "Language":
                                        audio.language = value;
                                        break;
                                }
                            } else if (step == MediaInfo.StreamKind.Image) {
                                //TODO: use Sanselan to get metadata like EXIF
                                switch (key) {
                                    case "Format":
                                        ((MCRImageObject) media).subFormat = ovalue;
                                        break;
                                    case "Format_Settings_Matrix":
                                        ((MCRImageObject) media).subFormatFull = ovalue;
                                        break;
                                    case "CodecID":
                                        ((MCRImageObject) media).codecID = value;
                                        break;
                                    case "Codec":
                                        ((MCRImageObject) media).codec = ovalue;
                                        break;
                                    case "Codec/Info":
                                        ((MCRImageObject) media).codecFull = ovalue;
                                        break;
                                    case "CodecID/Url":
                                    case "Codec/Url":
                                        ((MCRImageObject) media).codecURL = ovalue;
                                        break;
                                    case "Encoded_Library/String":
                                        media.encoderStr = ovalue;
                                        break;
                                    case "Width":
                                        ((MCRImageObject) media).width = Integer.parseInt(value);
                                        break;
                                    case "Height":
                                        ((MCRImageObject) media).height = Integer.parseInt(value);
                                        break;
                                    case "Resolution":
                                        ((MCRImageObject) media).resolution = Integer.parseInt(value);
                                        break;
                                }
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

    public synchronized MCRMediaObject parse(org.mycore.datamodel.ifs.MCRFile file) throws Exception {
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
