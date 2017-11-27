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
