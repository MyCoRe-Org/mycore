package org.mycore.webtools.processing;

import org.mycore.common.MCRJSONManager;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableRegistry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Utility methods for processable API.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRProcessableJSONUtil {

    /**
     * Converts a registry to json. Returns a json array including
     * every {@link MCRProcessableCollection}.
     * 
     * @param registry the registry to convert
     * @return json array including processable collection
     */
    public static JsonArray toJSON(MCRProcessableRegistry registry) {
        JsonArray registryJSON = new JsonArray();
        registry.stream().map(MCRProcessableJSONUtil::toJSON).forEach(registryJSON::add);
        return registryJSON;
    }

    /**
     * Converts a processable collection to json.
     * 
     * @param collection the collection to convert
     * @return json object of the collection
     */
    public static JsonObject toJSON(MCRProcessableCollection collection) {
        JsonObject collectionJSON = new JsonObject();
        collectionJSON.addProperty("name", collection.getName());
        JsonArray processablesJSON = new JsonArray();
        collectionJSON.add("processables", processablesJSON);
        collection.stream().map(MCRProcessableJSONUtil::toJSON).forEach(processablesJSON::add);
        return collectionJSON;
    }

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
        return processableJSON;
    }

}
