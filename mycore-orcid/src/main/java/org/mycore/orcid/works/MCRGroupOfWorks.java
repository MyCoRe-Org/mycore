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

package org.mycore.orcid.works;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.mods.merger.MCRMergeTool;

/**
 * Represents a group of works as the activities:group element returned in the ORCID response to fetch work summaries.
 * ORCID groups multiple works from different sources that are assumed to represent the same publication.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRGroupOfWorks {

    private List<MCRWork> works = new ArrayList<>();

    void add(MCRWork work) {
        works.add(work);
    }

    /**
     * Returns the works grouped together here.
     * All these work entries are assumed to represent the same publication, but from different sources.
     */
    public List<MCRWork> getWorks() {
        return works;
    }

    /**
     * Returns a single mods:mods representation of the publication represented by this group.
     * The MODS from each is merged together.
     */
    public Element buildMergedMODS() {
        Element mods = works.get(0).getMODS().clone();
        for (int i = 1; i < works.size(); i++) {
            MCRMergeTool.merge(mods, works.get(i).getMODS());
        }
        return mods;
    }
}
