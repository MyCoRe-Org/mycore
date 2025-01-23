/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.webtools.processing.socket.impl;

/**
 * @author Sebastian Hofmann
 */
class MCRUpdateCollectionPropertyMessage extends MCRWebSocketMessage {

    public Integer id;

    public String propertyName;

    public Object propertyValue;

    MCRUpdateCollectionPropertyMessage(Integer id, String propertyName, Object propertyValue) {
        super(MCRMessageType.UPDATE_COLLECTION_PROPERTY);
        this.id = id;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

}
