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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategoryID;

/**
 * This holds information about file metadata that is stored in derivate xml
 * 
 * @author Thomas Scheffler (yagee)
 * @author shermann
 * @see MCRObjectDerivate
 */
public class MCRFileMetadata implements Comparable<MCRFileMetadata> {

    private Collection<MCRCategoryID> categories;

    private String urn, handle;

    private String name;

    public MCRFileMetadata(Element file) {
        this.name = file.getAttributeValue("name");
        this.urn = file.getChildText("urn");
        this.handle = file.getChildText("handle");

        List<Element> categoryElements = file.getChildren("category");
        this.categories = Collections.emptySet();
        if (!categoryElements.isEmpty()) {
            categories = new ArrayList<>(categoryElements.size());
            for (Element categElement : categoryElements) {
                MCRCategoryID categId = MCRCategoryID.fromString(categElement.getAttributeValue("id"));
                categories.add(categId);
            }
        }
    }

    public MCRFileMetadata() {
        this(null, null, null);
    }

    public MCRFileMetadata(String name, String urn, Collection<MCRCategoryID> categories) {
        super();
        setName(name);
        setUrn(urn);
        setHandle(null);
        setCategories(categories);
    }

    public MCRFileMetadata(String name, String urn, String handle, Collection<MCRCategoryID> categories) {
        this(name, urn, categories);
        setHandle(handle);
    }

    public Element createXML() {
        Element file = new Element("file");
        file.setAttribute("name", name);
        if (urn != null) {
            Element urn = new Element("urn");
            urn.setText(this.urn);
            file.addContent(urn);
        }
        if (handle != null) {
            Element handle = new Element("handle");
            handle.setText(this.handle);
            file.addContent(handle);
        }
        for (MCRCategoryID categid : categories) {
            Element category = new Element("category");
            category.setAttribute("id", categid.toString());
            file.addContent(category);
        }
        return file;
    }

    public Collection<MCRCategoryID> getCategories() {
        return Collections.unmodifiableCollection(categories);
    }

    public void setCategories(Collection<MCRCategoryID> categories) {
        if (categories == null || categories.isEmpty()) {
            this.categories = Collections.emptySet();
        } else {
            this.categories = new ArrayList<>(categories);
        }
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    /**
     * @return the handle
     */
    public String getHandle() {
        return handle;
    }

    /**
     * @param handle the handle to set
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(MCRFileMetadata o) {
        return this.name.compareTo(o.name);
    }
}
