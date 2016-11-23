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
