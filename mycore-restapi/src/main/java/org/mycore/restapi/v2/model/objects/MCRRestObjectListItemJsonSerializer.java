package org.mycore.restapi.v2.model.objects;

import java.io.IOException;

import org.mycore.restapi.v2.model.MCRRestLink;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class MCRRestObjectListItemJsonSerializer extends StdSerializer<MCRRestObjectListItem> {

    private static final long serialVersionUID = 1L;

    public MCRRestObjectListItemJsonSerializer() {
        super(MCRRestObjectListItem.class, false);
    }

    @Override
    public void serialize(MCRRestObjectListItem value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("id", value.getId());
        jgen.writeStringField("modified", value.getModified().toString());
        jgen.writeObjectFieldStart("links");
        for (MCRRestLink rl : value.getLinks()) {
            jgen.writeStringField(rl.getRel(), rl.getUrl());
        }
        jgen.writeEndObject();
        jgen.writeEndObject();
    }
}
