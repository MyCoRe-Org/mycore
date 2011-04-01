/*
 * $Revision$ $Date$
 * $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- und
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The class creates {@link Mets} from {@link JsonObject} objects
 * 
 * @author Silvio Hermann (shermann)
 */
public class MCRMetsProvider {

    final private static Logger LOGGER = Logger.getLogger(MCRMetsProvider.class);

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
    public MCRMetsProvider(String derivate) {
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
    public Mets toMets(JsonObject source) throws JDOMException, IOException {
        // these are derivate id and probably a name/label
        String name = source.get("name").toString();
        String docType = source.get("structureType").toString();

        this.logicalStructMp.getDivContainer().setLabel(MCRJSONTools.stripBracketsAndQuotes(name));
        this.logicalStructMp.getDivContainer().setDocumentType(MCRJSONTools.stripBracketsAndQuotes(docType));

        // the children of the derivate should be present
        JsonArray children = (JsonArray) source.get("children");
        if (children == null) {
            LOGGER.error("No children attribute found and thus not a valid json format for mets.");
            return null;
        }

        process((Iterator<JsonElement>) children.iterator(), null);

        return this.mets;
    }

    /**
     * @param parent
     *            a JsonObject with children attribut
     */
    @SuppressWarnings("unchecked")
    private void process(Iterator<JsonElement> it, SubDiv parentDiv) {
        while (it.hasNext()) {
            JsonObject json = it.next().getAsJsonObject();
            String id = MCRJSONTools.stripBracketsAndQuotes(json.get("id").getAsString());
            String label = MCRJSONTools.stripBracketsAndQuotes(json.get("name").getAsString());
            String structType = MCRJSONTools.stripBracketsAndQuotes(json.get("structureType").getAsString());
            int logicalOrder = json.get("logicalOrder").getAsInt();
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
                        String firstFileId = getFirstFileId(json.get("children").getAsJsonArray().iterator());
                        if (firstFileId != null) {
                            SubDiv physical = new SubDiv(firstFileId, null, -1, null);
                            structLink.addSmLink(new SmLink(parentDiv, physical));
                        }
                    }
                } else {
                    logicalStructMp.getDivContainer().addSubDiv(logDiv);
                }
                process(json.get("children").getAsJsonArray().iterator(), logDiv);
            }
            /*
             * current json object is a file/image, thus we must register it in
             * the different fileSec objects
             */
            else {
                String path = MCRJSONTools.stripBracketsAndQuotes(json.get("path").getAsString());
                int physicalOrder = json.get("physicalOrder").getAsInt();
                String orderLabel = MCRJSONTools.stripBracketsAndQuotes(json.get("orderLabel").getAsString());

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
    private String getFirstFileId(Iterator<JsonElement> iterator) {
        String fileId = null;
        while (iterator.hasNext()) {
            JsonObject json = iterator.next().getAsJsonObject();
            if (json.has("children")) {
                return getFirstFileId(json.get("children").getAsJsonArray().iterator());
            }

            String structType = MCRJSONTools.stripBracketsAndQuotes(json.get("structureType").getAsString());
            if ("page".equals(structType)) {
                fileId = MCRJSONTools.stripBracketsAndQuotes(json.get("id").getAsString());
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
        String idStripped = MCRJSONTools.stripBracketsAndQuotes(id);

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
        String idStripped = MCRJSONTools.stripBracketsAndQuotes(id);
        String nameStripped = MCRJSONTools.stripBracketsAndQuotes(name);
        File file = null;

        file = new File(idStripped, nameStripped, File.MIME_TYPE_TIFF);
        file.setFLocat(new FLocat(FLocat.LOCTYPE_URL, path));
        fileGrpMaster.addFile(file);
    }
}
