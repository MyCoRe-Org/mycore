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

package org.mycore.datamodel.ifs.extractors;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.farng.mp3.MP3File;
import org.jdom.Element;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;

/**
 * Extracts ID3v1, ID3v2 and LyricsV3 metadata from MP3 files.
 * Uses the org.farng.** metadata extraction library.
 * It supports ID3v1, ID3v1.1, Lyrics3v1, Lyrics3v2, 
 * ID3v2.2, ID3v2.3, and ID3v2.4 tags. * 
 * See http://sourceforge.net/projects/javamusictag/ for details.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRDataExtractorMP3 extends MCRDataExtractor {

    /**
     * Helper table for metadata extraction. Keys are element names in resulting
     * XML output. Values are method names of the extraction API used.
     */
    private Properties p = new Properties();

    public MCRDataExtractorMP3() {
        p.put("album", "getAlbum");
        p.put("albumTitle", "getAlbumTitle");
        p.put("artist", "getArtist");
        p.put("authorComposer", "getAuthorComposer");
        p.put("comment", "getComment");
        p.put("leadArtist", "getLeadArtist");
        p.put("songComment", "getSongComment");
        p.put("songGenre", "getSongGenre");
        p.put("songComment", "getSongComment");
        p.put("songLyric", "getSongLyric");
        p.put("songTitle", "getSongTitle");
        p.put("title", "getTitle");
        p.put("trackNumberOnAlbum", "getTrackNumberOnAlbum");
        p.put("year", "getYear");
        p.put("yearReleased", "getYearReleased");
    }

    /** Table of supported file content types */
    private final static HashMap map = new HashMap();

    protected HashMap getSupportedContentTypes() {
        if (map.isEmpty()) {
            MCRFileContentType ct = MCRFileContentTypeFactory.getType("mp3");
            map.put(ct.getID(), ct);
        }
        return map;
    }

    /**
     * The last ID used for created temp file
     */
    private static String lastID;

    /**
     * Builds a unique ID used as the file name in temp file creation.
     * 
     * @return a unique ID
     */
    private static synchronized String buildTempID() {
        String newID = Long.toString(System.currentTimeMillis(), 36);
        if (newID.equals(lastID)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            return buildTempID();
        }
        lastID = newID;
        return newID;
    }

    protected void extractData(Element container, InputStream in) throws Exception {
        File f = File.createTempFile(buildTempID(), "mp3");
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        MCRUtils.copyStream(in, out);
        in.close();
        out.close();

        MP3File mp3 = new MP3File(f);
        addDataValue(container, "bitRate", String.valueOf(mp3.getBitRate()));
        addDataValue(container, "frequency", String.valueOf(mp3.getFrequency()));

        try {
            if (mp3.hasID3v1Tag())
                extractTagData(container, "ID3v1", mp3.getID3v1Tag());
        } catch (Exception ignored) {
        }
        try {
            if (mp3.hasID3v2Tag())
                extractTagData(container, "ID3v2", mp3.getID3v2Tag());
        } catch (Exception ignored) {
        }
        try {
            if (mp3.hasLyrics3Tag())
                extractTagData(container, "Lyrics3", mp3.getLyrics3Tag());
        } catch (Exception ignored) {
        }

        try {
            f.delete();
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * Extracts metadata from a single ID3v1, ID3v2 or Lyrics3 object
     */
    private void extractTagData(Element root, String parentName, Object tag) {
        Element parent = new Element(parentName);

        for (Enumeration keys = p.keys(); keys.hasMoreElements();) {
            String key = (String) (keys.nextElement());
            String method = p.getProperty(key);
            try {
                Method m = tag.getClass().getMethod(method, null);
                String value = (String) (m.invoke(tag, null));
                addDataValue(parent, key, value);
            } catch (Exception ex) {
                continue;
            }
        }

        if (parent.getChildren().size() > 0)
            root.addContent(parent);
    }

    /**
     * Adds extracted metadata value to the resulting XML output, if it is not
     * null or empty.
     */
    private void addDataValue(Element parent, String name, String value) {
        if ((value != null) && (value.trim().length() > 0) && (!value.trim().equals("0")) && (!value.trim().equals("0.0")))
            parent.addContent(new Element(name).setText(value.trim()));
    }

    /**
     * Test application that outputs extracted metadata for a given local file.
     * 
     * @param args
     *            the path to a locally stored MP3 file
     */
    public static void main(String[] args) {
        new MCRDataExtractorMP3().testLocalFile(args[0]);
    }
}
