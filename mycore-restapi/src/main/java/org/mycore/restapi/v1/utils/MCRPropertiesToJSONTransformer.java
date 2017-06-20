package org.mycore.restapi.v1.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@Produces(MediaType.APPLICATION_JSON)
@Provider
public class MCRPropertiesToJSONTransformer implements MessageBodyWriter<Properties> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Properties.class.isAssignableFrom(type) && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public long getSize(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Properties t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {
        httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE,
            MediaType.APPLICATION_JSON_TYPE.withCharset(StandardCharsets.UTF_8.name()));
        final Type mapType = new TypeToken<Map<String, String>>() {
            private static final long serialVersionUID = 1L;
        }.getType();
        Map<String, String> writeMap = t.entrySet()
            .stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        String json = new GsonBuilder().registerTypeAdapter(mapType, new BundleMapSerializer())
            .create()
            .toJson(writeMap, mapType);
        entityStream.write(json.getBytes(StandardCharsets.UTF_8));
    }

    public static class BundleMapSerializer implements JsonSerializer<Map<String, String>> {

        private static final Logger LOGGER = LogManager.getLogger();

        @Override
        public JsonElement serialize(final Map<String, String> bundleMap, final Type typeOfSrc,
            final JsonSerializationContext context) {
            final JsonObject resultJson = new JsonObject();

            for (final String key : bundleMap.keySet()) {
                try {
                    createFromBundleKey(resultJson, key, bundleMap.get(key));
                } catch (final IOException e) {
                    LOGGER.error("Bundle map serialization exception: ", e);
                }
            }

            return resultJson;
        }

        public static JsonObject createFromBundleKey(final JsonObject resultJson, final String key, final String value)
            throws IOException {
            if (!key.contains(".")) {
                resultJson.addProperty(key, value);

                return resultJson;
            }

            final String currentKey = firstKey(key);
            if (currentKey != null) {
                final String subRightKey = key.substring(currentKey.length() + 1, key.length());
                final JsonObject childJson = getJsonIfExists(resultJson, currentKey);

                resultJson.add(currentKey, createFromBundleKey(childJson, subRightKey, value));
            }

            return resultJson;
        }

        private static String firstKey(final String fullKey) {
            final String[] splittedKey = fullKey.split("\\.");

            return (splittedKey.length != 0) ? splittedKey[0] : fullKey;
        }

        private static JsonObject getJsonIfExists(final JsonObject parent, final String key) {
            if (parent == null) {
                LOGGER.warn("Parent json parameter is null!");
                return null;
            }

            if (parent.get(key) != null && !(parent.get(key) instanceof JsonObject)) {
                throw new IllegalArgumentException("Invalid key \'" + key + "\' for parent: " + parent
                    + "\nKey can not be JSON object and property or array in one time");
            }

            if (parent.getAsJsonObject(key) != null) {
                return parent.getAsJsonObject(key);
            } else {
                return new JsonObject();
            }
        }
    }
}
