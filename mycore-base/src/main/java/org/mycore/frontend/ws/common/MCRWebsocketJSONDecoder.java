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

package org.mycore.frontend.ws.common;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Decodes a json string to a gson object.
 * 
 * @author Matthias Eichner
 */
public class MCRWebsocketJSONDecoder implements Decoder.Text<JsonObject> {

    @Override
    public JsonObject decode(String request) throws DecodeException {
        return new JsonParser().parse(request).getAsJsonObject();
    }

    @Override
    public void init(EndpointConfig config) {
        // nothing to initialize
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }

    @Override
    public boolean willDecode(String s) {
        return s != null;
    }

}
