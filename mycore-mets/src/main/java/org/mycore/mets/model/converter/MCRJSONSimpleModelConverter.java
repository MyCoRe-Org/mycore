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

package org.mycore.mets.model.converter;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mycore.common.MCRException;
import org.mycore.mets.model.MCRMetsModelHelper;
import org.mycore.mets.model.converter.MCRAltoLinkTypeAdapter.MCRAltoLinkPlaceHolder;
import org.mycore.mets.model.converter.MCRMetsLinkTypeAdapter.MCRMetsLinkPlaceholder;
import org.mycore.mets.model.simple.MCRMetsAltoLink;
import org.mycore.mets.model.simple.MCRMetsFile;
import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsPage;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

import com.google.gson.GsonBuilder;

/**
 * This class converts JSON to MCRMetsSimpleModel.
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRJSONSimpleModelConverter {

    public static MCRMetsSimpleModel toSimpleModel(String model) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(MCRMetsLink.class, new MCRMetsLinkTypeAdapter());
        gsonBuilder.registerTypeAdapter(MCRMetsAltoLink.class, new MCRAltoLinkTypeAdapter());
        gsonBuilder.setPrettyPrinting();

        MCRMetsSimpleModel metsSimpleModel = gsonBuilder.create().fromJson(model, MCRMetsSimpleModel.class);

        Map<String, MCRMetsPage> idPageMap = new HashMap<>();
        metsSimpleModel.getMetsPageList().stream().forEach(page -> idPageMap.put(page.getId(), page));

        final Map<String, MCRMetsFile> idMCRMetsFileMap = extractIdFileMap(metsSimpleModel.getMetsPageList());

        Map<String, MCRMetsSection> idSectionMap = new HashMap<>();
        processSections(metsSimpleModel.getRootSection(), idSectionMap, idMCRMetsFileMap);

        List<MCRMetsLink> sectionPageLinkList = metsSimpleModel.getSectionPageLinkList();
        List<MCRMetsLink> metsLinks = sectionPageLinkList
            .stream()
            .map((link) -> {
                if (link instanceof MCRMetsLinkPlaceholder placeholder) {
                    MCRMetsSection metsSection = idSectionMap.get(placeholder.getFromString());
                    MCRMetsPage metsPage = idPageMap.get(placeholder.getToString());
                    return new MCRMetsLink(metsSection, metsPage);
                } else {
                    return link;
                }
            }).collect(toList());

        sectionPageLinkList.clear();
        sectionPageLinkList.addAll(metsLinks);

        return metsSimpleModel;
    }

    private static Map<String, MCRMetsFile> extractIdFileMap(List<MCRMetsPage> pages) {
        final Map<String, MCRMetsFile> idFileMap = new HashMap<>();
        pages.forEach(p -> p.getFileList().stream()
            .filter(file -> file.getUse().equals(MCRMetsModelHelper.ALTO_USE))
            .forEach(file -> idFileMap.put(file.getId(), file)));

        return idFileMap;
    }

    private static void processSections(MCRMetsSection current, Map<String, MCRMetsSection> idSectionTable,
        Map<String, MCRMetsFile> idFileMap) {
        idSectionTable.put(current.getId(), current);

        final List<MCRMetsAltoLink> altoLinks = current.getAltoLinks().stream().map(altoLink -> {
            if (altoLink instanceof MCRAltoLinkPlaceHolder ph) {
                if (!idFileMap.containsKey(ph.getFileID())) {
                    throw new MCRException(
                        "Could not parse link from section to alto! (FileID of alto not found in file list)");
                }
                return new MCRMetsAltoLink(idFileMap.get(ph.getFileID()), ph.getBegin(), ph.getEnd());
            }
            return altoLink;
        }).collect(toList());

        current.setAltoLinks(altoLinks);

        current.getMetsSectionList().forEach((child) -> {
            child.setParent(current);
            processSections(child, idSectionTable, idFileMap);
        });
    }

}
