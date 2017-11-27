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

package org.mycore.media.video;

import org.apache.logging.log4j.LogManager;
import org.mycore.media.MCRMediaSourceType;

public class MCRMediaSource {
    private String uri;

    private MCRMediaSourceType type;

    public MCRMediaSource(String file, MCRMediaSourceType type) {
        LogManager.getLogger().info("uri : {}", file);
        this.uri = file;
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public MCRMediaSourceType getType() {
        return type;
    }
}
