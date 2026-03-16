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

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.emptyNameException;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.missingException;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.property;

import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceSource} is a {@link MCRSourceBase} that interprets a {@link MCRInstance}.
 */
@SuppressWarnings({ "PMD.MCR.Singleton.ClassModifiers", "PMD.MCR.Singleton.PrivateConstructor",
    "PMD.MCR.Singleton.NonPrivateConstructors", "PMD.MCR.Singleton.MethodModifiers",
    "PMD.MCR.Singleton.MethodReturnType", "PMD.SingletonClassReturningNewInstance" })
final class MCRInstanceSource extends MCRSourceBase {

    private final MCRInstance annotation;

    MCRInstanceSource(MCRInstance annotation) {
        this.annotation = annotation;
    }

    @Override
    public MCRSourceType type() {
        return MCRSourceType.INSTANCE;
    }

    @Override
    public Class<MCRInstance> annotationClass() {
        return MCRInstance.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Class<?> valueClass() {
        return annotation.valueClass();
    }

    @Override
    public Object get(MCRInstanceConfiguration configuration, MCRTarget target) {

        String name = annotation.name();
        if (name.isEmpty()) {
            throw emptyNameException(target);
        }

        Object instance = getInstance(configuration, target, name);

        if (instance == null && annotation.required()) {
            throw missingException(property(configuration, annotation.name()), target, "instance");
        }

        return instance;

    }

    private Object getInstance(MCRInstanceConfiguration configuration, MCRTarget target, String name) {

        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration(name);
        String property = nestedConfiguration.name().canonical();

        Class<?> valueClass = annotation.valueClass();
        MCRSentinel sentinel = annotation.sentinel();

        return getInstance(property, target, valueClass, nestedConfiguration, sentinel, "instance");

    }

}
