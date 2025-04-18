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

package org.mycore.datamodel.ifs2;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

class MCRStoreDefaultConfig implements MCRStoreConfig {
    private String storeConfigPrefix;

    private String id;

    MCRStoreDefaultConfig(String id) {
        this.id = id;
        storeConfigPrefix = "MCR.IFS2.Store." + id + ".";
    }

    @Override
    public String getBaseDir() {
        return MCRConfiguration2.getStringOrThrow(storeConfigPrefix + "BaseDir");
    }

    @Override
    public String getPrefix() {
        return MCRConfiguration2.getString(storeConfigPrefix + "Prefix").orElse(id + "_");
    }

    @Override
    public String getSlotLayout() {
        return MCRConfiguration2.getStringOrThrow(storeConfigPrefix + "SlotLayout");
    }

    @Override
    public String getID() {
        return id;
    }

}
