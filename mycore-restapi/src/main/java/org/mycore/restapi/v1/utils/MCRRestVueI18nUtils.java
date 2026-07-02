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
package org.mycore.restapi.v1.utils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.mycore.common.config.MCRProperties;
import org.mycore.services.i18n.MCRTranslation;

import com.google.gson.stream.JsonWriter;

/**
 * This class contains some generic utility functions to convert MyCoRe message properties
 * into the VueI18n Message Format Syntax (https://vue-i18n.intlify.dev/guide/essentials/syntax)
 *
 * @author Robert Stephan
 */
public class MCRRestVueI18nUtils {
    protected static final String SELF_KEY = "_self";

    public static void createVueI18nJson(JsonWriter writer, String lang, String filter) throws IOException {
        writer.beginObject();
        for (String l : lang.split(";")) {
            Locale locale = Locale.forLanguageTag(l);

            Properties data = new MCRProperties();
            for (String prefix : filter.split(";")) {
                data.putAll(MCRTranslation.translatePrefixToLocale(prefix, locale));
            }
            Map<String, Object> tree = flattenToVueI18nFormat(data);
            writer.name(l);
            writer.beginObject();
            outputVueI18nFormat(tree, writer);
            writer.endObject();
        }
        writer.endObject();
    }

    private static SortedMap<String, Object> flattenToVueI18nFormat(Properties messages) {
        SortedMap<String, Object> result = new TreeMap<>();

        for (Entry<Object, Object> entry : messages.entrySet()) {
            String[] parts = entry.getKey().toString().split("\\.");
            Map<String, Object> current = result;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];

                if (i == parts.length - 1) {
                    if (current.get(part) instanceof String existingString) {
                        Map<String, Object> promoted = new TreeMap<>();
                        promoted.put(SELF_KEY, existingString);
                        current.put(part, promoted);
                    }
                    if (current.get(part) instanceof Map<?, ?> existingMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> typedMap = (Map<String, Object>) existingMap;
                        typedMap.put(SELF_KEY, entry.getValue());
                    } else {
                        current.put(part, entry.getValue());
                    }
                } else {
                    if (current.get(part) instanceof String existingString) {
                        Map<String, Object> promoted = new TreeMap<>();
                        promoted.put(SELF_KEY, existingString);
                        current.put(part, promoted);
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> next =
                        (Map<String, Object>) current.computeIfAbsent(part, k -> new TreeMap<String, Object>());
                    current = next;
                }
            }
        }

        return result;
    }

    private static void outputVueI18nFormat(Map<String, Object> tree, JsonWriter writer) throws IOException {
        for (Entry<String, Object> e : tree.entrySet()) {
            writer.name(e.getKey());
            if (e.getValue() instanceof Map<?, ?> existingMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) existingMap;
                writer.beginObject();
                outputVueI18nFormat(typedMap, writer);
                writer.endObject();
            } else {
                writer.value(e.getValue().toString());
            }
        }
    }
}
