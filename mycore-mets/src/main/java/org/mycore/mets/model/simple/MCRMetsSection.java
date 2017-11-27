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
import java.util.Collections;
import java.util.List;

public class MCRMetsSection {

    public MCRMetsSection() {
        this.metsSectionList = new ArrayList<>();
        this.altoLinks = new ArrayList<>();
    }

    public MCRMetsSection(String id, String type, String label, MCRMetsSection parent) {
        this();
        this.id = id;
        this.type = type;
        this.label = label;
        this.parent = parent;
    }

    private List<MCRMetsSection> metsSectionList;

    private String id;

    private String type;

    private String label;

    private List<MCRMetsAltoLink> altoLinks;

    private transient MCRMetsSection parent;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<MCRMetsSection> getMetsSectionList() {
        return Collections.unmodifiableList(metsSectionList);
    }

    public void addSection(MCRMetsSection section) {
        section.parent = this;
        this.metsSectionList.add(section);
    }

    public void removeSection(MCRMetsSection section) {
        section.parent = null;
        this.metsSectionList.remove(section);
    }

    public MCRMetsSection getParent() {
        return parent;
    }

    public void setParent(MCRMetsSection parent) {
        this.parent = parent;
    }

    public List<MCRMetsAltoLink> getAltoLinks() {
        return Collections.unmodifiableList(altoLinks);
    }

    public void setAltoLinks(List<MCRMetsAltoLink> altoLinks) {
        this.altoLinks = altoLinks;
    }

    public void addAltoLink(MCRMetsAltoLink link) {
        this.altoLinks.add(link);
    }

    public void removeAltoLink(MCRMetsAltoLink link) {
        this.altoLinks.remove(link);
    }

}
