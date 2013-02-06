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
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.mets.misc.LogicalIdProvider;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.files.FileSec;
import org.mycore.mets.model.sections.AmdSec;
import org.mycore.mets.model.sections.DmdSec;
import org.mycore.mets.model.struct.AbstractLogicalDiv;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.LogicalSubDiv;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;

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

    private FileGrp fileGrpMaster;

    private PhysicalStructMap physicalStructMp;

    private LogicalStructMap logicalStructMp;

    private StructLink structLink;

    private LogicalIdProvider idProvider;

    /** Sets up the underlying mets object */
    public MCRMetsProvider(String derivate) {
        this.idProvider = new LogicalIdProvider("log", 6);
        mets = new Mets();
        DmdSec dmdSec = new DmdSec("dmd_" + derivate);

        AmdSec amdSec = new AmdSec("amd_" + derivate);
        mets.addDmdSec(dmdSec);
        mets.addAmdSec(amdSec);

        // filesec
        FileSec fileSec = new FileSec();
        fileGrpMaster = new FileGrp(FileGrp.USE_MASTER);
        fileSec.addFileGrp(fileGrpMaster);

        mets.setFileSec(fileSec);

        /* init the two structure maps */
        /* init logical structure map */
        logicalStructMp = new LogicalStructMap();
        LogicalDiv logDivContainer = new LogicalDiv("log_" + derivate, "monograph", "Label for " + derivate, 1, amdSec.getId(),
                dmdSec.getId());
        logicalStructMp.setDivContainer(logDivContainer);

        /* init physical structure map */
        physicalStructMp = new PhysicalStructMap();
        PhysicalDiv physDivContainer = new PhysicalDiv("phys_" + dmdSec.getId(), PhysicalDiv.TYPE_PHYS_SEQ);
        physicalStructMp.setDivContainer(physDivContainer);

        /* add the different structure maps */
        mets.addStructMap(physicalStructMp);
        mets.addStructMap(logicalStructMp);

        /* add the struct link section */
        structLink = new StructLink();
        mets.setStructLink(structLink);
    }

    public Mets toMets(JsonObject source) throws JDOMException, IOException {
        // these are derivate id and probably a name/label
        String name = source.get("name").toString();
        String docType = source.get("structureType").toString();

        this.logicalStructMp.getDivContainer().setLabel(MCRJSONTools.stripBracketsAndQuotes(name));
        this.logicalStructMp.getDivContainer().setType(MCRJSONTools.stripBracketsAndQuotes(docType));

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
    private void process(Iterator<JsonElement> it, AbstractLogicalDiv parentDiv) {
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
                LogicalSubDiv logDiv = new LogicalSubDiv(idProvider.getNextId(), structType, label, logicalOrder);
                if (parentDiv != null) {
                    parentDiv.add(logDiv);
                } else {
                    logicalStructMp.getDivContainer().add(logDiv);
                }
                process(json.get("children").getAsJsonArray().iterator(), logDiv);
            }
            /*
             * current json object is a file/image, thus we must register it in
             * the different fileSec objects
             */
            else {
                String path = MCRJSONTools.stripBracketsAndQuotes(json.get("path").getAsString());
                boolean hide = json.get("hide").getAsBoolean();

                /* create the file object and add it to the file section */
                File file = createFile(id, path);
                fileGrpMaster.addFile(file);

                // only files with hide attribute == false appear in the structlink section and in the physical struct map
                if (!hide) {
                    int physicalOrder = json.get("physicalOrder").getAsInt();
                    String orderLabel = MCRJSONTools.stripBracketsAndQuotes(json.get("orderLabel").getAsString());
                    orderLabel = orderLabel.equals("undefined") ? "" : orderLabel;
                    path = encode(path);

                    /* create the physical div and add it to the physical struct map */
                    PhysicalSubDiv physDiv = createPhysicalDiv(id, physicalOrder, orderLabel);
                    physicalStructMp.getDivContainer().add(physDiv);

                    /* create div in log struct map and add the symlink */
                    if (parentDiv != null) {
                        structLink.addSmLink(new SmLink(parentDiv.getId(), physDiv.getId()));
                    } else {
                        structLink.addSmLink(new SmLink(logicalStructMp.getDivContainer().getId(), physDiv.getId()));
                    }
                }
            }
        }
    }

    /**
     * Creates a {@link PhysicalSubDiv}.
     * 
     * @param id
     *            the id of the file
     * @param physicalOrder
     *            the position of the file in the sequence of all files
     * @param orderLabel
     *            the order label of the file
     */
    private PhysicalSubDiv createPhysicalDiv(String id, int physicalOrder, String orderLabel) {
        String idStripped = MCRJSONTools.stripBracketsAndQuotes(id);
        PhysicalSubDiv physDiv = new PhysicalSubDiv(PhysicalSubDiv.ID_PREFIX + idStripped, PhysicalSubDiv.TYPE_PAGE, physicalOrder,
                orderLabel);
        physDiv.add(new Fptr(id));
        return physDiv;
    }

    /**
     * Creates a {@link File} object and sets the FLocat object to the files
     * 
     * @param id
     *            the id of the file
     * @param path
     *            the path of the image relative to the derivate root, must be
     *            encoded
     */
    private File createFile(String id, String path) {
        String idStripped = MCRJSONTools.stripBracketsAndQuotes(id);
        File file = null;
        file = new File(idStripped, File.MIME_TYPE_TIFF);
        file.setFLocat(new FLocat(LOCTYPE.URL, path));

        return file;
    }

    /**
     * @param source
     *            the string to decode (url decode)
     * @return the input string decoded or null if the input string is null
     */
    private String encode(String source) {
        if (source == null) {
            return null;
        }
        try {
            return MCRXMLFunctions.encodeURIPath(source);
        } catch (URISyntaxException ex) {
            LOGGER.error("Error occured while decoding source string \"" + source + "\"", ex);
            return null;
        }
    }
}
