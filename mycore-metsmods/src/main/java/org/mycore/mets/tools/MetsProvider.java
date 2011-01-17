/*
 * $Revision: 3280 $ $Date: 2011-01-13 17:13:11 +0100 (Thu, 13 Jan 2011) $
 * $LastChangedBy: shermann $ Copyright 2010 - Thüringer Universitäts- und
 * Landesbibliothek Jena
 * 
 * Mets-Editor is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Mets-Editor is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Mets-Editor. If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools;

import java.io.IOException;
import java.util.Iterator;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.mycore.mets.misc.LogicalIdProvider;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.Div;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;
import org.mycore.mets.model.struct.SubDiv;

/**
 * The class creates {@link Mets} from {@link JSONObject} objects
 * 
 * @author Silvio Hermann (shermann)
 */
public class MetsProvider {

    final private static Logger LOGGER = Logger.getLogger(MetsProvider.class);

    private Mets mets;

    private DmdSec dmdSec;

    private AmdSec amdSec;

    private FileSec fileSec;

    private FileGrp fileGrpMaster;

    private PhysicalStructMap physicalStructMp;

    private LogicalStructMap logicalStructMp;

    private StructLink structLink;

    private LogicalIdProvider idProvider;

    /** Sets up the underlying mets object */
    public MetsProvider(String derivate) {
        this.idProvider = new LogicalIdProvider("log", 6);
        mets = new Mets();
        dmdSec = new DmdSec("dmd_" + derivate);

        amdSec = new AmdSec("amd_" + derivate);
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);

        // filesec
        fileSec = new FileSec();
        fileGrpMaster = new FileGrp(FileGrp.USE_MASTER);
        fileSec.addFileGrp(fileGrpMaster);

        mets.setFileSec(fileSec);

        /* init the two structure maps */
        /* init logical structure map */
        logicalStructMp = new LogicalStructMap();
        Div logDivContainer = new Div("log_" + derivate, dmdSec.getId(), amdSec.getId(), "monograph", "Label for " + derivate);
        logDivContainer.setOrder(1);
        logicalStructMp.setDivContainer(logDivContainer);

        /* init physical structure map */
        physicalStructMp = new PhysicalStructMap();
        Div physDivContainer = new Div("phys_" + dmdSec.getId(), Div.TYPE_PHYS_SEQ);
        physicalStructMp.setDivContainer(physDivContainer);

        /* add the different structure maps */
        mets.setPysicalStructMap(physicalStructMp);
        mets.setLogicalStructMap(logicalStructMp);

        /* add the struct link section */
        structLink = new StructLink();
        mets.setStructLink(structLink);
    }

    @SuppressWarnings("unchecked")
    public Mets toMets(JSONObject source) throws JDOMException, IOException {
        // these are derivate id and probably a name/label
        String name = source.get("name").toString();
        String docType = source.get("structureType").toString();

        this.logicalStructMp.getDivContainer().setLabel(JSONTools.stripBracketsAndQuotes(name));
        this.logicalStructMp.getDivContainer().setDocumentType(JSONTools.stripBracketsAndQuotes(docType));

        // the children of the derivate should be present
        JSONArray children = null;
        try {
            children = source.getJSONArray("children");
        } catch (JSONException ex) {
            LOGGER.error("No children attribute found and thus not a valid json format for mets.");
            return null;
        }

        process((Iterator<JSONObject>) children.iterator(), null);

        return this.mets;
    }

    /**
     * @param parent
     *            a JSONObject with children attribut
     */
    @SuppressWarnings("unchecked")
    private void process(Iterator<JSONObject> it, SubDiv parentDiv) {
        while (it.hasNext()) {
            JSONObject json = it.next();
            String id = JSONTools.stripBracketsAndQuotes(json.getString("id"));
            String label = JSONTools.stripBracketsAndQuotes(json.getString("name"));
            String structType = JSONTools.stripBracketsAndQuotes(json.getString("structureType"));
            int logicalOrder = json.getInt("logicalOrder");
            /*
             * current json object is a structure (as defined in
             * StructureModel.js) and not actually a file/image
             */
            if (json.has("children")) {
                SubDiv logDiv = new SubDiv(idProvider.getNextId(), structType, logicalOrder, label);

                if (parentDiv != null) {
                    parentDiv.addLogicalDiv(logDiv);
                    // reference to 1st file (file may be in a sub structure)
                    /* TODO remove condition once it is needed */
                    if (false) {
                        String firstFileId = getFirstFileId(json.getJSONArray("children").iterator());
                        if (firstFileId != null) {
                            SubDiv physical = new SubDiv(firstFileId, null, -1, null);
                            structLink.addSmLink(new SmLink(parentDiv, physical));
                        }
                    }
                } else {
                    logicalStructMp.getDivContainer().addSubDiv(logDiv);
                }
                process((Iterator<JSONObject>) json.getJSONArray("children").iterator(), logDiv);
            }
            /*
             * current json object is a file/image, thus we must register it in
             * the different fileSec objects
             */
            else {
                String path = JSONTools.stripBracketsAndQuotes(json.getString("path"));
                int physicalOrder = json.getInt("physicalOrder");
                String orderLabel = JSONTools.stripBracketsAndQuotes(json.getString("orderLabel"));

                SubDiv physical = addFileToPhysicalStructMp(id, path, physicalOrder, orderLabel);
                addFileToGroups(id, path, label);

                /* create div in log struct map and add the symlink */
                if (parentDiv != null) {
                    structLink.addSmLink(new SmLink(parentDiv, physical));
                } else {
                    SmLink link = new SmLink(logicalStructMp.getDivContainer().asLogicalSubDiv(), physical);
                    structLink.addSmLink(link);
                }
            }
        }

    }

    /**
     * Method traverses through the objects in the iterator and looks for a json
     * object representing a file. The id of the first file found is returned.
     * 
     * @param iterator
     * @return the id of the first file in the hierarchy or <code>null</code> if
     *         there is no such file
     */
    @SuppressWarnings("unchecked")
    private String getFirstFileId(Iterator<JSONObject> iterator) {
        String fileId = null;
        while (iterator.hasNext()) {
            JSONObject json = iterator.next();
            if (json.has("children")) {
                return getFirstFileId(json.getJSONArray("children").iterator());
            }

            String structType = JSONTools.stripBracketsAndQuotes(json.getString("structureType"));
            if ("page".equals(structType)) {
                fileId = JSONTools.stripBracketsAndQuotes(json.getString("id"));
                return SubDiv.ID_PREFIX + fileId;
            }
        }
        return fileId;
    }

    /**
     * Adds the file with the given id to the physical struct map
     * 
     * @param id
     *            the id of the file
     * @param physicalOrder
     *            the position of the file in the sequence of all files
     * @return the {@link SubDiv}
     */
    private SubDiv addFileToPhysicalStructMp(String id, String path, int physicalOrder, String orderLabel) {
        Div divContainer = physicalStructMp.getDivContainer();
        String idStripped = JSONTools.stripBracketsAndQuotes(id);

        SubDiv subDiv = new SubDiv(SubDiv.ID_PREFIX + idStripped, SubDiv.TYPE_PAGE, physicalOrder, true);
        subDiv.setOrderLabel(orderLabel);

        subDiv.addFptr(new Fptr(id));
        divContainer.addSubDiv(subDiv);

        return subDiv;
    }

    /**
     * Adds the file to the 3 goups (default,min and max) and sets the FLocat
     * object to the files
     * 
     * @param id
     *            the id of the file
     * @param name
     *            the name/label of the file
     */
    private void addFileToGroups(String id, String path, String name) {
        String idStripped = JSONTools.stripBracketsAndQuotes(id);
        String nameStripped = JSONTools.stripBracketsAndQuotes(name);
        File file = null;

        file = new File(idStripped, nameStripped, File.MIME_TYPE_TIFF);
        file.setFLocat(new FLocat(FLocat.LOCTYPE_URL, path));
        fileGrpMaster.addFile(file);
    }
}