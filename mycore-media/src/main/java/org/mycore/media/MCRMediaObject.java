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

package org.mycore.media;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

import org.jdom2.Element;

/**
 * This Class is the Superclass of all other classes. Is holds some 
 * methods for format some properties.
 *  
 * @author René Adler (Eagle)
 * 
 */
public class MCRMediaObject implements Cloneable {
    public enum MediaType {
        VIDEO, AUDIO, IMAGE, TEXT
    }

    protected MediaType type;

    protected String folderName;

    protected String fileName;

    protected long fileSize;

    protected String mimeType;

    protected String format;

    protected String formatFull;

    protected long duration;

    protected int bitRate;

    protected String encoderStr;

    protected MCRMediaTagObject tags;

    protected boolean XMLwithoutFileInfo = true;

    public MCRMediaObject() {
        tags = null;
    }

    public MediaType getType() {
        return type;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFormat() {
        return format;
    }

    public String getFormatFull() {
        return formatFull;
    }

    public long getDuration() {
        return duration;
    }

    public int getBitRate() {
        return bitRate;
    }

    public String getEncoder() {
        return encoderStr;
    }

    /**
     * Returns the hours part of the duration of this asset
     * 
     * @return the hours part of the duration of this asset
     */
    public int getDurationHours() {
        return (int) (duration / 1000) / 3600;
    }

    /**
     * Returns the minutes part of the duration of this asset
     * 
     * @return the minutes part of the duration of this asset
     */
    public int getDurationMinutes() {
        return (int) ((duration / 1000) - getDurationHours() * 3600) / 60;
    }

    /**
     * Returns the seconds part of the duration of this asset
     * 
     * @return the seconds part of the duration of this asset
     */
    public int getDurationSeconds() {
        return (int) ((duration / 1000) - getDurationHours() * 3600 - getDurationMinutes() * 60) / 60;
    }

    /**
     * Returns the streaming bitrate formatted as a String, e. g. "1.3 MBit" or
     * "300 kBit".
     * 
     * @return the streaming bitrate formatted as a String
     */
    public String getBitRateFormatted() {
        if (bitRate > 1024 * 1024) {
            double b = Math.round(bitRate / 10485.76) / 100.0;
            return new DecimalFormat("##0.##").format(b) + " MBit";
        }
        double b = Math.round(bitRate / 102.4) / 10.0;
        return new DecimalFormat("##0.#").format(b) + " kBit";
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
        StringBuilder sb = new StringBuilder();

        if (getDurationHours() > 0) {
            sb.append(getDurationHours());
            sb.append(" ").append(hourLabel).append(" ");
            sb.append(getDurationMinutes());
            sb.append(" ").append(minutesLabel);
        } else if (getDurationMinutes() > 0) {
            sb.append(getDurationMinutes());
            sb.append(" ").append(minutesLabel).append(" ");
            sb.append(getDurationSeconds());
            sb.append(" ").append(secondsLabel);
        } else {
            sb.append(getDurationSeconds());
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
        StringBuilder sb = new StringBuilder();
        sb.append(formatter.format(getDurationHours()));
        sb.append(":");
        sb.append(formatter.format(getDurationMinutes()));
        sb.append(":");
        sb.append(formatter.format(getDurationSeconds()));

        return sb.toString();
    }

    /**
     * Returns the maximal seek position of the MediaObject if supported.
     * 
     * @return 0 if not supported
     */
    public long getMaxSeekPosition() {
        return 0;
    }

    /**
     * Has media thumbnail support?
     * 
     * @return true or false
     */
    public boolean hasThumbnailSupport() {
        return false;
    }

    /**
     * Returns an ByteArray with the thumbnail in PNG format.
     * 
     * @return the Image in PNG format
     */
    public synchronized byte[] getThumbnail(MCRMediaObject media, long seek, int maxWidth, int maxHeight,
        boolean keepAspect) throws Exception {
        return null;
    }

    @Override
    public String toString() {
        String out = fileName + " (" + fileSize + ")\n";
        String _fill = "";
        for (int c = 0; c < out.length(); c++)
            _fill += "-";
        out += _fill + "\n";
        out += (type != null ? "Main type        : " + type.toString() + "\n" : "");
        out += "mimeType         : " + mimeType + "\n";
        out += "Format           : " + format + (formatFull != null ? " (" + formatFull + ")" : "") + "\n";
        out += "Duration         : " + duration + "\n";
        out += "Overall BitRate  : " + bitRate + "\n";
        out += (encoderStr != null ? "Encoder        : " + encoderStr + "\n" : "");

        if (tags != null)
            out += tags.toString();

        return out;
    }

    /**
     * Output metadata as XML.
     * 
     * @return a JDOM Element with data of the MediaObject 
     */
    public Element toXML() {
        Element xml = new Element("media");

        createElement(xml, "@type", (type != null ? type.toString().toLowerCase() : ""));
        createElement(xml, "@mimeType", mimeType);
        createElement(xml, "@containerType", format);
        createElement(xml, "@containerDescription", formatFull);
        createElement(xml, "@bitrate", String.valueOf(bitRate));
        createElement(xml, "@duration", String.valueOf(duration));

        if (!XMLwithoutFileInfo) {
            Element file = new Element("file");
            file.addContent(fileName);
            createElement(file, "@size", String.valueOf(fileSize));
            createElement(file, "@path", folderName);
            xml.addContent(file);
        }

        createElement(xml, "encoder", encoderStr);

        if (tags != null)
            xml.addContent(tags.toXML());

        return xml;
    }

    public static MCRMediaObject buildFromXML(Element xml) {
        MCRMediaObject media = null;

        String type = xml.getAttributeValue("type");

        if ("video".equals(type)) {
            media = MCRVideoObject.buildFromXML(xml);
        } else if ("audio".equals(type)) {
            media = MCRAudioObject.buildFromXML(xml);
        } else {
            MCRMediaObject _media = new MCRMediaObject();

            _media.mimeType = getXMLValue(xml, "@mimeType");
            _media.format = getXMLValue(xml, "@containerType");
            _media.formatFull = getXMLValue(xml, "@containerDescription");
            _media.bitRate = Integer.parseInt(getXMLValue(xml, "@bitrate", "0"));
            _media.duration = Integer.parseInt(getXMLValue(xml, "@duration", "0"));

            _media.fileSize = Long.parseLong(getXMLValue(xml, "file/@size", "0"));
            _media.folderName = getXMLValue(xml, "file/@path");
            _media.fileName = getXMLValue(xml, "file");

            _media.encoderStr = getXMLValue(xml, "encoder");
        }

        if (xml.getChild("tags") != null) {
            ((MCRMediaObject) media).tags = MCRMediaTagObject.buildFromXML(xml.getChild("tags"));
        }

        return media;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Object cloned = super.clone();
        if (cloned instanceof MCRMediaObject) {
            if (type == MediaType.VIDEO) {
                MCRVideoObject clone = new MCRVideoObject();

                clone.folderName = ((MCRMediaObject) cloned).folderName;
                clone.fileName = ((MCRMediaObject) cloned).fileName;
                clone.fileSize = ((MCRMediaObject) cloned).fileSize;
                clone.mimeType = ((MCRMediaObject) cloned).mimeType;

                clone.format = ((MCRMediaObject) cloned).format;
                clone.formatFull = ((MCRMediaObject) cloned).formatFull;

                clone.duration = ((MCRMediaObject) cloned).duration;
                clone.bitRate = ((MCRMediaObject) cloned).bitRate;
                clone.encoderStr = ((MCRMediaObject) cloned).encoderStr;

                if (tags != null)
                    clone.tags = (MCRMediaTagObject) ((MCRMediaObject) cloned).tags.clone();

                cloned = clone;
            } else if (type == MediaType.AUDIO) {
                MCRAudioObject clone = new MCRAudioObject();

                clone.folderName = ((MCRMediaObject) cloned).folderName;
                clone.fileName = ((MCRMediaObject) cloned).fileName;
                clone.fileSize = ((MCRMediaObject) cloned).fileSize;
                clone.mimeType = ((MCRMediaObject) cloned).mimeType;

                clone.format = ((MCRMediaObject) cloned).format;
                clone.formatFull = ((MCRMediaObject) cloned).formatFull;

                clone.duration = ((MCRMediaObject) cloned).duration;
                clone.bitRate = ((MCRMediaObject) cloned).bitRate;
                clone.encoderStr = ((MCRMediaObject) cloned).encoderStr;

                if (tags != null)
                    clone.tags = (MCRMediaTagObject) ((MCRMediaObject) cloned).tags.clone();

                cloned = clone;
            } else if (type == MediaType.IMAGE) {
                MCRImageObject clone = new MCRImageObject();

                clone.folderName = ((MCRMediaObject) cloned).folderName;
                clone.fileName = ((MCRMediaObject) cloned).fileName;
                clone.fileSize = ((MCRMediaObject) cloned).fileSize;
                clone.mimeType = ((MCRMediaObject) cloned).mimeType;

                clone.format = ((MCRMediaObject) cloned).format;
                clone.formatFull = ((MCRMediaObject) cloned).formatFull;

                clone.encoderStr = ((MCRMediaObject) cloned).encoderStr;

                if (tags != null)
                    clone.tags = (MCRMediaTagObject) ((MCRMediaObject) cloned).tags.clone();

                cloned = clone;
            }
        }

        return cloned;
    }

    protected static void createElement(Element xml, String tagName, String value) {
        if (tagName != null && value != null) {
            tagName = (tagName.startsWith("/") ? tagName.substring(1) : tagName);

            StringTokenizer st = new StringTokenizer(tagName, "/");
            if (st.hasMoreTokens() && st.countTokens() > 1) {
                String tagPart = st.nextToken().trim();

                Element elm = xml.getChild(tagPart);
                if (elm == null)
                    elm = new Element(tagPart);

                createElement(elm, tagName.replace(tagPart, ""), value);
            } else if (tagName.startsWith("@")) {
                xml.setAttribute(tagName.substring(1), value);
            } else {
                Element elm = new Element(tagName);
                elm.addContent(value);

                xml.addContent(elm);
            }
        }
    }

    protected static String getXMLValue(Element xml, String tagName) {
        return getXMLValue(xml, tagName, null);
    }

    protected static String getXMLValue(Element xml, String tagName, String defaultValue) {
        String ret = defaultValue;
        if (tagName != null) {
            tagName = (tagName.startsWith("/") ? tagName.substring(1) : tagName);

            StringTokenizer st = new StringTokenizer(tagName, "/");
            if (st.hasMoreTokens() && st.countTokens() > 1) {
                String tagPart = st.nextToken().trim();

                Element elm = xml.getChild(tagPart);
                if (elm != null)
                    ret = getXMLValue(elm, tagName.replace(tagPart, ""), defaultValue);
            } else if (tagName.startsWith("@")) {
                ret = (xml.getAttributeValue(tagName.substring(1)) != null ? xml.getAttributeValue(tagName.substring(1))
                    : defaultValue);
            } else {
                ret = xml.getChildTextTrim(tagName);
                if (ret == null || ret.length() == 0)
                    ret = defaultValue;
            }
        }

        return ret;
    }
}
