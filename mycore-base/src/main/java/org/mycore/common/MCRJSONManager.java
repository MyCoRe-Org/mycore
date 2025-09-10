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
 * An automatically configured shared instance can be obtained with
 * {@link MCRJSONManager#obtainInstance()}. This instance should generally be used,
 * although custom instances can be created when necessary. It is configured using the property prefix
 * {@link MCRJSONManager#MANAGER_PROPERTY}.
 * <pre><code>
 * MCR.JSON.Manger.Class=org.mycore.common.MCRJSONManager
 * </code></pre>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRJSONManager#TYPE_ADAPTERS_KEY} can be used to
 * specify the list of type adapters to be used.
 * <li> For each type adapter, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that adapter from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.MCRJSONManager
 * [...].TypeAdapters.foo.Class=foo.bar.FooTypeAdapter
 * [...].TypeAdapters.foo.Enabled=true
 * [...].TypeAdapters.foo.Key1=Value1
 * [...].TypeAdapters.foo.Key2=Value2
 * [...].TypeAdapters.bar.Class=foo.bar.BarTypeAdapter
 * [...].TypeAdapters.bar.Enabled=false
 * [...].TypeAdapters.bar.Key1=Value1
 * [...].TypeAdapters.bar.Key2=Value2
 * </code></pre>
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

    public static MCRJSONManager obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCRJSONManager createInstance() {
        String classProperty = MANAGER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRJSONManager.class, classProperty);
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
