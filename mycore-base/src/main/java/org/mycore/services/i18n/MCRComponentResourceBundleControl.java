/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 4, 2013 $
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
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRComponent;
import org.mycore.datamodel.language.MCRLanguageFactory;

import com.google.common.collect.Lists;

/**
 * A Control class that merges all bundles of components defined in {@link #MCRComponentResourceBundleControl(Collection)}.
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
class MCRComponentResourceBundleControl extends Control {

    private Locale defaultLocale = MCRLanguageFactory.instance().getDefaultLanguage().getLocale();

    private Collection<MCRComponent> components;

    public MCRComponentResourceBundleControl(Collection<MCRComponent> components) {
        this.components = components;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
        throws IllegalAccessException, InstantiationException, IOException {
        Logger.getLogger(getClass()).info("New bundle: " + baseName + ", locale " + locale);
        try (InputStream is = getCombindedInputStream(locale)) {
            if (is != null) {
                return new PropertyResourceBundle(is);
            }
        }
        return null;
    }

    private InputStream getCombindedInputStream(Locale locale) {
        LinkedList<InputStream> cList = new LinkedList<>();
        for (MCRComponent component : components) {
            InputStream is = component.getMessagesInputStream(locale);
            if (is != null) {
                cList.add(is);
            }
        }
        if (cList.isEmpty()) {
            return null;
        }
        return new SequenceInputStream(Collections.enumeration(cList));
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((components == null) ? 0 : components.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRComponentResourceBundleControl)) {
            return false;
        }
        MCRComponentResourceBundleControl other = (MCRComponentResourceBundleControl) obj;
        if (components == null) {
            if (other.components != null) {
                return false;
            }
        } else if (!components.equals(other.components)) {
            return false;
        }
        return true;
    }
}
