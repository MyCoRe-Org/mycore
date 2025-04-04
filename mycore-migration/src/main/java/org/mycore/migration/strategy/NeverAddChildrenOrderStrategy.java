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
 * A {@link ChildrenOrderMigrationStrategy} that never decides to add the childrenOrder element.
 * This is the default strategy if no specific strategy is configured.
 */
public class NeverAddChildrenOrderStrategy implements ChildrenOrderMigrationStrategy {

    @Override
    public boolean shouldAddChildrenOrder(MCRObjectID objectId, Document objectXML) {
        return false;
    }

}
