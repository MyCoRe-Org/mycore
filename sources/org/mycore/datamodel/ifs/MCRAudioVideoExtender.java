/*
 * $RCSfile$
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

package org.mycore.datamodel.ifs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;

/**
 * For MCRFiles that contain streaming audio/video, instances of this class
 * provide methods to get technical metadata like bitrate, framerate, duration,
 * size etc. and to start a player to stream the asset to a browser.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public abstract class MCRAudioVideoExtender {
    /** Constant for media type = video */
    public final static boolean VIDEO = true;

    /** Constant for media type = audio */
    public final static boolean AUDIO = false;

    /** The bitrate of the asset in number of bits per second */
    protected int bitRate = 0;

    /** The framerate of the asset in number of frames per second */
    protected double frameRate = 0;

    /** The media type of the asset, either AUDIO or VIDEO */
    protected boolean mediaType = VIDEO;

    /** The hours part of the duration of the asset */
    protected int durationHours = 0;

    /** The minutes part of the duration of the asset */
    protected int durationMinutes = 0;

    /** The seconds part of the duration of the asset */
    protected int durationSeconds = 0;

    /** The size of the asset in bytes */
    protected long size = 0;

    /** The content type of the asset */
    protected String contentTypeID = "unknown";

    /** The URL where clients can download a player for the asset */
    protected String playerDownloadURL = "";

    /** The MIME type a servlet has to send with the player starter */
    protected String playerStarterCT = "";

    /** The base URL where to get a metafile that starts a player in browser */
    protected String basePlayerStarter = "";

    /** The base URL of a cgi that provides technical metadata about the asset */
    protected String baseMetadata = "";

    /** The asset file this extender belongs to */
    protected MCRFileReader file;

    /**
     * Creates a new MCRAudioVideoExtender. The instance has to be initialized
     * by invoking init() before it can be used.
     */
    public MCRAudioVideoExtender() {
    }

    /**
     * Initializes this AudioVideoExtender and gets technical metadata from the
     * server that holds the streaming asset. Subclasses must override this
     * method!
     * 
     * @param file
     *            the MCRFile that this extender belongs to
     */
    public void init(MCRFileReader file) throws MCRException {
        this.file = file;
    }

    /**
     * Returns the maximum number of bits per seconds when asset is streamed
     * 
     * @return the maximum number of bits per seconds when asset is streamed
     */
    public int getBitRate() {
        return bitRate;
    }

    /**
     * Returns the streaming bitrate formatted as a String, e. g. "1.3 MBit" or
     * "300 kBit".
     * 
     * @return the streaming bitrate formatted as a String
     */
    public String getBitRateFormatted() {
        if (bitRate > (1024 * 1024)) {
            double b = Math.round(bitRate / 10485.76) / 100.0;
            return new DecimalFormat("##0.##").format(b) + " MBit";
        }
        double b = Math.round(bitRate / 102.4) / 10.0;
        return new DecimalFormat("##0.#").format(b) + " kBit";
    }

    /**
     * Returns the maximum number of frames per second for a streaming video
     * asset
     * 
     * @return the maximum number of frames per second for a streaming video
     *         asset
     */
    public double getFrameRate() {
        return frameRate;
    }

    /**
     * Returns the framerate formatted as a String, e. g. "25.0"
     * 
     * @return the framerate formatted as a String, e. g. "25.0"
     */
    public String getFrameRateFormatted() {
        // double r = (double)( Math.round( frameRate * 10.0 ) ) / 10.0;
        return new DecimalFormat("##.#").format(frameRate);
    }

    /**
     * Returns the media type, either AUDIO od VIDEO for this asset
     * 
     * @return the media type, compare to the boolean constants in this class
     */
    public boolean getMediaType() {
        return mediaType;
    }

    /**
     * Returns true, if this asset is an audio asset.
     * 
     * @return true, if this asset is an audio asset.
     */
    public boolean isAudio() {
        return (mediaType == AUDIO);
    }

    /**
     * Returns true, if this asset is a video asset.
     * 
     * @return true, if this asset is a video asset.
     */
    public boolean isVideo() {
        return (mediaType == VIDEO);
    }

    /**
     * Returns the hours part of the duration of this asset
     * 
     * @return the hours part of the duration of this asset
     */
    public int getDurationHours() {
        return durationHours;
    }

    /**
     * Returns the minutes part of the duration of this asset
     * 
     * @return the minutes part of the duration of this asset
     */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /**
     * Returns the seconds part of the duration of this asset
     * 
     * @return the seconds part of the duration of this asset
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Returns the duration of this asset, formatted as a String for output. For
     * example, <tt>getDurationFormatted( "Std.", "Min.", "Sek." )</tt> will
     * return the String "1 Std. 15 Min." for an asset that is one hour and 15
     * minutes long. If duration is less than one hour, only minutes and seconds
     * go into the output string, otherwise hours and minutes are used.
     * 
     * @param hourLabel
     *            the label for the hours part of the duration
     * @param minutesLabel
     *            the label for the minutes part of the duration
     * @param secondsLabel
     *            the label for the seconds part of the duration
     * @return the duration of this asset, formatted as a String
     */
    public String getDurationFormatted(String hourLabel, String minutesLabel, String secondsLabel) {
        StringBuffer sb = new StringBuffer();

        if (durationHours > 0) {
            sb.append(durationHours);
            sb.append(" ").append(hourLabel).append(" ");
            sb.append(durationMinutes);
            sb.append(" ").append(minutesLabel);
        } else if (durationMinutes > 0) {
            sb.append(durationMinutes);
            sb.append(" ").append(minutesLabel).append(" ");
            sb.append(durationSeconds);
            sb.append(" ").append(secondsLabel);
        } else {
            sb.append(durationSeconds);
            sb.append(" ").append(secondsLabel);
        }

        return sb.toString();
    }

    /**
     * Returns the duration of the asset, formatted as a timcode, e. g.
     * "01:15:00" for an asset thats duration is one hour and 15 minutes.
     * 
     * @return the duration foramatted as a timecode like "hh:mm:ss"
     */
    public String getDurationTimecode() {
        DecimalFormat formatter = new DecimalFormat("00");
        StringBuffer sb = new StringBuffer();
        sb.append(formatter.format(durationHours));
        sb.append(":");
        sb.append(formatter.format(durationMinutes));
        sb.append(":");
        sb.append(formatter.format(durationSeconds));

        return sb.toString();
    }

    /**
     * Returns the asset size in number of bytes.
     * 
     * @return the asset size in number of bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the asset size, formatted as a String.
     * 
     * @return the asset size, formatted as a String
     */
    public String getSizeFormatted() {
        return MCRFilesystemNode.getSizeFormatted(size);
    }

    /**
     * Returns the ID of the content type of this asset
     * 
     * @return the ID of the content type of this asset
     */
    public String getContentTypeID() {
        return contentTypeID;
    }

    /**
     * Returns the URL where clients can download a player for this asset
     * 
     * @return the URL where clients can download a player for this asset
     */
    public String getPlayerDownloadURL() {
        return playerDownloadURL;
    }

    /**
     * Writes a metafile that starts a streaming player for this asset to an
     * OutputStream, e. g. a ServletOutputStream. The browser then streams the
     * asset. The client may provide a start and stop position to play only a
     * certain part of the asset.
     * 
     * @param out
     *            the OutputStream to write the player starter to
     * @param startPos
     *            the optional start position in the format "hh:mm:ss"
     * @param stopPos
     *            the optional stop position in the format "hh:mm:ss"
     */
    public abstract void getPlayerStarterTo(OutputStream out, String startPos, String stopPos) throws MCRPersistenceException;

    /**
     * Returns the MIME type a servlet has to set in the HTTP response that
     * delivers the player starter metafile to the browser
     * 
     * @return the MIME type of the player starter metafile
     */
    public String getPlayerStarterContentType() {
        return playerStarterCT;
    }

    /**
     * Returns a string representation of this extender's data, useful for
     * debugging.
     * 
     * @return a String containing useful information about this extender's data
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Media Type      : ");
        sb.append(isVideo() ? "Video\n" : "Audio\n");
        sb.append("Bitrate         : ").append(getBitRateFormatted()).append("/sec.");
        sb.append(" (").append(getBitRate()).append(")\n");

        if (isVideo()) {
            sb.append("Framerate       : ").append(getFrameRateFormatted()).append(" fps");
            sb.append(" (").append(getFrameRate()).append(")\n");
        }

        sb.append("Duration        : ").append(getDurationFormatted("h", "min", "sec"));
        sb.append(" (").append(getDurationTimecode()).append(")\n");
        sb.append("Size            : ").append(getSizeFormatted());
        sb.append(" (").append(getSize()).append(")\n");
        sb.append("Content Type    : ").append(getContentTypeID()).append("\n");
        sb.append("Player Download : ").append(playerDownloadURL);

        return sb.toString();
    }

    /**
     * Helper method to get a substring that lies between a prefix and a suffix
     * string. If either prefix or suffix are not found in the string, the
     * defaultValue is returned. This helper method is used by subclasses to
     * parse metadata that is read from the server.
     * 
     * @param prefix
     *            the string before the substring
     * @param suffix
     *            the string after the substring
     * @param data
     *            the string to search through
     * @param defaultValue
     *            the default to return when no match is found
     * @return the substring between prefix and suffix
     */
    protected String getBetween(String prefix, String suffix, String data, String defaultValue) {
        int from = data.indexOf(prefix);

        if (from == -1) {
            return defaultValue;
        }

        from += prefix.length();

        int to = data.indexOf(suffix, from);

        if (to == -1) {
            return defaultValue;
        }

        return data.substring(from, to).trim();
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
        InputStream in = connection.getInputStream();
        MCRUtils.copyStream(in, out);
        out.close();
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
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            forwardData(connection, out);

            return new String(out.toByteArray());
        } catch (IOException exc) {
            String msg = "Could not get metadata from Audio/Video Store URL: " + url;
            throw new MCRPersistenceException(msg, exc);
        }
    }
}
