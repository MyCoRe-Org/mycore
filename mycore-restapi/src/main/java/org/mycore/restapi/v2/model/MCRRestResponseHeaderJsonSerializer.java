package org.mycore.restapi.v2.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class MCRRestResponseHeaderJsonSerializer extends StdSerializer<MCRRestResponseHeader> {

    private static final long serialVersionUID = 1L;

    public MCRRestResponseHeaderJsonSerializer() {
        super(MCRRestResponseHeader.class, false);
    }

    @Override
    public void serialize(MCRRestResponseHeader value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeNumberField("status", value.getStatus());
        jgen.writeObjectFieldStart("links");
        for (MCRRestLink rl : value.getLinks()) {
            jgen.writeStringField(rl.getRel(), rl.getUrl());
        }
        jgen.writeEndObject();
        jgen.writeEndObject();
    }
}
