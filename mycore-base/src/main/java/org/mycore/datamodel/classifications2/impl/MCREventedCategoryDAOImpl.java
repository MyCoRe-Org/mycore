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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
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

    private static MCREventManager manager = MCREventManager.instance();

    private static final String EVENT_OBJECT = MCREvent.CLASS_TYPE;

    @Override
    public MCRCategory addCategory(MCRCategoryID parentID, MCRCategory category, int position) {
        MCRCategory rv = super.addCategory(parentID, category, position);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.CREATE_EVENT);
        evt.put("class", category);
        manager.handleEvent(evt);
        queueForCommit(evt);
        return rv;
    }

    @Override
    public void deleteCategory(MCRCategoryID id) {
        MCRCategory category = super.getCategory(id, -1);
        if (category == null) {
            throw new MCRPersistenceException("Category " + id + " was not found. Delete aborted.");
        }
        MCREvent evt = new MCREvent(MCREvent.CLASS_TYPE, MCREvent.DELETE_EVENT);
        evt.put("class", category);
        manager.handleEvent(evt, MCREventManager.BACKWARD);
        queueForCommit(evt);
        super.deleteCategory(id);
    }

    @Override
    public void moveCategory(MCRCategoryID id, MCRCategoryID newParentID, int index) {
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        evt.put("parent", super.getCategory(newParentID, -1));
        evt.put("index", index);
        // Type is used for specifying a special Update operation
        // originally named UType (Update Type), it is an Optional Value
        evt.put("type", "move");
        manager.handleEvent(evt);
        queueForCommit(evt);
        super.moveCategory(id, newParentID, index);
    }

    @Override
    public MCRCategory removeLabel(MCRCategoryID id, String lang) {
        MCRCategory rv = super.removeLabel(id, lang);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        queueForCommit(evt);
        return rv;
    }

    @Override
    public Collection<MCRCategoryImpl> replaceCategory(MCRCategory newCategory) throws IllegalArgumentException {
        Collection<MCRCategoryImpl> rv = super.replaceCategory(newCategory);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", newCategory);
        manager.handleEvent(evt);
        queueForCommit(evt);
        return rv;
    }

    @Override
    public MCRCategory setLabel(MCRCategoryID id, MCRLabel label) {
        MCRCategory rv = super.setLabel(id, label);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        queueForCommit(evt);
        return rv;
    }

    @Override
    public MCRCategory setLabels(MCRCategoryID id, Set<MCRLabel> labels) {
        MCRCategory rv = super.setLabels(id, labels);
        MCREvent evt = new MCREvent(EVENT_OBJECT, MCREvent.UPDATE_EVENT);
        evt.put("class", super.getCategory(id, -1));
        manager.handleEvent(evt);
        queueForCommit(evt);
        return rv;
    }

    @SuppressWarnings("unchecked")
    protected void queueForCommit(MCREvent evt) {
        String classQueue = "classQueue";
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        ((ArrayList<MCREvent>) currentSession.get(classQueue)).add(evt);
    }
}
