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

/**
 * This Class represents the basic metadata of an Image.
 * 
 * @author René Adler (Eagle)
 *
 */
public class MCRImageObject extends MCRMediaObject {
    protected String subFormat;

    protected String subFormatFull;

    protected String codecID;

    protected String codec;

    protected String codecFull;

    protected String codecURL;

    protected int width;

    protected int height;

    protected int resolution;

    public MCRImageObject() {
        type = MCRMediaObject.MediaType.IMAGE;
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getResolution() {
        return resolution;
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
        out += "Dimention        : " + width + "x" + height + " @ " + resolution + "\n";
        out += (encoderStr != null ? "Encoder          : " + encoderStr + "\n" : "");

        if (tags != null)
            out += tags.toString();

        return out;
    }
}
