/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 5, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.i18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.ResourceBundle.Control;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRRuntimeComponentDetector;

import com.google.common.collect.Lists;

/**
 * A {@link Control} that stacks different ResourceBundles of {@link MCRStackedResourceBundle}.
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRStackedResourceBundleControl extends Control {
    private static Logger LOGGER = Logger.getLogger(MCRStackedResourceBundleControl.class);

    private static MCRComponentResourceBundleControl MCR_RB_CTRL = new MCRComponentResourceBundleControl(
        MCRRuntimeComponentDetector.getMyCoReComponents());

    private static MCRComponentResourceBundleControl APP_RB_CTRL = new MCRComponentResourceBundleControl(
        MCRRuntimeComponentDetector.getApplicationModules());

    @Override
    public List<String> getFormats(String baseName) {
        return Lists.newArrayList("mycore.stacked");
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
        LOGGER.info("New bundle: " + baseName + ", locale " + locale);
        String bundleName = baseName.substring(baseName.indexOf(':') + 1);
        ResourceBundle mainBundle = null;
        ResourceBundle mcrBundle = null;
        ResourceBundle appBundle = null;
        try {
            mainBundle = ResourceBundle.getBundle(bundleName, locale);
        } catch (MissingResourceException e) {
            LOGGER.debug("Could not load resource bundle " + bundleName);
        }
        try {
            mcrBundle = ResourceBundle.getBundle(Integer.toString(MCR_RB_CTRL.hashCode()), locale, MCR_RB_CTRL);
        } catch (MissingResourceException e) {
            LOGGER.debug("Could not load mcr bundle " + baseName);
        }
        SortedSet<MCRComponent> applicationModules = MCRRuntimeComponentDetector.getApplicationModules();
        if (!applicationModules.isEmpty()) {
            try {
                appBundle = ResourceBundle.getBundle(Integer.toString(APP_RB_CTRL.hashCode()), locale, APP_RB_CTRL);
            } catch (MissingResourceException e) {
                LOGGER.info("Could not find message keys in: " + applicationModules);
            }
        }
        ArrayList<ResourceBundle> bundleStack = Lists.newArrayList(mainBundle, appBundle, mcrBundle);
        while (bundleStack.remove(null)) {
            //remove unresolved bundles
        }
        if (bundleStack.isEmpty()) {
            throw new MissingResourceException("Can't find bundle for base name " + baseName + ", locale " + locale,
                bundleName + "_" + locale, // className
                "");
        }
        ResourceBundle[] resourceBundles = bundleStack.toArray(new ResourceBundle[bundleStack.size()]);
        return new MCRStackedResourceBundle(resourceBundles);
    }

}
