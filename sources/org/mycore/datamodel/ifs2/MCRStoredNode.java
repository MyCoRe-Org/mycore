/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.VFS;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;

/**
 * A file or directory really stored by importing it from outside the system.
 * Can be modified, updated and deleted, in contrast to virtual nodes.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRStoredNode extends MCRNode {
    private final static String defaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "en");

    /**
     * The optional labels of this node, in multiple languages. Key is the
     * xml:lang language ID, value is the label in that language.
     */
    protected SortedMap<String, String> labels;

    /**
     * Creates a new stored node instance
     * 
     * @param parent
     *            the parent directory containing this node
     * @param fo
     *            the file object in local filesystem representing this node
     */
    protected MCRStoredNode(MCRDirectory parent, FileObject fo) throws Exception {
        super(parent, fo);
        labels = new TreeMap<String, String>();
    }

    /**
     * Deletes this node with all its data and children
     */
    public void delete() throws Exception {
        if (parent != null)
            ((MCRDirectory) parent).removeMetadata(this);
    }

    /**
     * Called when metadata of this node was changed, to notify the parent
     * directory to update its own metadata file
     */
    protected void updateMetadata() throws Exception {
        if (parent != null)
            ((MCRDirectory) parent).updateMetadata(this.getName(), this);
    }

    /**
     * Writes metadata of this node to the XML element given.
     * 
     * @param entry
     *            the XML element holding the metadata of this node
     */
    protected void writeChildData(Element entry) throws Exception {
        entry.setAttribute("name", this.getName());

        entry.setAttribute("numChildren", String.valueOf(this.getNumChildren()));

        MCRMetaISO8601Date date = new MCRMetaISO8601Date();
        date.setDate(this.getLastModified());
        date.setFormat(MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS);
        entry.setAttribute("lastModified", date.getISOString());

        entry.removeChildren("label");
        if (!labels.isEmpty()) {
            Iterator<String> it = labels.keySet().iterator();
            while (it.hasNext()) {
                String lang = it.next();
                String label = labels.get(lang);
                entry.addContent(new Element("label").setAttribute("lang", lang).setText(label));
            }
        }
    }

    /**
     * Reads metadata of this node from a stored XML element coming from the
     * parent directory
     * 
     * @param entry
     *            the XML element holding this node's metadata
     */
    protected void readChildData(Element entry) throws Exception {
        labels.clear();
        for (Element label : (List<Element>) (entry.getChildren("label")))
            labels.put(label.getAttributeValue("lang"), label.getTextTrim());
    }

    /**
     * Repairs metadata of this node by rebuilding it from the underlying
     * filesystem
     */
    protected abstract void repairMetadata() throws Exception;

    /**
     * Renames this node.
     * 
     * @param name
     *            the new file name
     */
    public void renameTo(String name) throws Exception {
        String oldName = getName();
        FileObject fNew = VFS.getManager().resolveFile(fo.getParent(), name);
        fo.moveTo(fNew);
        fo = fNew;

        if (parent != null)
            ((MCRDirectory) parent).updateMetadata(oldName, this);
    }

    /**
     * Sets a label for this node
     * 
     * @param lang
     *            the xml:lang language ID
     * @param label
     *            the label in this language
     */
    public void setLabel(String lang, String label) throws Exception {
        labels.put(lang, label);
        updateMetadata();
    }

    /**
     * Removes all labels set
     */
    public void clearLabels() throws Exception {
        labels.clear();
        updateMetadata();
    }

    /**
     * Returns all labels, sorted by xml:lang
     */
    public SortedMap<String, String> getLabels() {
        return labels;
    }

    /**
     * Returns the label in the given language
     * 
     * @param lang
     *            the xml:lang langauge ID
     * @return the label, or null if there is no label for that language
     */
    public String getLabel(String lang) {
        return labels.get(lang);
    }

    /**
     * Returns the label in the current language, otherwise in default language,
     * otherwise the first label defined, if any at all.
     * 
     * @return the label
     */
    public String getCurrentLabel() {
        if (labels.isEmpty())
            return null;
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = labels.get(lang);
        if (label != null)
            return label;
        label = labels.get(defaultLang);
        if (label != null)
            return label;
        return labels.get(labels.firstKey());
    }
}
