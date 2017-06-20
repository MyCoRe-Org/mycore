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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.content.MCRFileContent;
import org.mycore.frontend.cli.MCRExternalProcess;

/**
 * This Class represents the technical metadata of an Videostream.
 * 
 * @author René Adler (Eagle)
 *
 */
public class MCRVideoObject extends MCRMediaObject {
    protected String subFormat;

    protected String subFormatVersion;

    protected String subFormatProfile;

    protected String subFormatFull;

    protected String codecID;

    protected String codec;

    protected String codecFull;

    protected String codecURL;

    protected String aspectRatio;

    protected int width;

    protected int height;

    protected int resolution;

    protected int streamBitRate;

    protected float frameRate;

    protected ArrayList<MCRAudioObject> audioCodes = new ArrayList<MCRAudioObject>();

    public MCRVideoObject() {
        type = MCRMediaObject.MediaType.VIDEO;
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

    public String getAspectRation() {
        return aspectRatio;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getResolution() {
        return resolution;
    }

    public int getStreamBitRate() {
        return streamBitRate;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public long getMaxSeekPosition() {
        return duration;
    }

    public boolean hasThumbnailSupport() {
        return true;
    }

    public int[] getScaledSize(int maxWidth, int maxHeight, boolean keepAspect) {
        int scaledSize[] = new int[2];

        maxWidth = (maxWidth == 0 ? 256 : maxWidth);
        maxHeight = (maxHeight == 0 ? 256 : maxHeight);

        if (keepAspect) {
            float scaleFactor = (maxWidth >= maxHeight ? Float.intBitsToFloat(maxWidth) / Float.intBitsToFloat(width)
                : Float.intBitsToFloat(maxHeight) / Float.intBitsToFloat(height));

            scaledSize[0] = Math.round(width * scaleFactor);
            scaledSize[1] = Math.round(height * scaleFactor);
        } else {
            scaledSize[0] = maxWidth;
            scaledSize[1] = maxHeight;
        }

        return scaledSize;
    }

    /**
     * Take a Snapshot from VideoObject.
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
     */
    public synchronized byte[] getThumbnail(MCRMediaObject media, long seek, int maxWidth, int maxHeight,
        boolean keepAspect) throws Exception {
        MCRExternalProcess ep;
        try {
            File tmpFile = File.createTempFile("MCRVideoObject", ".png");
            tmpFile.deleteOnExit();

            int[] scaledSize = ((MCRVideoObject) media).getScaledSize(maxWidth, maxHeight, keepAspect);

            String args[] = new String[13];
            args[0] = "ffmpeg";
            args[1] = "-ss";
            args[2] = "" + seek;
            args[3] = "-i";
            args[4] = media.folderName + media.fileName;
            args[5] = "-an";
            args[6] = "-s";
            args[7] = scaledSize[0] + "x" + scaledSize[1];
            args[8] = "-vframes";
            args[9] = "1";
            args[10] = "-f";
            args[11] = "image2";
            args[12] = tmpFile.getAbsolutePath();
            ep = new MCRExternalProcess(args);

            if (ep.run() != 0)
                throw new Exception("An error occures on run getThumb.\n" + ep.getErrors());
            else {
                return new MCRFileContent(tmpFile).asByteArray();
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public String toString() {
        String out = fileName + " (" + fileSize + ")\n";
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
        out += "Duration         : " + duration + "\n";
        out += "Dimention        : " + width + "x" + height + " @ " + frameRate + "\n";
        out += (aspectRatio != null ? "Aspect Ratio     : " + aspectRatio + "\n" : "");
        out += "BitRate          : " + streamBitRate + "\n";
        out += (encoderStr != null ? "Encoder          : " + encoderStr + "\n" : "");

        if (tags != null)
            out += tags.toString();

        for (MCRAudioObject audio : audioCodes)
            out += audio.toString();

        return out;
    }

    /**
     * Output metadata as XML.
     */
    @Override
    public Element toXML() {
        Element xml = new Element("media");

        xml.setAttribute("type", "video");
        createElement(xml, "@mimeType", mimeType);
        createElement(xml, "@containerType", format);
        createElement(xml, "@containerDescription", formatFull);
        createElement(xml, "@overallBitrate", String.valueOf(bitRate));

        if (!XMLwithoutFileInfo) {
            Element file = new Element("file");
            file.addContent(fileName);
            createElement(file, "@size", String.valueOf(fileSize));
            createElement(file, "@path", folderName);
            xml.addContent(file);
        }

        Element stream = new Element("videostream");
        createElement(stream, "@bitrate", String.valueOf(streamBitRate));
        createElement(stream, "@framerate", String.valueOf(frameRate));
        createElement(stream, "@duration", String.valueOf(duration));

        Element dimensions = new Element("dimensions");
        createElement(dimensions, "@width", String.valueOf(width));
        createElement(dimensions, "@height", String.valueOf(height));
        createElement(dimensions, "@aspectRatio", aspectRatio);
        stream.addContent(dimensions);

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

        xml.addContent(stream);

        Element audioStreams = new Element("audioStreams");

        for (MCRAudioObject audio : audioCodes)
            audioStreams.addContent(audio.toXML(false));
        xml.addContent(audioStreams);

        if (tags != null)
            xml.addContent(tags.toXML());

        return xml;
    }

    @SuppressWarnings("unchecked")
    public static MCRVideoObject buildFromXML(Element xml) {
        MCRVideoObject video = new MCRVideoObject();

        video.mimeType = getXMLValue(xml, "@mimeType");
        video.format = getXMLValue(xml, "@containerType");
        video.formatFull = getXMLValue(xml, "@containerDescription");
        video.bitRate = Integer.parseInt(getXMLValue(xml, "@overallBitrate"));

        video.fileSize = Long.parseLong(getXMLValue(xml, "file/@size", "0"));
        video.folderName = getXMLValue(xml, "file/@path");
        video.fileName = getXMLValue(xml, "file");

        video.streamBitRate = Integer.parseInt(getXMLValue(xml, "videostream/@bitrate", "0"));
        video.frameRate = Float.parseFloat(getXMLValue(xml, "videostream/@framerate", "0.0"));
        video.duration = Long.parseLong(getXMLValue(xml, "videostream/@duration", "0"));

        video.width = Integer.parseInt(getXMLValue(xml, "videostream/dimensions/@width", "0"));
        video.height = Integer.parseInt(getXMLValue(xml, "videostream/dimensions/@height", "0"));
        video.aspectRatio = getXMLValue(xml, "videostream/dimensions/@aspectRatio");

        video.subFormat = getXMLValue(xml, "videostream/format/@short");
        video.subFormatVersion = getXMLValue(xml, "videostream/format/@version");
        video.subFormatProfile = getXMLValue(xml, "videostream/format/@profile");
        video.subFormatFull = getXMLValue(xml, "videostream/format");

        video.codecID = getXMLValue(xml, "videostream/codec/@id");
        video.codec = getXMLValue(xml, "videostream/codec/@type");
        video.codecURL = getXMLValue(xml, "videostream/codec/@url");
        video.encoderStr = getXMLValue(xml, "videostream/codec/encoder");
        video.codecFull = getXMLValue(xml, "videostream/codec");

        if (xml.getChild("audioStreams") != null) {
            List<Element> audioStreams = xml.getChild("audioStreams").getChildren("audiostream");
            for (Element audioStream : audioStreams) {
                Element astream = (Element) audioStream;

                MCRAudioObject audio = new MCRAudioObject(video);
                MCRAudioObject.addStreamFromXML(audio, astream);

                video.audioCodes.add(audio);
            }
        }

        return video;
    }
}
