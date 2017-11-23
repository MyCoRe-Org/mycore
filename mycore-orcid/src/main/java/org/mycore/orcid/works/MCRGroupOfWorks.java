/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid.works;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.mods.merger.MCRMergeTool;

/**
 * Represents a group of works as the activities:group element returned in the ORCID response to fetch work summaries.
 * ORCID groups mulitple works from different sources that are assumed to represent the same publication.
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
     * The MODS from each work are merged together.
     */
    public Element buildMergedMODS() {
        Element mods = works.get(0).getMODS().clone();
        for (int i = 1; i < works.size(); i++) {
            MCRMergeTool.merge(mods, works.get(i).getMODS());
        }
        return mods;
    }
}
