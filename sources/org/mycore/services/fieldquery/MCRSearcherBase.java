/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.List;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Abstract base class for searchers and indexers. Searcher implementations for
 * a specific backend should be implemented as a subclass. This class implements
 * MCREventHandler. Indexers can easily be implemented by overwriting the two
 * methods addToIndex and removeFromIndex. Searchers are implemented by
 * overwriting the method search.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRSearcherBase extends MCREventHandlerBase implements MCREventHandler, MCRSearcher {
    /** The unique searcher ID for this MCRSearcher implementation */
    protected String ID;

    /** The prefix of all properties in mycore.properties for this searcher */
    protected String prefix;

    /** The ID of the index this searcher handles * */
    protected String index;

    /**
     * Initializes the searcher and sets its unique ID.
     * 
     * @param ID
     *            the non-null unique ID of this searcher instance
     */
    public void init(String ID) {
        this.ID = ID;
        this.prefix = "MCR.Searcher." + ID + ".";
        this.index = MCRConfiguration.instance().getString(prefix + "Index");
    }

    /**
     * Returns the unique store ID that was set for this store instance
     * 
     * @return the unique store ID that was set for this store instance
     */
    public String getID() {
        return ID;
    }

    /**
     * Returns the ID of the index this searcher is configured for.
     * 
     * @return
     */
    public String getIndex() {
        return index;
    }

    public abstract MCRResults search(MCRCondition condition, int maxResults);

    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        List fields = MCRData2Fields.buildFields(file, index);
        addToIndex(entryID, fields);
    }

    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        List fields = MCRData2Fields.buildFields(file, index);
        removeFromIndex(entryID);
        addToIndex(entryID, fields);
    }

    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        removeFromIndex(entryID);
    }

    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        List fields = MCRData2Fields.buildFields(obj, index);
        addToIndex(entryID, fields);
    }

    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        List fields = MCRData2Fields.buildFields(obj, index);
        removeFromIndex(entryID);
        addToIndex(entryID, fields);
    }

    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        removeFromIndex(entryID);
    }

    /**
     * Adds field values to the search index. Searchers that need an indexer
     * must overwrite this method to store the values in their backend index. If
     * this class is configured as event handler, this method is automatically
     * called when objects are created or updated. The field values have been
     * extracted from the object's data as defined by searchfields.xml
     * 
     * @param entryID
     *            the unique ID of this entry in the index
     * @param fields
     *            a List of MCRSearchField objects
     */
    protected void addToIndex(String entryID, List fields) {
    }

    /**
     * Removes the values of the given entry from the backend index. Searchers
     * that need an indexer must overwrite this method to delete the values in
     * their backend index. If this class is configured as event handler, this
     * method is automatically called when objects are deleted or updated.
     * 
     * @param entryID
     *            the unique ID of this entry in the index
     */
    protected void removeFromIndex(String entryID) {
    }
}
