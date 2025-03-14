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

package org.mycore.services.i18n;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfigurationInputStream;
import org.mycore.datamodel.language.MCRLanguageFactory;

import com.google.common.collect.Lists;

/**
 * A {@link Control} that stacks ResourceBundles of {@link MCRComponent}.
 * 
 * @author Thomas Scheffler (yagee)
 * @since 2014.04
 */
public class MCRCombinedResourceBundleControl extends Control {
    private static final Logger LOGGER = LogManager.getLogger();

    private Locale defaultLocale = MCRLanguageFactory.obtainInstance().getDefaultLanguage().getLocale();

    private static final ResourceBundle.Control CONTROL_HELPER = new ResourceBundle.Control() {
    };

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
        throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New bundle: {}, locale {}", baseName, locale);
        }
        Locale resolvedLocale;

        if (locale.equals(Locale.ROOT)) {
            //MCR-1064 fallback should be default language, if property key does not exist
            resolvedLocale = defaultLocale;
        } else {
            resolvedLocale = locale;
        }
        String bundleName = baseName.substring(baseName.indexOf(':') + 1);
        String filename = CONTROL_HELPER.toBundleName(bundleName, resolvedLocale) + ".properties";
        try (MCRConfigurationInputStream propertyStream = new MCRConfigurationInputStream(filename)) {
            if (propertyStream.isEmpty()) {
                String className = bundleName + "_" + resolvedLocale;
                throw new MissingResourceException(
                    "Can't find bundle for base name " + baseName + ", locale " + resolvedLocale, className, "");
            }
            return new PropertyResourceBundle(propertyStream);
        }
    }

    @Override
    public List<String> getFormats(String baseName) {
        return Lists.newArrayList("mycore");
    }

    @Override
    public Locale getFallbackLocale(String baseName, Locale locale) {
        return defaultLocale.equals(locale) ? null : defaultLocale;
    }

    @Override
    public long getTimeToLive(String baseName, Locale locale) {
        //JAR files never change in runtime
        return Long.MAX_VALUE;
    }

    @Override
    public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader,
        ResourceBundle bundle, long loadTime) {
        //JAR files never change in runtime
        return false;
    }

}
