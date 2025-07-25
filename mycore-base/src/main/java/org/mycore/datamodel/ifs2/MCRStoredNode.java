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

package org.mycore.datamodel.ifs2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRDefaultFileAttributes;
import org.mycore.datamodel.niofs.MCRFileAttributes;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;

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
    private Element additionalData;

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
    protected MCRStoredNode(MCRDirectory parent, Path fo, Element data) {
        super(parent, fo);
        this.additionalData = data;
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
    protected MCRStoredNode(MCRDirectory parent, String name, String type) {
        super(parent, parent.path.resolve(name));
        additionalData = new Element(type);
        additionalData.setAttribute(NAME_ATT, name);
        parent.writeData(e -> e.addContent(additionalData));
    }

    /**
     * Returns the local {@link Path} representing this stored file or directory. Be careful
     * to use this only for reading data, do never modify directly!
     *
     * @return the file in the local filesystem representing this file
     */
    public Path getLocalPath() {
        return path;
    }

    /**
     * Deletes this node with all its data and children
     */
    public void delete() throws IOException {
        writeData(Element::detach);
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new MCRRecursiveDeleter());
        } else {
            Files.deleteIfExists(path);
        }
        getRoot().saveAdditionalData();
    }

    /**
     * Renames this node.
     * 
     * @param name
     *            the new file name
     */
    public void renameTo(String name) throws IOException {
        Path oldPath = path;
        Path newPath = path.resolveSibling(name);
        Files.move(oldPath, newPath);
        Files.setLastModifiedTime(newPath, FileTime.from(Instant.now()));
        path = newPath;
        writeData(e -> e.setAttribute(NAME_ATT, name));
        getRoot().saveAdditionalData();
    }

    /**
     * Sets last modification time of this file to a custom value.
     * 
     * @param time
     *            the time to be stored as last modification time
     */
    public void setLastModified(Date time) throws IOException {
        Files.setLastModifiedTime(path, FileTime.from(time.toInstant()));
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
        writeData(e -> e.getChildren(LABEL_ELEMENT)
            .stream()
            .filter(child -> lang.equals(child.getAttributeValue(LANG_ATT, Namespace.XML_NAMESPACE)))
            .findAny()
            .orElseGet(() -> {
                Element newLabel = new Element(LABEL_ELEMENT).setAttribute(LANG_ATT, lang, Namespace.XML_NAMESPACE);
                e.addContent(newLabel);
                return newLabel;
            })
            .setText(label));
        getRoot().saveAdditionalData();
    }

    /**
     * Removes all labels set
     */
    public void clearLabels() throws IOException {
        writeData(e -> e.removeChildren(LABEL_ELEMENT));
        getRoot().saveAdditionalData();
    }

    /**
     * Returns a map of all labels, sorted by xml:lang, Key is xml:lang, value
     * is the label for that language.
     */
    public Map<String, String> getLabels() {
        Map<String, String> labels = new TreeMap<>();
        for (Element label : readData(e -> e.getChildren(LABEL_ELEMENT))) {
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
        return readData(e -> e.getChildren(LABEL_ELEMENT)
            .stream()
            .filter(label -> lang.equals(
                label.getAttributeValue(LANG_ATT, Namespace.XML_NAMESPACE)))
            .findAny()
            .map(Element::getText)
            .orElse(null));
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

        String defaultLang = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG);
        label = getLabel(defaultLang);
        if (label != null) {
            return label;
        }

        return readData(e -> e.getChildText(LABEL_ELEMENT));
    }

    /**
     * Repairs additional metadata of this node
     */
    abstract void repairMetadata() throws IOException;

    protected <T> T readData(Function<Element, T> readOperation) {
        return getRoot().getDataGuard().read(() -> readOperation.apply(additionalData));
    }

    protected <T> void writeData(Consumer<Element> writeOperation) {
        getRoot().getDataGuard().write(() -> writeOperation.accept(additionalData));
    }

    public MCRFileAttributes<String> getBasicFileAttributes() throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return MCRDefaultFileAttributes.ofAttributes(attrs, null);
    }

}
