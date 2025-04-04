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

package org.mycore.migration.strategy;

import org.jdom2.Document;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Strategy interface to decide whether the children element should be migrated
 * to childrenOrder during the object normalization migration (MCR-3375).
 */
@FunctionalInterface
public interface ChildrenOrderMigrationStrategy {

    /**
     * Decides if the children element should be migrated to childrenOrder for the given object.
     *
     * @param objectId The ID of the object being migrated.
     * @param objectXML The XML document of the object being migrated.
     * @return true if the children element should be renamed to childrenOrder and attributes removed, false otherwise.
     */
    boolean shouldAddChildrenOrder(MCRObjectID objectId, Document objectXML);

}
