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

package org.mycore.common.log4j2.lookups;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.mycore.common.config.MCRConfiguration;

/**
 * Lookup a value in {@link MCRConfiguration}. Uses <code>key</code> as property key.
 * 
 * @author Thomas Scheffler
 */
@Plugin(
    name = "mcrcfg",
    category = StrLookup.CATEGORY)
public class MCRConfigurationLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        return MCRConfiguration.instance().getString(key, null);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

}
