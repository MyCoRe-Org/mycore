package org.mycore.pi.doi.rest;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class MCRDOIRestResponseEntryDataValueDeserializer implements JsonDeserializer<MCRDOIRestResponseEntryData> {
    @Override
    public MCRDOIRestResponseEntryData deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject dataObject = jsonElement.getAsJsonObject();

        String format = dataObject.get("format").getAsJsonPrimitive().getAsString();
        JsonElement value = dataObject.get("value");

        switch (format) {
            case "string":
                return new MCRDOIRestResponseEntryData(format,
                    new MCRDOIRestResponseEntryDataStringValue(value.getAsJsonPrimitive().getAsString()));
            case "base64":
                return new MCRDOIRestResponseEntryData(format,
                    new MCRDOIRestResponseEntryDataBase64Value(value.getAsJsonPrimitive().getAsString()));
            case "hex":
            case "admin":
            case "vlist":
            case "site":
            default:
                // currently not supported
                return new MCRDOIRestResponseEntryData(format, new MCRDOIRestResponseEntryDataValue());
        }
    }
}
