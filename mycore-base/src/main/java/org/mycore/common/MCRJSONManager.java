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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A {@link MCRJSONManager} can be used to obtain consistently configured JSON processing
 * capabilities.
 * <p>
 * A singular, globally available and centrally configured instance can be obtained with
 * {@link MCRJSONManager#obtainInstance()}. This instance is configured using the property prefix
 * {@link MCRJSONManager#MANAGER_PROPERTY} and should be used in order to obtain consistently
 * configured JSON processing capabilities, although custom instances can be created when necessary.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Type adapters are configured as a map using the property suffix {@link MCRJSONManager#TYPE_ADAPTERS_KEY}.
 * <li> Each type adapter can be excluded from the configuration using the property {@link MCRSentinel#ENABLED_KEY}.
 * </ul>
 * Example:
 * <pre>
 * MCR.JSON.Manger.Class=org.mycore.common.MCRJSONManager
 * MCR.JSON.Manger.TypeAdapters.foo.Class=foo.bar.FooTypeAdapter
 * MCR.JSON.Manger.TypeAdapters.foo.Enabled=true
 * MCR.JSON.Manger.TypeAdapters.foo.Key1=Value1
 * MCR.JSON.Manger.TypeAdapters.foo.Key2=Value2
 * MCR.JSON.Manger.TypeAdapters.bar.Class=foo.bar.BarTypeAdapter
 * MCR.JSON.Manger.TypeAdapters.bar.Enabled=false
 * MCR.JSON.Manger.TypeAdapters.bar.Key1=Value1
 * MCR.JSON.Manger.TypeAdapters.bar.Key2=Value2
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRJSONManager.Factory.class)
public final class MCRJSONManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MANAGER_PROPERTY = "MCR.JSON.Manager";

    public static final String TYPE_ADAPTERS_KEY = "TypeAdapters";

    private final GsonBuilder gsonBuilder = new GsonBuilder();

    public MCRJSONManager(List<MCRJSONTypeAdapter<?>> typeAdapters) {

        Objects.requireNonNull(typeAdapters, "Type adapters must not be null");
        typeAdapters.forEach(adapter -> Objects.requireNonNull(adapter, "Type adapter must not be null"));

        LOGGER.info(() -> "Working with type adapters for: " + String.join(", ", typeAdapters.stream()
            .map(adapter -> adapter.bindTo().toString()).toList()));
        typeAdapters.forEach(adapter -> gsonBuilder.registerTypeAdapter(adapter.bindTo(), adapter));

    }

    /**
     * @deprecated Provide adapters to the constructor or define them in properties.
     */
    @Deprecated
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

    public static MCRJSONManager createInstance() {
        String classProperty = MANAGER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRJSONManager.class, classProperty);
    }

    /**
     * @deprecated Use {@link #createGsonBuilder()} instead
     */
    @Deprecated
    public GsonBuilder getGsonBuilder() {
        return gsonBuilder.create().newBuilder();
    }

    public Gson createGson() {
        return gsonBuilder.create();
    }

    public GsonBuilder createGsonBuilder() {
        return gsonBuilder.create().newBuilder();
    }

    public static class Factory implements Supplier<MCRJSONManager> {

        @MCRInstanceMap(name = TYPE_ADAPTERS_KEY, valueClass = MCRJSONTypeAdapter.class, required = false,
            sentinel = @MCRSentinel)
        public Map<String, MCRJSONTypeAdapter<?>> typeAdapters;

        @Override
        public MCRJSONManager get() {
            return new MCRJSONManager(new ArrayList<>(typeAdapters.values()));
        }

    }

    private static final class LazyInstanceHolder {
        public static final MCRJSONManager SHARED_INSTANCE = createInstance();
    }

}
