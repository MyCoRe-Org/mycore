/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 2, 2013 $
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import org.mycore.common.config.MCRRuntimeComponentDetector;

/**
 * A {@link ResourceBundle} that automatically includes mycore and application specific <strong>messages*.properties</strong> files.
 * I18N keys from {@link MCRRuntimeComponentDetector#getMyCoReComponents()} can be overwritten by
 * {@link MCRRuntimeComponentDetector#getApplicationModules()}. All of these are in turn overwritten by <code>baseName</code>
 * parameter of {@link MCRStackedResourceBundle#getResourceBundle(String, Locale)}, e.g. <strong>"messages"</strong> for
 * <strong>"messages*.properties"</strong>.
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRStackedResourceBundle extends ResourceBundle {

    private static final MCRStackedResourceBundleControl CONTROL = new MCRStackedResourceBundleControl();

    private ResourceBundle[] bundles;

    MCRStackedResourceBundle(ResourceBundle... bundles) {
        this.bundles = bundles;
    }

    /* (non-Javadoc)
     * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
     */
    @Override
    protected Object handleGetObject(String key) {
        for (ResourceBundle bundle : bundles) {
            if (bundle.containsKey(key)) {
                return bundle.getObject(key);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.ResourceBundle#getKeys()
     */
    @Override
    public Enumeration<String> getKeys() {
        return new BundleEnumeration(bundles);
    }

    private static class BundleEnumeration implements Enumeration<String> {
        private ResourceBundle[] bundles;

        private Enumeration<String> currentEnumeration;

        private int pos;

        public BundleEnumeration(ResourceBundle... bundles) {
            pos = -1;
            currentEnumeration = Collections.emptyEnumeration();
        }

        @Override
        public boolean hasMoreElements() {
            if (currentEnumeration.hasMoreElements()) {
                return true;
            } else {
                if (next()) {
                    return hasMoreElements();
                }
            }
            return false;
        }

        private boolean next() {
            if (++pos < bundles.length) {
                currentEnumeration = bundles[pos].getKeys();
                return true;
            }
            return false;
        }

        @Override
        public String nextElement() {
            if (hasMoreElements()) {
                return currentEnumeration.nextElement();
            }
            throw new NoSuchElementException();
        }

    }

    public static ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return ResourceBundle.getBundle("stacked:" + baseName, locale, CONTROL);
    }
}
