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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandler;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRCondition;

/**
 * Abstract base class for searchers and indexers. Searcher implementations for
 * a specific backend must be implemented as a subclass. This class implements
 * MCREventHandler. Indexers can easily be implemented by overwriting the two
 * methods addToIndex and removeFromIndex. Searchers are implemented by
 * overwriting the method search. Searchers that do not need indexing or do this
 * on their own can simply ignore the add/remove methods.
 * 
 * @author Frank L�tzenkirchen
 */
public abstract class MCRSearcher extends MCREventHandlerBase implements MCREventHandler {
    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRSearcher.class);

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
     */
    public String getIndex() {
        return index;
    }

    private String getReturnID(MCRFile file) {
        // Maybe fieldquery is used in application without link table manager
        if( MCRConfiguration.instance().getString( "MCR.Persistence.LinkTable.Store.Class", null ) == null ) 
          return file.getID();
            
        String ownerID = file.getOwnerID();
        
        List list = MCRLinkTableManager.instance().getSourceOf(ownerID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE);
        if ((list == null) || (list.size() == 0))
            return file.getID();
        
        // Return ID of MCRObject this MCRFile belongs to
        return (String) (list.get(0)); 
    }

    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        String returnID = getReturnID(file);
        List fields = MCRData2Fields.buildFields(file, index);
        addToIndex(entryID, returnID, fields);
    }

    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        String returnID = getReturnID(file);
        List fields = MCRData2Fields.buildFields(file, index);
        removeFromIndex(entryID);
        addToIndex(entryID, returnID, fields);
    }

    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        String entryID = file.getID();
        removeFromIndex(entryID);
    }

    protected void handleFileRepaired(MCREvent evt, MCRFile file) {
        handleFileUpdated(evt, file);
    }

    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        List fields = MCRData2Fields.buildFields(obj, index);
        addToIndex(entryID, entryID, fields);
    }

    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        List fields = MCRData2Fields.buildFields(obj, index);
        removeFromIndex(entryID);
        addToIndex(entryID, entryID, fields);
    }

    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        String entryID = obj.getId().getId();
        removeFromIndex(entryID);
    }

    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleObjectUpdated(evt, obj);
    }
    
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
      handleObjectDeleted(evt, obj);
    }

    protected void undoObjectDeleted(MCREvent evt, MCRObject obj) {
      handleObjectCreated(evt, obj);
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
     * @param returnID
     *            the ID to return as result of a search (MCRHit ID)
     * @param fields
     *            a List of MCRFieldValue objects
     */
    public void addToIndex(String entryID, String returnID, List fields) {
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
    public void removeFromIndex(String entryID) {
    }

    /**
     * Executes a query on this searcher. The query MUST only refer to fields
     * that are managed by this searcher.
     * 
     * @param cond
     *            the query condition
     * @param maxResults
     *            the maximum number of results to return, 0 means all results
     * @param sortBy
     *            a not-null list of MCRSortBy sort criteria. The list is empty
     *            if the results should not be sorted
     * @param addSortData
     *            if false, backend should sort results itself while executing
     *            the query. If this is not possible or the parameter is true,
     *            backend should not sort the results itself, but only store the
     *            data of the fields in the sortBy list which are needed to sort
     *            later
     * @return the query results
     */
    public abstract MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData);

    /**
     * Adds field values needed for sorting for those hits that do not have sort
     * data set already. Subclasses must overwrite this method, otherwise
     * sorting results will not always work correctly. The default
     * implementation in this class does nothing.
     * 
     * @param hits
     *            the MCRHit objects that do not have sort data set
     * @param sortBy
     *            the MCRFieldDef fields that are sort criteria
     */
    public void addSortData(Iterator hits, List<MCRSortBy> sortBy) {
    }

    /**
     * Removes all entries from index.
     */
    public void clearIndex() {
    }
}
