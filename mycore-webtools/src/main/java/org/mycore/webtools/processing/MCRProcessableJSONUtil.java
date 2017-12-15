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

package org.mycore.webtools.processing;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.mycore.common.MCRJSONManager;
import org.mycore.common.processing.MCRProcessable;

/**
 * Utility methods for processable API.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRProcessableJSONUtil {

    /**
     * Converts a processable to json.
     * 
     * @param processable the processable to convert
     * @return json object of the processable
     */
    public static JsonObject toJSON(MCRProcessable processable) {
        JsonObject processableJSON = new JsonObject();
        processableJSON.addProperty("name", processable.getName());
        processableJSON.addProperty("user", processable.getUserId());
        processableJSON.addProperty("status", processable.getStatus().toString());
        processableJSON.addProperty("createTime", processable.getCreateTime().toEpochMilli());

        if (!processable.isCreated()) {
            processableJSON.addProperty("startTime", processable.getStartTime().toEpochMilli());
            if (processable.isDone()) {
                processableJSON.addProperty("endTime", processable.getEndTime().toEpochMilli());
                processableJSON.addProperty("took", processable.took().toMillis());
            }
        }
        if (processable.isFailed()) {
            Gson gson = MCRJSONManager.instance().getGsonBuilder().create();
            processableJSON.addProperty("error", gson.toJson(processable.getError(), Throwable.class));
        }
        if (processable.getProgress() != null) {
            processableJSON.addProperty("progress", processable.getProgress());
        }
        if (processable.getProgressText() != null) {
            processableJSON.addProperty("progressText", processable.getProgressText());
        }
        JsonObject propertiesJSON = new JsonObject();
        processable.getProperties().forEach((k, v) -> propertiesJSON.addProperty(k, v.toString()));
        processableJSON.add("properties", propertiesJSON);
        return processableJSON;
    }

    /**
     * Converts a map of properties to a json representation.
     * 
     * @param properties the properties to convert
     * @return a json object containing the properties
     */
    public static JsonObject toJSON(Map<String, Object> properties) {
        Gson gson = MCRJSONManager.instance().createGson();
        return gson.toJsonTree(properties).getAsJsonObject();
    }

    /**
     * Converts a object to a json.
     * 
     * @param value the value to convert
     * @return a json element
     */
    public static JsonElement toJSON(Object value) {
        Gson gson = MCRJSONManager.instance().createGson();
        return gson.toJsonTree(value);
    }

}
