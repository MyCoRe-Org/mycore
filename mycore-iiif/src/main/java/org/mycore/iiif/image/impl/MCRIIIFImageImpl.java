package org.mycore.iiif.image.impl;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.iiif.image.model.MCRIIIFImageInformation;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;

public abstract class MCRIIIFImageImpl {

    private static final String MCR_IIIF_IMAGE_CONFIG_PREFIX = "MCR.IIIFImage.";

    private static final Map<String, MCRIIIFImageImpl> implHolder = new HashMap<>();

    private final String implName;

    public MCRIIIFImageImpl(final String implName) {
        this.implName = implName;
    }

    public static synchronized MCRIIIFImageImpl getInstance(String implName) {
        if (implHolder.containsKey(implName)) {
            return implHolder.get(implName);
        }

        String classPropertyName = MCR_IIIF_IMAGE_CONFIG_PREFIX + implName;
        String className = MCRConfiguration.instance().getString(classPropertyName);

        try {
            Class<MCRIIIFImageImpl> classObject = (Class<MCRIIIFImageImpl>) Class.forName(className);
            Constructor<MCRIIIFImageImpl> constructor = classObject.getConstructor(String.class);
            MCRIIIFImageImpl imageImpl = constructor.newInstance(implName);
            implHolder.put(implName, imageImpl);
            return imageImpl;
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
            .getPropertiesMap(MCR_IIIF_IMAGE_CONFIG_PREFIX + implName + ".");

        Map<String, String> shortened = new HashMap<>();

        propertiesMap.keySet().stream().forEach(key -> {
            String newKey = key.substring(MCR_IIIF_IMAGE_CONFIG_PREFIX.length() + implName.length() + 1);
            shortened.put(newKey, propertiesMap.get(key));
        });

        return shortened;
    }

    public abstract BufferedImage provide(String identifier,
        MCRIIIFImageSourceRegion region,
        MCRIIIFImageTargetSize targetSize,
        MCRIIIFImageTargetRotation rotation,
        MCRIIIFImageQuality imageQuality,
        String format)
        throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException, MCRIIIFUnsupportedFormatException,
        MCRAccessException;

    public abstract MCRIIIFImageInformation getInformation(String identifier)
        throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException, MCRAccessException;

    public abstract MCRIIIFImageProfile getProfile();

}
