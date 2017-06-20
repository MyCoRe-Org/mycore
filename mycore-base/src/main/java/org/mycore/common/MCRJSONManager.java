package org.mycore.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MCRJSONManager {
    private GsonBuilder gsonBuilder;

    private static MCRJSONManager instance;

    private MCRJSONManager() {
        gsonBuilder = new GsonBuilder();
    }

    public void registerAdapter(MCRJSONTypeAdapter<?> typeAdapter) {
        gsonBuilder.registerTypeAdapter(typeAdapter.bindTo(), typeAdapter);
    }

    public static MCRJSONManager instance() {
        if (instance == null) {
            instance = new MCRJSONManager();
        }
        return instance;
    }

    public GsonBuilder getGsonBuilder() {
        return gsonBuilder;
    }

    public Gson createGson() {
        return gsonBuilder.create();
    }
}
