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

package org.mycore.mets.model.converter;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jdom2.Document;
import org.mycore.common.MCRException;
import org.mycore.mets.model.Mets;
import org.mycore.mets.model.files.FLocat;
import org.mycore.mets.model.files.File;
import org.mycore.mets.model.files.FileGrp;
import org.mycore.mets.model.simple.MCRMetsPage;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;
import org.mycore.mets.model.struct.Area;
import org.mycore.mets.model.struct.Fptr;
import org.mycore.mets.model.struct.LOCTYPE;
import org.mycore.mets.model.struct.LogicalDiv;
import org.mycore.mets.model.struct.LogicalStructMap;
import org.mycore.mets.model.struct.PhysicalDiv;
import org.mycore.mets.model.struct.PhysicalStructMap;
import org.mycore.mets.model.struct.PhysicalSubDiv;
import org.mycore.mets.model.struct.Seq;
import org.mycore.mets.model.struct.SmLink;
import org.mycore.mets.model.struct.StructLink;

/**
 * This Class converts MCRMetsSimpleModel to JDOM XML.
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRSimpleModelXMLConverter {

    public static final String DEFAULT_PHYSICAL_TYPE = "page";

    public static final String PHYSICAL_ID_PREFIX = "phys_";

    public static final String LOGICAL_ID_PREFIX = "log_";

    /**
     * Converts MetsSimpleModel to XML
     * @param msm the MetsSimpleModel which should be converted
     * @return xml
     */
    public static Document toXML(MCRMetsSimpleModel msm) {
        Mets mets = new Mets();

        Hashtable<MCRMetsPage, String> pageIdMap = new Hashtable<>();
        Map<String, String> idToNewIDMap = new Hashtable<>();
        buildPhysicalPages(msm, mets, pageIdMap, idToNewIDMap);

        Hashtable<MCRMetsSection, String> sectionIdMap = new Hashtable<>();
        buildLogicalPages(msm, mets, sectionIdMap, idToNewIDMap);

        StructLink structLink = mets.getStructLink();
        msm.getSectionPageLinkList().stream()
            .map((metsLink) -> {
                MCRMetsSection section = metsLink.getFrom();
                MCRMetsPage page = metsLink.getTo();
                String fromId = sectionIdMap.get(section);
                String toId = pageIdMap.get(page);
                return new SmLink(fromId, toId);
            })
            .forEach(structLink::addSmLink);

        return mets.asDocument();
    }

    private static void buildPhysicalPages(MCRMetsSimpleModel msm, Mets mets, Map<MCRMetsPage, String> pageIdMap,
        Map<String, String> idToNewIDMap) {
        List<MCRMetsPage> pageList = msm.getMetsPageList();
        PhysicalStructMap structMap = (PhysicalStructMap) mets.getStructMap(PhysicalStructMap.TYPE);
        structMap.setDivContainer(
            new PhysicalDiv(PHYSICAL_ID_PREFIX + UUID.randomUUID(), PhysicalDiv.TYPE_PHYS_SEQ));

        for (MCRMetsPage page : pageList) {
            String id = page.getId();
            PhysicalSubDiv physicalSubDiv = new PhysicalSubDiv(id, DEFAULT_PHYSICAL_TYPE);
            String orderLabel = page.getOrderLabel();
            if (orderLabel != null) {
                physicalSubDiv.setOrderLabel(orderLabel);
            }
            String contentIds = page.getContentIds();
            if (contentIds != null) {
                physicalSubDiv.setContentids(contentIds);
            }

            structMap.getDivContainer().add(physicalSubDiv);
            pageIdMap.put(page, id);

            page.getFileList().forEach((simpleFile) -> {
                String href = simpleFile.getHref();
                String fileID = simpleFile.getId();
                String mimeType = simpleFile.getMimeType();
                String use = simpleFile.getUse();

                idToNewIDMap.put(simpleFile.getId(), fileID);
                File file = new File(fileID, mimeType);
                FLocat fLocat = new FLocat(LOCTYPE.URL, href);
                file.setFLocat(fLocat);

                FileGrp fileGrp = getFileGroup(mets, use);
                fileGrp.addFile(file);

                physicalSubDiv.add(new Fptr(fileID));
            });
        }
    }

    private static void buildLogicalPages(MCRMetsSimpleModel msm, Mets mets, Map<MCRMetsSection, String> sectionIdMap,
        Map<String, String> idToNewIDMap) {
        LogicalStructMap logicalStructMap = (LogicalStructMap) mets.getStructMap(LogicalStructMap.TYPE);
        MCRMetsSection rootSection = msm.getRootSection();
        String type = rootSection.getType();
        String label = rootSection.getLabel();
        String id = rootSection.getId();
        LogicalDiv logicalDiv = new LogicalDiv(id, type, label);
        sectionIdMap.put(rootSection, id);

        for (MCRMetsSection metsSection : rootSection.getMetsSectionList()) {
            buildLogicalSubDiv(metsSection, logicalDiv, sectionIdMap, idToNewIDMap);
        }
        logicalStructMap.setDivContainer(logicalDiv);
    }

    private static void buildLogicalSubDiv(MCRMetsSection metsSection, LogicalDiv parent,
        Map<MCRMetsSection, String> sectionIdMap, Map<String, String> idToNewIDMap) {
        String id = metsSection.getId();
        LogicalDiv logicalSubDiv = new LogicalDiv(id, metsSection.getType(), metsSection.getLabel());

        if (metsSection.getAltoLinks().size() > 0) {
            Fptr fptr = new Fptr();
            List<Seq> seqList = fptr.getSeqList();

            Seq seq = new Seq();
            seqList.add(seq);

            metsSection.getAltoLinks().forEach(al -> {
                Area area = new Area();
                seq.getAreaList().add(area);
                area.setBetype("IDREF");
                area.setBegin(al.getBegin());
                area.setEnd(al.getEnd());
                String oldID = al.getFile().getId();
                if (!idToNewIDMap.containsKey(oldID)) {
                    throw new MCRException("Could not get new id for: " + oldID);
                }
                area.setFileId(idToNewIDMap.get(oldID));
            });
            logicalSubDiv.getFptrList().add(fptr);
        }

        sectionIdMap.put(metsSection, id);
        parent.add(logicalSubDiv);
        int count = 1;
        for (MCRMetsSection section : metsSection.getMetsSectionList()) {
            buildLogicalSubDiv(section, logicalSubDiv, sectionIdMap, idToNewIDMap);
        }
    }

    private static FileGrp getFileGroup(Mets mets, String use) {
        FileGrp fileGroup = mets.getFileSec().getFileGroup(use);

        if (fileGroup == null) {
            fileGroup = new FileGrp(use);
            mets.getFileSec().addFileGrp(fileGroup);
        }

        return fileGroup;
    }

}
