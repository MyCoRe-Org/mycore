package org.mycore.mets.model.simple;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple data structure to hold data from mets.xml.
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRMetsSimpleModel {

    /**
     * Creates a new empty MCRMetsSimpleModel.
     */
    public MCRMetsSimpleModel() {
        metsPageList = new ArrayList<>();
        sectionPageLinkList = new ArrayList<>();
    }

    private MCRMetsSection rootSection;

    private List<MCRMetsPage> metsPageList;

    public List<MCRMetsLink> sectionPageLinkList;

    public MCRMetsSection getRootSection() {
        return rootSection;
    }

    public void setRootSection(MCRMetsSection rootSection) {
        this.rootSection = rootSection;
    }

    public List<MCRMetsLink> getSectionPageLinkList() {
        return sectionPageLinkList;
    }

    public List<MCRMetsPage> getMetsPageList() {
        return metsPageList;
    }

}
