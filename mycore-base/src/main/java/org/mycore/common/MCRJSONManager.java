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
