package org.mycore.iiif.presentation.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;

public abstract class MCRIIIFPresentationImpl {

    private static final String MCR_IIIF_PRESENTATION_CONFIG_PREFIX = "MCR.IIIFPresentation.";

    private static final Map<String, MCRIIIFPresentationImpl> implHolder = new HashMap<>();

    private final String implName;

    public MCRIIIFPresentationImpl(final String implName) {
        this.implName = implName;
    }

    public static synchronized MCRIIIFPresentationImpl getInstance(String implName) {
        if (implHolder.containsKey(implName)) {
            return implHolder.get(implName);
        }

        String classPropertyName = MCR_IIIF_PRESENTATION_CONFIG_PREFIX + implName;
        String className = MCRConfiguration.instance().getString(classPropertyName);

        try {
            Class<MCRIIIFPresentationImpl> classObject = (Class<MCRIIIFPresentationImpl>) Class.forName(className);
            Constructor<MCRIIIFPresentationImpl> constructor = classObject.getConstructor(String.class);
            MCRIIIFPresentationImpl presentationImpl = constructor.newInstance(implName);
            implHolder.put(implName, presentationImpl);
            return presentationImpl;
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + className + ") not found: " + classPropertyName, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + className + ") needs a string constructor: " + classPropertyName);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
    }

    public String getImplName() {
        return implName;
    }

    protected final Map<String, String> getProperties() {
        Map<String, String> propertiesMap = MCRConfiguration.instance()
            .getPropertiesMap(MCR_IIIF_PRESENTATION_CONFIG_PREFIX + implName + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(MCR_IIIF_PRESENTATION_CONFIG_PREFIX.length() + implName.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract MCRIIIFManifest getManifest(String id);

}
