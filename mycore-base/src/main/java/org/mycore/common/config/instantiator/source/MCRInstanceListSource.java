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

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.emptyException;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.orderedKeys;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceListSource} is a {@link MCRSourceBase} that interprets a {@link MCRInstanceList}.
 */
final class MCRInstanceListSource extends MCRSourceBase {

    private final MCRInstanceList annotation;

    MCRInstanceListSource(MCRInstanceList annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.INSTANCE_LIST;
    }

    @Override
    public Class<MCRInstanceList> annotationClass() {
        return MCRInstanceList.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Class<?> valueClass() {
        return List.class;
    }

    @Override
    public List<Object> get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {

        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurationMap =
            configuration.nestedMap(annotation.valueClass(), annotation.name());

        List<String> keyList = orderedKeys(property(configuration, annotation.name()), target,
            nestedConfigurationMap, "instance list");

        List<Object> instanceList = new ArrayList<>(nestedConfigurationMap.size());
        for (String key : keyList) {
            MCRInstanceConfiguration<?> nestedConfiguration = nestedConfigurationMap.get(key);
            Object instance = getInstance(target, nestedConfiguration, annotation.sentinel(), "instance list element");
            if (instance != null) {
                instanceList.add(instance);
            }
        }

        if (instanceList.isEmpty() && annotation.required()) {
            throw emptyException(property(configuration, annotation.name()), target, "instance list");
        }

        return instanceList;

    }

}
