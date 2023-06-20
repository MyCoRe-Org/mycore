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

package org.mycore.ocfl.layout;

import org.mycore.common.config.MCRConfiguration2;

import io.ocfl.api.util.Enforce;
import io.ocfl.core.extension.OcflExtensionConfig;

public class MCRStorageLayoutConfig implements OcflExtensionConfig {

    public String extensionName;

    private String slotLayout;

    public MCRStorageLayoutConfig() {
        slotLayout = MCRConfiguration2.getString("MCR.OCFL.MyCoReStorageLayout.SlotLayout").orElseGet(() -> {
            String pattern = MCRConfiguration2.getString("MCR.Metadata.ObjectID.NumberPattern").orElse("0000000000");
            return pattern.length() - 4 + "-2-2";
        });
    }

    @Override
    public String getExtensionName() {
        return MCRStorageLayoutExtension.EXTENSION_NAME;
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    /**
     * Overwrites the Class SlotLayout for the OCFL Repository
     * 
     * @param slotLayout MyCoRe SlotLayout, see MCRStore for more info
     * @return MCRLayoutConfig
     */
    public MCRStorageLayoutConfig setSlotLayout(String slotLayout) {
        this.slotLayout = Enforce.notNull(slotLayout, "Class SlotLayout cannot be null!");
        return this;
    }

    /**
     * @return Current SlotLayout
     */
    public String getSlotLayout() {
        return this.slotLayout;
    }

}
