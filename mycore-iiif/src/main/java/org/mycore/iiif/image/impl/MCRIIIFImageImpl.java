/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

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
