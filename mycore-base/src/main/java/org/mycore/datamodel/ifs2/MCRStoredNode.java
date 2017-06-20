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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;

/**
 * A file or directory really stored by importing it from outside the system.
 * Can be modified, updated and deleted, in contrast to virtual nodes.
 * 
 * @author Frank Lützenkirchen
 */
public abstract class MCRStoredNode extends MCRNode {

    private static final String LANG_ATT = "lang";

    private static final String LABEL_ELEMENT = "label";

    private static final String NAME_ATT = "name";

    /**
     * Any additional data of this node that is not stored in the file object
     */
    protected Element data;

    /**
     * Returns a stored node instance that already exists
     * 
     * @param parent
     *            the parent directory containing this node
     * @param fo
     *            the file object in local filesystem representing this node
     * @param data
     *            the additional data of this node that is not stored in the
     *            file object
     */
    protected MCRStoredNode(MCRDirectory parent, FileObject fo, Element data) throws IOException {
        super(parent, fo);
        this.data = data;
    }

    /**
     * Creates a new stored node
     * 
     * @param parent
     *            the parent directory
     * @param name
     *            the name of the node
     * @param type
     *            the node type, dir or file
     */
    protected MCRStoredNode(MCRDirectory parent, String name, String type) throws IOException {
        super(parent, VFS.getManager().resolveFile(parent.fo, name));
        data = new Element(type);
        data.setAttribute(NAME_ATT, name);
        parent.data.addContent(data);
    }

    /**
     * Returns the local java.io.File representing this stored file or directory. Be careful
     * to use this only for reading data, do never modify directly!
     * 
     * @return the file in the local filesystem representing this file
     */
    public File getLocalFile() throws IOException {
        if (fo instanceof LocalFile) {
            return new File(fo.getURL().getPath());
        } else {
            return null;
        }
    }

    /**
     * Deletes this node with all its data and children
     */
    public void delete() throws IOException {
        data.detach();
        fo.delete(Selectors.SELECT_ALL);
        getRoot().saveAdditionalData();
    }

    /**
     * Renames this node.
     * 
     * @param name
     *            the new file name
     */
    public void renameTo(String name) throws IOException {
        FileObject fNew = VFS.getManager().resolveFile(fo.getParent(), name);
        fo.moveTo(fNew);
        fo = fNew;
        fo.getContent().setLastModifiedTime(System.currentTimeMillis());
        data.setAttribute(NAME_ATT, name);
        getRoot().saveAdditionalData();
    }

    /**
     * Sets last modification time of this file to a custom value.
     * 
     * @param time
     *            the time to be stored as last modification time
     */
    public void setLastModified(Date time) throws IOException {
        fo.getContent().setLastModifiedTime(time.getTime());
    }

    /**
     * Sets a label for this node
     * 
     * @param lang
     *            the xml:lang language ID
     * @param label
     *            the label in this language
     */
    public void setLabel(String lang, String label) throws IOException {

        data.getChildren(LABEL_ELEMENT)
            .stream()
            .filter(child -> lang.equals(
                child.getAttributeValue(LANG_ATT,
                    Namespace.XML_NAMESPACE)))
            .findAny()
            .orElseGet(() -> {
                Element newLabel = new Element(LABEL_ELEMENT).setAttribute(LANG_ATT, lang, Namespace.XML_NAMESPACE);
                data.addContent(newLabel);
                return newLabel;
            })
            .setText(label);
        getRoot().saveAdditionalData();
    }

    /**
     * Removes all labels set
     */
    public void clearLabels() throws IOException {
        data.removeChildren(LABEL_ELEMENT);
        getRoot().saveAdditionalData();
    }

    /**
     * Returns a map of all labels, sorted by xml:lang, Key is xml:lang, value
     * is the label for that language.
     */
    public Map<String, String> getLabels() {
        Map<String, String> labels = new TreeMap<String, String>();
        for (Element label : (List<Element>) data.getChildren(LABEL_ELEMENT)) {
            labels.put(label.getAttributeValue(LANG_ATT, Namespace.XML_NAMESPACE), label.getText());
        }
        return labels;
    }

    /**
     * Returns the label in the given language
     * 
     * @param lang
     *            the xml:lang language ID
     * @return the label, or null if there is no label for that language
     */
    public String getLabel(String lang) {
        return data.getChildren(LABEL_ELEMENT)
            .stream()
            .filter(label -> lang.equals(
                label.getAttributeValue(LANG_ATT, Namespace.XML_NAMESPACE)))
            .findAny()
            .map(Element::getText)
            .orElse(null);
    }

    /**
     * Returns the label in the current language, otherwise in default language,
     * otherwise the first label defined, if any at all.
     * 
     * @return the label
     */
    public String getCurrentLabel() {
        String currentLang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        String label = getLabel(currentLang);
        if (label != null) {
            return label;
        }

        String defaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang",
            MCRConstants.DEFAULT_LANG);
        label = getLabel(defaultLang);
        if (label != null) {
            return label;
        }

        return data.getChildText(LABEL_ELEMENT);
    }

    /**
     * Repairs additional metadata of this node
     */
    abstract void repairMetadata() throws IOException;
}
