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

import org.jdom2.Element;

/**
 * This Class represents the technical metadata of an Audiostream.
 * 
 * @author René Adler (Eagle)
 *
 */
public class MCRAudioObject extends MCRMediaObject {
    protected Object parentContainer;

    protected String subFormat;

    protected String subFormatVersion;

    protected String subFormatProfile;

    protected String subFormatFull;

    protected String codecID;

    protected String codec;

    protected String codecFull;

    protected String codecURL;

    protected String streamBitRateMode;

    protected int streamBitRate;

    protected int channels;

    protected int samplingRate;

    protected String language;

    public MCRAudioObject() {
        type = MCRMediaObject.MediaType.AUDIO;
    }

    public MCRAudioObject(Object container) {
        type = MCRMediaObject.MediaType.AUDIO;
        parentContainer = container;
    }

    public String getSubFormat() {
        return subFormat;
    }

    public String getSubFormatFull() {
        return subFormatFull;
    }

    public String getCodecID() {
        return codecID;
    }

    public String getCodec() {
        return codec;
    }

    public String getCodecFull() {
        return codecFull;
    }

    public String getCodecURL() {
        return codecURL;
    }

    public int getStreamBitRate() {
        //currently only video can be a parent
        int calcBitRate = (parentContainer != null
            ? ((MCRVideoObject) parentContainer).bitRate - ((MCRVideoObject) parentContainer).streamBitRate : 0);

        return (streamBitRate != 0 ? streamBitRate : calcBitRate);
    }

    public String getStreamBitRateMode() {
        return streamBitRateMode;
    }

    public int getChannels() {
        return channels;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    @Override
    public String toString() {
        String out = "";

        if (parentContainer instanceof MCRVideoObject) {
            out += ((MCRVideoObject) parentContainer).fileName + " (" + ((MCRVideoObject) parentContainer).fileSize
                + ")\n";
        } else
            out += fileName + " (" + fileSize + ")\n";

        String _fill = "";
        for (int c = 0; c < out.length(); c++)
            _fill += "-";
        out += _fill + "\n";
        out += "Type             : " + type.toString() + "\n";
        out += (mimeType != null ? "mimeType         : " + mimeType + "\n" : "");
        out += (format != null
            ? "Container Format : " + format + (formatFull != null ? " (" + formatFull + ")" : "") + "\n" : "");
        out += "Format           : " + subFormat + (subFormatFull != null ? " (" + subFormatFull + ")" : "") + "\n";
        out += "Codec            : " + codec + (codecFull != null ? " - " + codecFull : "")
            + (codecURL != null ? " (" + codecURL + ")" : "") + "\n";
        out += (encoderStr != null ? "Encoder          : " + encoderStr + "\n" : "");
        out += "Duration         : " + duration + "\n";

        int calcBitRate = (parentContainer != null
            ? ((MCRVideoObject) parentContainer).bitRate - ((MCRVideoObject) parentContainer).streamBitRate : 0);
        if (streamBitRate != 0)
            out += "BitRate          : " + streamBitRate + (streamBitRateMode != null ? " @ " + streamBitRateMode : "")
                + "\n";
        else
            out += "BitRate (calc.)  : " + calcBitRate + (streamBitRateMode != null ? " @ " + streamBitRateMode : "")
                + "\n";

        out += "Channel(s)       : " + channels + "\n";
        out += "SamplingRate     : " + samplingRate + "\n";
        out += (language != null ? "Language         : " + language + "\n" : "");

        if (tags != null)
            out += tags.toString();

        return out;
    }

    /**
     * Output metadata as XML.
     */
    @Override
    public Element toXML() {
        return toXML(true);
    }

    /**
     * Output metadata as XML.
     * 
     * @param withRoot
     *                  complete output or only stream info
     */
    public Element toXML(boolean withRoot) {
        Element xml;

        Element stream = new Element("audiostream");
        createElement(stream, "@bitrate", String.valueOf(getStreamBitRate()));
        createElement(stream, "@mode", streamBitRateMode);
        createElement(stream, "@duration", String.valueOf(duration));
        createElement(stream, "@channels", String.valueOf(channels));
        createElement(stream, "@samplingrate", String.valueOf(samplingRate));
        createElement(stream, "@language", language);

        Element subformat = new Element("format");
        subformat.addContent(subFormatFull);
        createElement(subformat, "@short", subFormat);
        createElement(subformat, "@version", subFormatVersion);
        createElement(subformat, "@profile", subFormatProfile);
        stream.addContent(subformat);

        Element cod = new Element("codec");
        cod.addContent(codecFull);
        createElement(cod, "@id", codecID);
        createElement(cod, "@type", codec);
        createElement(cod, "@url", codecURL);

        createElement(cod, "encoder", encoderStr);

        stream.addContent(cod);

        if (withRoot) {
            xml = new Element("media");
            xml.setAttribute("type", "audio");
            createElement(xml, "@mimeType", mimeType);
            createElement(xml, "@containerType", format);
            createElement(xml, "@containerDescription", formatFull);
            createElement(xml, "@overallBitrate", String.valueOf(bitRate));

            Element contformat = new Element("format");
            if (format != null)
                contformat.setAttribute("type", format);
            if (formatFull != null)
                contformat.setAttribute("description", formatFull);

            if (!XMLwithoutFileInfo) {
                Element file = new Element("file");
                file.addContent(fileName);
                createElement(file, "@size", String.valueOf(fileSize));
                createElement(file, "@path", folderName);
                xml.addContent(file);
            }

            xml.addContent(stream);

            if (tags != null)
                xml.addContent(tags.toXML());
        } else
            xml = stream;

        return xml;
    }

    public static MCRAudioObject buildFromXML(Element xml) {
        MCRAudioObject audio = new MCRAudioObject();

        if ("media".equals(xml.getName())) {
            audio.mimeType = getXMLValue(xml, "@mimeType");
            audio.format = getXMLValue(xml, "@containerType");
            audio.formatFull = getXMLValue(xml, "@containerDescription");
            audio.bitRate = Integer.parseInt(getXMLValue(xml, "@overallBitrate"));

            audio.fileSize = Long.parseLong(getXMLValue(xml, "file/@size", "0"));
            audio.folderName = getXMLValue(xml, "file/@path");
            audio.fileName = getXMLValue(xml, "file");

            addStreamFromXML(audio, xml.getChild("audiostream"));
        } else if ("audiostream".equals(xml.getName())) {
            addStreamFromXML(audio, xml);
        }

        return audio;
    }

    protected static void addStreamFromXML(MCRAudioObject audio, Element xml) {
        audio.streamBitRate = Integer.parseInt(getXMLValue(xml, "@bitrate", "0"));
        audio.streamBitRateMode = getXMLValue(xml, "@mode");
        audio.duration = Long.parseLong(getXMLValue(xml, "@duration", "0"));
        audio.channels = Integer.parseInt(getXMLValue(xml, "@channels", "0"));
        audio.samplingRate = Integer.parseInt(getXMLValue(xml, "@samplingrate", "0"));
        audio.language = getXMLValue(xml, "@language");

        audio.subFormat = getXMLValue(xml, "format/@short");
        audio.subFormatVersion = getXMLValue(xml, "format/@version");
        audio.subFormatProfile = getXMLValue(xml, "format/@profile");
        audio.subFormatFull = getXMLValue(xml, "format");

        audio.codecID = getXMLValue(xml, "codec/@id");
        audio.codec = getXMLValue(xml, "codec/@type");
        audio.codecURL = getXMLValue(xml, "codec/@url");
        audio.encoderStr = getXMLValue(xml, "codec/encoder");
        audio.codecFull = getXMLValue(xml, "codec");
    }
}
