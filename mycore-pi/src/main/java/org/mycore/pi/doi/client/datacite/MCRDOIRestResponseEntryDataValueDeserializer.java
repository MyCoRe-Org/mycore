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

package org.mycore.pi.doi.client.datacite;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class MCRDOIRestResponseEntryDataValueDeserializer
    implements JsonDeserializer<MCRDataciteRestResponseEntryData> {
    @Override
    public MCRDataciteRestResponseEntryData deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject dataObject = jsonElement.getAsJsonObject();

        String format = dataObject.get("format").getAsJsonPrimitive().getAsString();
        JsonElement value = dataObject.get("value");

        MCRDataciteRestResponseEntryDataValue dcValue = switch (format) {
            case "string" -> new MCRDataciteRestResponseEntryDataStringValue(value.getAsJsonPrimitive().getAsString());
            case "base64" -> new MCRDataciteRestResponseEntryDataBase64Value(value.getAsJsonPrimitive().getAsString());
            //hex, admin, vlist and site currently not supported
            default -> new MCRDataciteRestResponseEntryDataValue();
        };

        return new MCRDataciteRestResponseEntryData(format, dcValue);
    }
}
