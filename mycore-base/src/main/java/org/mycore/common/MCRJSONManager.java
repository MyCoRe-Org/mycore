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

package org.mycore.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class MCRJSONManager {

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    public void registerAdapter(MCRJSONTypeAdapter<?> typeAdapter) {
        gsonBuilder.registerTypeAdapter(typeAdapter.bindTo(), typeAdapter);
    }

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static MCRJSONManager instance() {
        return obtainInstance();
    }

    public static MCRJSONManager obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public GsonBuilder getGsonBuilder() {
        return gsonBuilder;
    }

    public Gson createGson() {
        return gsonBuilder.create();
    }

    private static final class LazyInstanceHolder {
        public static final MCRJSONManager SHARED_INSTANCE = new MCRJSONManager();
    }

}
