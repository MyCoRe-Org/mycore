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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceMapSource} is a {@link MCRSource} that interprets a {@link MCRInstanceMap}.
 */
final class MCRInstanceMapSource extends MCRInstanceSourceBase<Map<String, Object>> {

    private final MCRInstanceMap annotation;

    private final MCRSentinel sentinel;

    MCRInstanceMapSource(MCRInstanceMap annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        this.sentinel = annotationProvider.get(MCRSentinel.class);
    }

    @Override
    public Type type() {
        return Type.INSTANCE_MAP;
    }

    @Override
    public Class<MCRInstanceMap> annotationClass() {
        return MCRInstanceMap.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Set<MCRTarget.Type> allowedTargetTypes() {
        return MCRTarget.Types.ALL;
    }

    @Override
    public Class<?> valueClass() {
        return Map.class;
    }

    @Override
    protected String description() {
        return "instance map";
    }

    @Override
    protected String name() {
        return annotation.name();
    }

    @Override
    protected boolean allowsEmptyName() {
        return true;
    }

    @Override
    protected boolean absolute() {
        return false;
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

    @Override
    protected String defaultName() {
        return "";
    }

    @Override
    protected Map<String, Object> getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurationMap =
            configuration.nestedMap(annotation.valueClass(), annotation.name());

        Map<String, Object> instanceMap = new HashMap<>();

        for (String key : nestedConfigurationMap.keySet()) {

            MCRSourceContext nestedContext = context.nested(key, "instance map entry");
            MCRInstanceConfiguration<?> nestedConfiguration = nestedConfigurationMap.get(key);
            Object instance = createInstance(nestedContext, nestedConfiguration, sentinel);

            if (instance != null) {
                instanceMap.put(key, instance);
            }

        }

        return instanceMap;

    }

    @Override
    protected boolean isMissingResult(Map<String, Object> result) {
        return result.isEmpty();
    }

    @Override
    protected MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.emptyException();
    }

    @Override
    protected Map<String, Object> missingResultReplacement() {
        return new HashMap<>();
    }

}
