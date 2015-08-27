package org.mycore.mets.model.converter;

import java.util.Hashtable;
import java.util.List;

import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsPage;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

import com.google.gson.GsonBuilder;

import static java.util.stream.Collectors.toList;

/**
 * This class converts JSON to MCRMetsSimpleModel.
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRJSONSimpleModelConverter {

    public static MCRMetsSimpleModel toSimpleModel(String model) {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(MCRMetsLink.class, new MCRMetsLinkTypeAdapter());
        gsonBuilder.setPrettyPrinting();


        MCRMetsSimpleModel metsSimpleModel = gsonBuilder.create().fromJson(model, MCRMetsSimpleModel.class);

        Hashtable<String, MCRMetsPage> idPageMap = new Hashtable<>();
        metsSimpleModel.getMetsPageList().stream().forEach(page -> {
            idPageMap.put(page.getId(), page);
        });

        Hashtable<String, MCRMetsSection> idSectionMap = new Hashtable<>();
        processSections(metsSimpleModel.getRootSection(), idSectionMap);

        List<MCRMetsLink> sectionPageLinkList = metsSimpleModel.getSectionPageLinkList();
        List<MCRMetsLink> metsLinks = sectionPageLinkList
                .stream()
                .map((link) -> {
                    if (link instanceof MCRMetsLinkTypeAdapter.MCRMetsLinkPlaceholder) {
                        MCRMetsLinkTypeAdapter.MCRMetsLinkPlaceholder placeholder = (MCRMetsLinkTypeAdapter.MCRMetsLinkPlaceholder) link;
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

    private static void processSections(MCRMetsSection current, Hashtable<String, MCRMetsSection> idSectionTable) {
        idSectionTable.put(current.getId(), current);
        current.getMetsSectionList().forEach((child) -> {
            child.setParent(current);
            processSections(child, idSectionTable);
        });
    }

}
