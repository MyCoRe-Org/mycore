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

package org.mycore.common.config.instantiator.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.instantiator.MCRInstantiatorUtils;
import org.mycore.common.config.instantiator.target.MCRTarget;

public final class MCRSourceContext {

    private final MCRTarget target;

    private final String property;

    private final String description;

    private final List<String> hints;

    public MCRSourceContext(MCRTarget target, String property, String description, String... hints) {
        this(target, property, description, List.of(hints));
    }

    private MCRSourceContext(MCRTarget target, String property, String description, List<String> hints) {
        this.target = target;
        this.property = property;
        this.description = description;
        this.hints = hints;
    }

    public MCRTarget target() {
        return target;
    }

    public String property() {
        return property;
    }

    public String description() {
        return description + (hints.isEmpty() ? "" : " (" + String.join(", ", hints) + ")");
    }

    public List<String> hints() {
        return hints;
    }

    public MCRSourceContext nested(String suffix, String description) {
        return new MCRSourceContext(target, property + "." + suffix, description, hints);
    }

    public List<String> orderedKeys(Map<String, ?> map) {

        SortedMap<Integer, String> keyMap = new TreeMap<>();
        for (String key : map.keySet()) {
            try {
                Integer integerValue = Integer.parseInt(key);
                String alreadyMappedKey = keyMap.put(integerValue, key);
                if (alreadyMappedKey != null && !alreadyMappedKey.equals(key)) {
                    throw inconsistentKeysException(key, alreadyMappedKey);
                }
            } catch (NumberFormatException exception) {
                throw nonIntegerKeyException(key, exception);
            }
        }

        return new ArrayList<>(keyMap.values());

    }

    public MCRConfigurationException configurationException(String exceptionMessage) {
        return new MCRConfigurationException(exceptionMessage(exceptionMessage));
    }

    public MCRConfigurationException configurationException(String exceptionMessage, Exception exception) {
        return new MCRConfigurationException(exceptionMessage(exceptionMessage), exception);
    }

    public MCRConfigurationException incompatibilityException(Class<?> annotationValueClass, Class<?> actualClass) {
        return configurationException("has a class (" + actualClass.getName() + ")"
            + " that is incompatible with the annotated value class (" + annotationValueClass.getName() + ")");
    }

    public MCRConfigurationException missingException() {
        return configurationException("is missing");
    }

    public MCRConfigurationException emptyException() {
        return configurationException("is empty");
    }

    public MCRConfigurationException nonIntegerKeyException(String key, NumberFormatException exception) {
        return configurationException("has element with non-integer key " + key, exception);
    }

    public MCRConfigurationException inconsistentKeysException(String key1, String key2) {
        return configurationException("has element with inconsistent integer keys " + key1 + " and " + key2);
    }

    private String exceptionMessage(String exceptionMessage) {
        return MCRInstantiatorUtils.capitalize(description()) + ", configured in " + property()
            + " (and its sub-properties)," + " for target " + target.type().name().toLowerCase(Locale.ROOT) + " '"
            + target.name() + "' in configured class " + target.declaringClass().getName() + " " + exceptionMessage;
    }

}
