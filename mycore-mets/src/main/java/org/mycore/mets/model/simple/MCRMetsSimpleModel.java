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
