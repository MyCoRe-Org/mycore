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

package org.mycore.backend.realhelix;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the AudioVideoExtender functions for Real Server 8 and
 * Helix Universal Streaming Server 9 instances. It reads technical metadata
 * about stored assets by parsing the Real Server's "View Source" responses and
 * gets a player starter file using the "/ramgen/" mount point. The parameters
 * can be configured in mycore.properties:
 * 
 * <pre>
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.RamGenBaseURL      URL of ramgen mount point
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.AsxGenBaseURL      URL of asxgen mount point
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.ViewSourceBaseURL  URL of view source function
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.RealPlayerURL      Download URL for RealOne Player
 *   MCR.IFS.AVExtender.&lt;StoreID&gt;.MediaPlayerURL     Download URL for Microsoft Player
 * </pre>
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRAVExtRealHelix extends MCRAudioVideoExtender {

    /** The logger */
    private final static Logger LOGGER = LogManager.getLogger(MCRAVExtRealHelix.class);

    public MCRAVExtRealHelix() {
    }

    @Override
    public void init(MCRFileReader file) throws MCRPersistenceException {
        super.init(file);

        MCRConfiguration config = MCRConfiguration.instance();
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";

        baseMetadata = config.getString(prefix + "ViewSourceBaseURL");

        try {
            String data = getMetadata(baseMetadata + file.getStorageID());

            String sSize = getBetween("File Size:</strong>", "Bytes", data, "0");
            String sBitRate = getBetween("Bit Rate:</strong>", "Kbps", data, "0.0");
            String sFrameRate = getBetween("Frame Rate: </strong>", "fps", data, "0.0");
            String sDuration = getBetween("Duration:</strong>", "<br>", data, "0:0.0");
            String sType = getBetween("Stream:</strong>", "<br>", data, "");

            bitRate = Math.round(1024 * Float.valueOf(sBitRate));

            StringTokenizer st1 = new StringTokenizer(sFrameRate, " ,");

            while (st1.hasMoreTokens()) {
                double value = Double.valueOf(st1.nextToken());
                frameRate = Math.max(frameRate, value);
            }

            hasVideo = frameRate > 0;

            StringTokenizer st2 = new StringTokenizer(sDuration, ":.");
            durationMinutes = Integer.parseInt(st2.nextToken());
            durationSeconds = Integer.parseInt(st2.nextToken());

            if (Integer.parseInt(st2.nextToken()) > 499) {
                durationSeconds += 1;

                if (durationSeconds > 59) {
                    durationMinutes += 1;
                    durationSeconds = 0;
                }
            }

            StringTokenizer st3 = new StringTokenizer(sSize, ",");
            StringBuilder sb = new StringBuilder();

            while (st3.hasMoreTokens()) {
                sb.append(st3.nextToken());
            }

            size = Long.parseLong(sb.toString());

            durationHours = durationMinutes / 60;
            durationMinutes = durationMinutes - durationHours * 60;

            if (sType.contains("MPEG Layer 3")) {
                contentTypeID = "mp3";
                hasVideo = false;
            } else if (sType.contains("3GPP")) {
                contentTypeID = "3gp";
                hasVideo = true;
            } else if (sType.contains("MPEG4")) {
                contentTypeID = "mpeg4";
                hasVideo = true;
            } else if (sType.contains("MPEG")) {
                contentTypeID = "mpegvid";
                hasVideo = true;
            } else if (sType.contains("RealVideo")) {
                contentTypeID = "realvid";
            } else if (sType.contains("RealAudio")) {
                contentTypeID = "realaud";
            } else if (sType.contains("Wave File")) {
                contentTypeID = "wav";
                hasVideo = false;
            } else // should be one of "wma" "wmv" "asf"
            {
                contentTypeID = file.getContentTypeID();

                hasVideo = !contentTypeID.equals("wma");
            }

            if (" wma wmv asf asx ".contains(" " + contentTypeID + " ")) {
                basePlayerStarter = config.getString(prefix + "AsxGenBaseURL");
                playerDownloadURL = config.getString(prefix + "MediaPlayerURL");
            } else {
                basePlayerStarter = config.getString(prefix + "RamGenBaseURL");
                playerDownloadURL = config.getString(prefix + "RealPlayerURL");
            }

            URLConnection con = getConnection(basePlayerStarter + file.getStorageID());
            playerStarterCT = con.getContentType();
        } catch (Exception exc) {
            String msg = "Error parsing metadata from Real Server ViewSource: " + file.getStorageID();
            LOGGER.warn(msg, exc);
        }
    }

    private String getURL(String startPos, String stopPos) {
        StringBuilder cgi = new StringBuilder(basePlayerStarter);
        cgi.append(file.getStorageID());

        if (startPos != null || stopPos != null) {
            cgi.append("?");
        }

        if (startPos != null) {
            cgi.append("start=").append(startPos);
        }

        if (startPos != null && stopPos != null) {
            cgi.append("&");
        }

        if (stopPos != null) {
            cgi.append("end=").append(stopPos);
        }

        String url = cgi.toString();
        return url;
    }

    @Override
    public MCRContent getPlayerStarter(String startPos, String stopPos) throws IOException {
        MCRURLContent content = new MCRURLContent(new URL(getURL(startPos, stopPos)));
        content.setMimeType(getPlayerStarterContentType());
        if (file instanceof MCRFile) {
            content.setName(((MCRFile) file).getName() + startPos + "-" + stopPos);
        }
        return content;
    }
}
