/*
 * $Id$
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

package org.mycore.mods;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRUtils;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSearcherFactory;
import org.mycore.services.fieldquery.data2fields.MCRIndexEntry;

/**
 * Extracts mods:name fields from a MODS document and
 * indexes name and GND ID in a separate index. 
 * That index can be queried to build an A-Z list of names.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMODSNameIndexer extends MCREventHandlerBase {

    /** Search field storing the GND ID */
    private MCRFieldDef SEARCHFIELD_GND;

    /** Search field storing the name as it should be searchable */
    private MCRFieldDef SEARCHFIELD_NAME_SEARCH;

    /** Search field storing the name as it should be displayed */
    private MCRFieldDef SEARCHFIELD_NAME_DISPLAY;

    /** Search field storing the MCRObjectID this entry belongs to */
    private MCRFieldDef SEARCHFIELD_OWNER;

    /** The name of the search index that stores all the names */
    private static final String INDEX_NAME = "modsname";

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        removeNames(obj);
        indexNames(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        removeNames(obj);
        indexNames(obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        removeNames(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        removeNames(obj);
        indexNames(obj);
    }

    @Override
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
        removeNames(obj);
    }

    @Override
    protected void undoObjectDeleted(MCREvent evt, MCRObject obj) {
        indexNames(obj);
    }

    protected boolean isOfTypeMODS(MCRObject obj) {
        return "mods".equals(obj.getId().getTypeId());
    }

    private MCRSearcher indexer;

    public MCRMODSNameIndexer() {
        super();
        indexer = MCRSearcherFactory.getSearcherForIndex(INDEX_NAME);
        SEARCHFIELD_GND = MCRFieldDef.getDef("idxNameGND");
        SEARCHFIELD_NAME_SEARCH = MCRFieldDef.getDef("idxNameSearch");
        SEARCHFIELD_NAME_DISPLAY = MCRFieldDef.getDef("idxNameDisplay");
        SEARCHFIELD_OWNER = MCRFieldDef.getDef("idxNameOwner");
    }

    private void indexNames(MCRObject obj) {
        if (!isOfTypeMODS(obj))
            return;

        MCRMODSWrapper wrapper = new MCRMODSWrapper(obj);
        for (Element name : wrapper.getElements("mods:name")) {
            String displayForm = getDisplayForm(name);
            if ((displayForm == null) || displayForm.trim().isEmpty())
                continue;

            String gnd = getGND(name);

            String returnID = buildID(displayForm, gnd);
            String entryID = obj.getId() + "_" + returnID;

            MCRIndexEntry entry = new MCRIndexEntry();
            entry.setReturnID(returnID);
            entry.setEntryID(entryID);
            
            entry.addValue(new MCRFieldValue(SEARCHFIELD_OWNER, obj.getId().toString()));
            entry.addValue(new MCRFieldValue(SEARCHFIELD_NAME_DISPLAY, displayForm));
            entry.addValue(new MCRFieldValue(SEARCHFIELD_NAME_SEARCH, displayForm.split("\\s")[0]));
            if (!gnd.isEmpty())
                entry.addValue(new MCRFieldValue(SEARCHFIELD_GND, gnd));
            
            indexer.addToIndex(entry);
        }
    }

    /** Extracts the GND ID from the value URI */
    private String getGND(Element modsName) {
        String valueURI = modsName.getAttributeValue("valueURI");
        if (valueURI == null)
            return "";

        return valueURI.contains("/gnd/") ? valueURI.substring(valueURI.lastIndexOf("/") + 1) : "";
    }

    /** 
     * Returns a display form from the given mods:name.
     * If there is a mods:displayForm field, that will be used.
     * Otherwise, mods:nameParts are concatenated to build a display form.
     * It is assumed that mods:displayForm is "lastName, firstName".
     */
    private String getDisplayForm(Element modsName) {
        String displayForm = modsName.getChildText("displayForm", MCRConstants.MODS_NAMESPACE);
        if ((displayForm == null) || (displayForm.isEmpty())) {
            String family = "", given = "";

            List<Element> nameParts = modsName.getChildren("namePart", MCRConstants.MODS_NAMESPACE);
            for (Element namePart : nameParts) {
                String type = namePart.getAttributeValue("type", "");
                if ("family".equals(type) || type.isEmpty())
                    family = namePart.getTextTrim();
                if ("given".equals(type))
                    given = namePart.getTextTrim();
            }

            displayForm = family + (given.isEmpty() ? "" : ", ") + given;
        }
        return displayForm;
    }

    /**
     * Builds a unique entry ID for the given name.
     * If GND is not empty, the GND will be used as unique ID.
     * Otherwise, a hash code of the name will be used.
     */
    private String buildID(String displayForm, String gnd) {
        if (!gnd.isEmpty())
            return gnd;
        try {
            return MCRUtils.getMD5Sum(new ByteArrayInputStream(displayForm.getBytes()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Removes all name entries belonging to the given MODS document
     */
    private void removeNames(MCRObject obj) {
        if (!(isOfTypeMODS(obj)))
            return;

        MCRQueryCondition cond = new MCRQueryCondition(SEARCHFIELD_OWNER, "=", obj.getId().toString());
        MCRQuery query = new MCRQuery(cond);
        MCRResults results = MCRQueryManager.search(query);

        List<String> entryIDs = new ArrayList<String>();
        for (int i = 0; i < results.getNumHits(); i++)
            entryIDs.add(results.getHit(i).getID());
        for (String entryID : entryIDs)
            indexer.removeFromIndex(entryID);
    }
}
