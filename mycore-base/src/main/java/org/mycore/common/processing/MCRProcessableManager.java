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

package org.mycore.common.processing;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Manager for {@link MCRProcessable} and {@link MCRProcessableCollection} that manages a shared
 * {@link MCRProcessableRegistry}.
 */
public final class MCRProcessableManager {

    private static final MCRProcessableManager SINGLETON_INSTANCE = new MCRProcessableManager();

    private final MCRProcessableRegistry registry;

    private MCRProcessableManager() {
        registry = MCRConfiguration2.getInstanceOfOrThrow(
            MCRProcessableRegistry.class, "MCR.Processable.Registry.Class");
    }
    
    public static MCRProcessableManager getInstance() {
        return SINGLETON_INSTANCE;
    }

    public MCRProcessableRegistry getRegistry() {
        return registry;
    }

}
