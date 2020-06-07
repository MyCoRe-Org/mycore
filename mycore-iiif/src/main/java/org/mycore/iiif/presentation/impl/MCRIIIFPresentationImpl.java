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

package org.mycore.iiif.presentation.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

public abstract class MCRIIIFPresentationImpl {

    private static final String MCR_IIIF_PRESENTATION_CONFIG_PREFIX = "MCR.IIIFPresentation.";

    private static final Map<String, MCRIIIFPresentationImpl> IMPLHOLDER = new HashMap<>();

    private final String implName;

    public MCRIIIFPresentationImpl(final String implName) {
        this.implName = implName;
    }

    public static synchronized MCRIIIFPresentationImpl getInstance(String implNameParameter) {
        String implName = (implNameParameter == null || implNameParameter.isBlank()) ?
            MCRConfiguration2.getStringOrThrow("MCR.IIIFPresentation.Default") :
            implNameParameter;

        if (IMPLHOLDER.containsKey(implName)) {
            return IMPLHOLDER.get(implName);
        }

        if (IMPLHOLDER.containsKey(implName)) {
            return IMPLHOLDER.get(implName);
        }

        String classPropertyName = MCR_IIIF_PRESENTATION_CONFIG_PREFIX + implName;
        Class<? extends MCRIIIFPresentationImpl> classObject = MCRConfiguration2.<MCRIIIFPresentationImpl>getClass(
            classPropertyName)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(classPropertyName));

        try {
            Constructor<? extends MCRIIIFPresentationImpl> constructor = classObject.getConstructor(String.class);
            MCRIIIFPresentationImpl presentationImpl = constructor.newInstance(implName);
            IMPLHOLDER.put(implName, presentationImpl);
            return presentationImpl;
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + classObject.getName() + ") needs a string constructor: " + classPropertyName);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    public String getImplName() {
        return implName;
    }

    protected final Map<String, String> getProperties() {
        return MCRConfiguration2.getSubPropertiesMap(getConfigPrefix());
    }

    private String getConfigPrefix() {
        return MCR_IIIF_PRESENTATION_CONFIG_PREFIX + implName + ".";
    }

    public abstract MCRIIIFManifest getManifest(String id);

}
