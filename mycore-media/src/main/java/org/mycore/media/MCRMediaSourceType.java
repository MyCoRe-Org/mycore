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

import org.mycore.common.MCRException;

public enum MCRMediaSourceType {
    mp4, rtmp_stream, hls_stream, dash_stream;

    public String getSimpleType() {
        String str = toString();
        int pos = str.indexOf('_');
        return pos > 0 ? str.substring(0, pos) : str;
    }

    public String getMimeType() {
        return switch (this) {
            case mp4 -> "video/mp4";
            case hls_stream -> "application/x-mpegURL";
            case rtmp_stream -> "rtmp/mp4";
            case dash_stream -> "application/dash+xml";
            default -> throw new MCRException(this + " has no MIME type defined.");
        };
    }
}
