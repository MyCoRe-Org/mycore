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

package org.mycore.migration.objectversion;

import java.nio.file.Path;

import org.mycore.datamodel.ifs2.MCRMetadataStore;

/**
 * This class exists only to make the protected method getSlot of MCRMetadataStore accessible.
 * 
 * <p>
 *     Do not use this class outside of the migration component!
 * </p>    
 * @author Thomas Scheffler (yagee)
 */
public class MCRMigrationStore extends MCRMetadataStore {
    @Override
    protected Path getSlot(int id) {
        return super.getSlot(id);
    }
}
