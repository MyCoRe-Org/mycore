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

package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.Collection;
import java.util.SortedSet;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Category DAO Implementation with Event Handlers
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCREventedCategoryDAOImpl extends MCRCategoryDAOImpl {

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory rv = super.addCategory(parentID, category, position);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.CREATE);
        evt.put(MCREvent.CLASS_KEY, category);
        evt.put("parent", super.getCategory(parentID, -1));
        evt.put("index", position);
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCRCategory category = super.getCategory(id, -1);
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.DELETE);
        evt.put(MCREvent.CLASS_KEY, category);
        MCREventManager.instance().handleEvent(evt, MCREventManager.BACKWARD);
        super.deleteCategory(id);
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, super.getCategory(id, -1));
        evt.put("parent", super.getCategory(newParentID, -1));
        evt.put("index", index);
        // Type is used for specifying a special Update operation
        // originally named UType (Update Type), it is an Optional Value
        evt.put("type", "move");
        MCREventManager.instance().handleEvent(evt);
        super.moveCategory(id, newParentID, index);
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        MCRCategory rv = super.removeLabel(id, lang);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, super.getCategory(id, -1));
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> rv = super.replaceCategory(newCategory);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, newCategory);
        evt.put("replaced", rv.stream().map(MCRCategory::getId).toList());
        evt.put("type", "replace");
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        MCRCategory rv = super.setLabel(id, label);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, super.getCategory(id, -1));
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, SortedSet<MCRLabel> labels) {
        MCRCategory rv = super.setLabels(id, labels);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, super.getCategory(id, -1));
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }

    @Override
    public MCRCategory setURI(MCRCategoryID id, URI uri) {
        MCRCategory rv = super.setURI(id, uri);
        MCREvent evt = new MCREvent(MCREvent.ObjectType.CLASS, MCREvent.EventType.UPDATE);
        evt.put(MCREvent.CLASS_KEY, super.getCategory(id, -1));
        MCREventManager.instance().handleEvent(evt);
        return rv;
    }
}
