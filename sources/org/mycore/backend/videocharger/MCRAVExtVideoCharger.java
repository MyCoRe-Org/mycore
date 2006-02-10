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

package org.mycore.backend.videocharger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFileReader;

/**
 * This class implements the AudioVideoExtender functions for IBM VideoCharger
 * 7.1 and 8.1 instances. It reads technical metadata about stored assets by
 * parsing the vslist cgi responses and gets a player starter file using the
 * iscpfsel cgi. The parameters can be configured in mycore.properties:
 * 
 * <code>
 MCR.IFS.AVExtender.<StoreID>.VSListURL    URL of vslist cgi
 MCR.IFS.AVExtender.<StoreID>.ISCPFSelURL  URL of iscpfsel cgi
 MCR.IFS.AVExtender.<StoreID>.PlayerURL    Download URL for VideoCharger Player
 * </code>
 * 
 * This class also provides a method to list all assets in a VideoCharger store.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRAVExtVideoCharger extends MCRAudioVideoExtender {
    protected static MCRConfiguration CONFIG;

    public MCRAVExtVideoCharger() {
        if (CONFIG == null) {
            CONFIG = MCRConfiguration.instance();
        }
    }

    public void readConfig(String storeID) {
        String prefix = "MCR.IFS.AVExtender." + storeID + ".";

        baseMetadata = CONFIG.getString(prefix + "VSListURL");
        basePlayerStarter = CONFIG.getString(prefix + "ISCPFSelURL");
        playerDownloadURL = CONFIG.getString(prefix + "PlayerURL");
    }

    public void init(MCRFileReader file) throws MCRPersistenceException {
        super.init(file);

        readConfig(file.getStoreID());

        String assetID;

        try {
            assetID = URLEncoder.encode(file.getStorageID(), CONFIG.getString("MCR.request_charencoding", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new MCRPersistenceException("MCR.request_charencoding property does not contain a valid encoding:", e);
        }

        String data1 = getMetadata(baseMetadata + "?" + assetID);
        String data2 = getMetadata(basePlayerStarter + "?VIDEOID=" + assetID);

        URLConnection con = getConnection(basePlayerStarter + "?VIDEOID=" + assetID);
        playerStarterCT = con.getContentType();

        try {
            String sSize = getBetween("filesize64=", "\n", data2, "0,0");
            String sBitRate = getBetween("bitRate=", "\n", data1, "0");
            String sFrameRate = getBetween("frameRate=", "\n", data1, "0");
            String sDuration = getBetween("duration=", "\n", data1, "0:0:0:0");
            String sType = getBetween("type=", "\n", data1, "");

            bitRate = Integer.parseInt(sBitRate);
            frameRate = Double.valueOf(sFrameRate).doubleValue();
            mediaType = (frameRate > 0);

            StringTokenizer st1 = new StringTokenizer(sSize, ",");
            long sizeHi = Long.parseLong(st1.nextToken());
            long sizeLo = Long.parseLong(st1.nextToken());
            size = (sizeHi << 32) + sizeLo;

            StringTokenizer st2 = new StringTokenizer(sDuration, ":");
            durationHours = Integer.parseInt(st2.nextToken());
            durationMinutes = Integer.parseInt(st2.nextToken());
            durationSeconds = Integer.parseInt(st2.nextToken());

            if (sType.indexOf("MPEG1") >= 0) {
                if (frameRate > 0) {
                    contentTypeID = "mpegvid";
                } else if (file.getExtension().toLowerCase().equals("mp3")) {
                    contentTypeID = "mp3";
                } else {
                    contentTypeID = "mpegaud";
                }
            } else if (sType.indexOf("MPEG2") >= 0) {
                contentTypeID = "mpegvid2";
            } else if (sType.indexOf("MOV") >= 0) {
                contentTypeID = "qtvid";
            } else if (sType.indexOf("WAV") >= 0) {
                contentTypeID = "wav";
            } else if (sType.indexOf("AVI") >= 0) {
                contentTypeID = "avi";
            }
        } catch (Exception exc) {
            String msg = "Error parsing metadata from VideoCharger asset " + file.getStorageID();
            throw new MCRPersistenceException(msg, exc);
        }
    }

    public void getPlayerStarterTo(OutputStream out, String startPos, String stopPos) throws MCRPersistenceException {
        try {
            StringBuffer cgi = new StringBuffer(basePlayerStarter);
            cgi.append("?VIDEOID=");
            cgi.append(URLEncoder.encode(file.getStorageID(), CONFIG.getString("MCR.request_charencoding", "UTF-8")));

            if (startPos != null) {
                cgi.append("&StartPos=").append(startPos);
            }

            if (stopPos != null) {
                cgi.append("&StopPos=").append(stopPos);
            }

            URLConnection connection = getConnection(cgi.toString());
            forwardData(connection, out);
        } catch (IOException exc) {
            String msg = "Could not send VideoCharger player starter";
            throw new MCRPersistenceException(msg, exc);
        }
    }

    /**
     * Lists all assets that are stored in this VideoCharger store.
     * 
     * @return a String array containing all asset IDs
     */
    public String[] listAssets() throws MCRPersistenceException {
        String vslist = getMetadata(baseMetadata);
        StringTokenizer st = new StringTokenizer(vslist, "\n");
        String[] list = new String[st.countTokens()];

        for (int i = 0; i < list.length; i++)
            list[i] = st.nextToken();

        return list;
    }
}
