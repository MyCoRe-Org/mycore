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

import edu.wisc.library.ocfl.api.util.Enforce;
import edu.wisc.library.ocfl.core.extension.OcflExtensionConfig;

public class MCRLayoutConfig implements OcflExtensionConfig {

    public String extensionName;

    private String pattern;

    private String slotLayout;

    public MCRLayoutConfig() {
        pattern = MCRConfiguration2.getString("MCR.Metadata.ObjectID.NumberPattern").orElse("0000000000");
        String defaultLayout = pattern.length() - 4 + "-2-2";
        slotLayout = MCRConfiguration2.getString("MCR.IFS2.Store.class.SlotLayout").orElse(defaultLayout);
    }

    @Override
    public String getExtensionName() {
        return MCRLayoutExtension.EXTENSION_NAME;
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    /**
     * Overwrites the SlotLayout for the OCFL Repository
     * 
     * @param slotLayout MyCoRe SlotLayout, see MCRStore for more info
     * @return MCRLayoutConfig
     */
    public MCRLayoutConfig setSlotLayout(String slotLayout) {
        this.slotLayout = Enforce.notNull(slotLayout, "SlotLayout cannot be null!");
        return this;
    }

    /**
     * @return Current SlotLayout
     */
    public String getSlotLayout() {
        return this.slotLayout;
    }

    /**
     * Overwrites the Pattern for the OCFL Repository
     * 
     * @param pattern MyCoRe Pattern, see MCRStore for more info
     * @return MCRLayoutConfig
     */
    public MCRLayoutConfig setPattern(String pattern) {
        this.pattern = Enforce.notNull(pattern, "Pattern cannot be null!");
        return this;
    }

    /**
     * @return Current Pattern
     */
    public String getPattern() {
        return this.pattern;
    }
}
