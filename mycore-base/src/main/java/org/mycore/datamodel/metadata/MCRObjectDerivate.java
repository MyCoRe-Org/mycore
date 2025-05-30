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

package org.mycore.datamodel.metadata;

import static org.mycore.datamodel.metadata.MCRXMLConstants.CLASS;
import static org.mycore.datamodel.metadata.MCRXMLConstants.HERITABLE;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRXlink;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * This class implements all methode for handling one derivate data.
 *
 * @author Jens Kupferschmidt
 */
public class MCRObjectDerivate {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String XML_NAME = "derivate";

    public static final String ELEMENT_EXTERNALS = "externals";

    public static final String ELEMENT_INTERNALS = "internals";

    public static final String ELEMENT_INTERNAL = "internal";

    public static final String ELEMENT_TITLES = "titles";

    public static final String ELEMENT_CLASSIFICATIONS = "classifications";

    public static final String ELEMENT_FILESET = "fileset";

    public static final String ELEMENT_LINKMETAS = "linkmetas";

    public static final String ELEMENT_LINKMETA = "linkmeta";

    public static final String ATTRIBUTE_FILESET_URN = "urn";

    // derivate data
    private MCRMetaLinkID linkmeta;

    private final List<MCRMetaLink> externals;

    private MCRMetaIFS internals;

    private final List<MCRMetaLangText> titles;

    private final List<MCRMetaClassification> classifications;

    private String derivateURN;

    private List<MCRFileMetadata> files;

    private MCRObjectID derivateID;

    /**
     * This is the constructor of the MCRObjectDerivate class. All data are set
     * to null.
     */
    public MCRObjectDerivate(MCRObjectID derivateID) {
        linkmeta = null;
        externals = new ArrayList<>();
        internals = null;
        titles = new ArrayList<>();
        classifications = new ArrayList<>();
        files = Collections.emptyList();
        this.derivateID = derivateID;
    }

    public MCRObjectDerivate(MCRObjectID derivateID, Element derivate) {
        this(derivateID);
        setFromDOM(derivate);
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * structure data of the document.
     *
     * @param derivate
     *            a list of relevant DOM elements for the derivate
     */
    private void setFromDOM(Element derivate) {
        // Link to Metadata part
        setMetadataFromDOM(derivate);

        // External part
        setExternalFromDOM(derivate);

        // Internal part
        setInternalFromDOM(derivate);

        // Title part
        setTitleFromDOM(derivate);

        // Classification part
        setClassificationsFromDOM(derivate);

        // fileset part
        setFilesetFromDOM(derivate);
    }

    private void setFilesetFromDOM(Element derivate) {
        Element filesetElements = derivate.getChild(ELEMENT_FILESET);
        if (filesetElements != null) {
            String mainURN = filesetElements.getAttributeValue(ATTRIBUTE_FILESET_URN);
            if (mainURN != null) {
                this.derivateURN = mainURN;
            }
            List<Element> filesInList = filesetElements.getChildren();
            if (!filesInList.isEmpty()) {
                files = new ArrayList<>(filesInList.size());
                for (Element file : filesInList) {
                    files.add(new MCRFileMetadata(file));
                }
            }
        }
    }

    private void setClassificationsFromDOM(Element derivate) {
        Element classificationElement = derivate.getChild(ELEMENT_CLASSIFICATIONS);
        classifications.clear();
        if (classificationElement != null) {
            final List<Element> classificationList = classificationElement.getChildren();
            classificationList.stream().map((classElement) -> {
                MCRMetaClassification clazzObject = new MCRMetaClassification();
                clazzObject.setFromDOM(classElement);
                return clazzObject;
            }).forEach(classifications::add);
        }
    }

    private void setTitleFromDOM(Element derivate) {
        Element titlesElement = derivate.getChild(ELEMENT_TITLES);
        titles.clear();
        if (titlesElement != null) {
            List<Element> titleList = titlesElement.getChildren();
            for (Element titleElement : titleList) {
                MCRMetaLangText text = new MCRMetaLangText();
                text.setFromDOM(titleElement);
                if (text.isValid()) {
                    titles.add(text);
                }
            }
        }
    }

    private void setInternalFromDOM(Element derivate) {
        Element internalsElement = derivate.getChild(ELEMENT_INTERNALS);
        if (internalsElement != null) {
            Element internalElement = internalsElement.getChild(ELEMENT_INTERNAL);
            if (internalElement != null) {
                internals = new MCRMetaIFS();
                internals.setFromDOM(internalElement);
            }
        }
    }

    private void setExternalFromDOM(Element derivate) {
        Element externalsElement = derivate.getChild(ELEMENT_EXTERNALS);
        externals.clear();
        if (externalsElement != null) {
            List<Element> externalList = externalsElement.getChildren();
            for (Element externalElement : externalList) {
                MCRMetaLink eLink = new MCRMetaLink();
                eLink.setFromDOM(externalElement);
                externals.add(eLink);
            }
        }
    }

    private void setMetadataFromDOM(Element derivate) {
        Element linkmetaElement = derivate.getChild(ELEMENT_LINKMETAS).getChild(ELEMENT_LINKMETA);
        MCRMetaLinkID link = new MCRMetaLinkID();
        link.setFromDOM(linkmetaElement);
        linkmeta = link;
    }

    /**
     * returns link to the MCRObject.
     *
     * @return a metadata link as MCRMetaLinkID
     */
    public MCRMetaLinkID getMetaLink() {
        return linkmeta;
    }

    /**
     * This method set the metadata link
     *
     * @param link
     *            the MCRMetaLinkID object
     */
    public final void setLinkMeta(MCRMetaLinkID link) {
        linkmeta = link;
    }

    /**
     * This method return the size of the external array.
     */
    public final int getExternalSize() {
        return externals.size();
    }

    /**
     * This method get a single link from the external list as a MCRMetaLink.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a external link as MCRMetaLink
     */
    public final MCRMetaLink getExternal(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > externals.size())) {
            throw new IndexOutOfBoundsException("Index error in getExternal(" + index + ").");
        }

        return externals.get(index);
    }

    /**
     * This method return the size of the title array.
     */
    public final int getTitleSize() {
        return titles.size();
    }

    /**
     * This method get a single text from the titles list as a MCRMetaLangText.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a title text as MCRMetaLangText
     */
    public final MCRMetaLangText getTitle(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > titles.size())) {
            throw new IndexOutOfBoundsException("Index error in getTitle(" + index + ").");
        }

        return titles.get(index);
    }

    /**
     * This method get a single data from the internal list as a MCRMetaIFS.
     *
     * @return a internal data as MCRMetaIFS
     */
    public final MCRMetaIFS getInternals() {
        return internals;
    }

    /**
     * @param file the file to add
     * @param urn the urn of the file, if already known, if not provide null
     *
     * @throws NullPointerException if first argument is null
     */
    public MCRFileMetadata getOrCreateFileMetadata(MCRPath file, String urn) {
        return getOrCreateFileMetadata(file, urn, null);
    }

    public MCRFileMetadata getOrCreateFileMetadata(MCRPath file, String urn, String handle) {
        Objects.requireNonNull(file, "File may not be null");
        String path = "/" + file.subpathComplete();
        return getOrCreateFileMetadata(path, urn, handle);
    }

    private MCRFileMetadata getOrCreateFileMetadata(String path, String urn, String handle) {
        Objects.requireNonNull(path, "path may not be null");

        int fileCount = files.size();
        for (int i = 0; i < fileCount; i++) {
            MCRFileMetadata fileMetadata = files.get(i);
            int compare = fileMetadata.getName().compareTo(path);
            if (compare == 0) {
                return fileMetadata;
            } else if (compare > 0) {
                //we need to create entry here
                MCRFileMetadata newFileMetadata = createFileMetadata(path, urn, handle);
                files.add(i, newFileMetadata);
                return newFileMetadata;
            }
        }
        //add path to end of list;
        if (files.isEmpty()) {
            files = new ArrayList<>();
        }
        MCRFileMetadata newFileMetadata = createFileMetadata(path, urn, handle);
        files.add(newFileMetadata);
        return newFileMetadata;
    }

    public final MCRFileMetadata getOrCreateFileMetadata(String path) {
        return getOrCreateFileMetadata(MCRPath.getPath(derivateID.toString(), path), null, null);
    }

    public MCRFileMetadata getOrCreateFileMetadata(MCRPath file) {
        return getOrCreateFileMetadata(file, null, null);
    }

    private MCRFileMetadata createFileMetadata(String path, String urn, String handle) {
        MCRPath mcrFile = MCRPath.getPath(derivateID.toString(), path);
        if (!Files.exists(mcrFile)) {
            throw new MCRPersistenceException("File does not exist: " + mcrFile);
        }
        return new MCRFileMetadata(path, urn, handle, null);
    }

    public List<MCRFileMetadata> getFileMetadata() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Removes file metadata (urn information) from the {@link MCRObjectDerivate}
     */
    public void removeFileMetadata() {
        this.files = Collections.emptyList();
        this.derivateURN = null;
    }

    /**
     * Deletes file metadata of file idendified by absolute path.
     * @param path absolute path of this node starting with a '/'
     * @return true if metadata was deleted and false if file has no metadata.
     */
    public boolean deleteFileMetaData(String path) {
        Iterator<MCRFileMetadata> it = files.iterator();
        while (it.hasNext()) {
            MCRFileMetadata metadata = it.next();
            if (metadata.getName().equals(path)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * This method set the metadata internals (the IFS data)
     *
     * @param ifs
     *            the MCRMetaIFS object
     */
    public final void setInternals(MCRMetaIFS ifs) {
        if (ifs == null) {
            return;
        }

        internals = ifs;
    }

    /**
     * This methode create a XML stream for all derivate data.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }
        Element elm = new Element(XML_NAME);
        Element linkmetas = new Element(ELEMENT_LINKMETAS);
        linkmetas.setAttribute(CLASS, MCRMetaLinkID.class.getSimpleName());
        linkmetas.setAttribute(HERITABLE, MCRXMLConstants.FALSE);
        linkmetas.addContent(linkmeta.createXML());
        elm.addContent(linkmetas);
        if (!externals.isEmpty()) {
            Element extEl = createExternalsElement();
            elm.addContent(extEl);
        }
        if (internals != null) {
            Element intEl = createInternalsElement();
            elm.addContent(intEl);
        }
        if (!titles.isEmpty()) {
            Element titEl = createTitleElement();
            elm.addContent(titEl);
        }
        if (!classifications.isEmpty()) {
            Element clazzElement = createClassificationElement();
            elm.addContent(clazzElement);
        }
        if (this.derivateURN != null || !files.isEmpty()) {
            Element fileset = createFilesetElement();
            elm.addContent(fileset);
        }
        return elm;
    }

    private Element createExternalsElement() {
        Element extEl = new Element(ELEMENT_EXTERNALS);
        extEl.setAttribute(CLASS, MCRMetaLink.class.getSimpleName());
        extEl.setAttribute(HERITABLE, MCRXMLConstants.FALSE);
        for (MCRMetaLink external : externals) {
            extEl.addContent(external.createXML());
        }
        return extEl;
    }

    private Element createInternalsElement() {
        Element intEl = new Element(ELEMENT_INTERNALS);
        intEl.setAttribute(CLASS, MCRMetaIFS.class.getSimpleName());
        intEl.setAttribute(HERITABLE, MCRXMLConstants.FALSE);
        intEl.addContent(internals.createXML());
        return intEl;
    }

    private Element createTitleElement() {
        Element titEl = new Element(ELEMENT_TITLES);
        titEl.setAttribute(CLASS, MCRMetaLangText.class.getSimpleName());
        titEl.setAttribute(HERITABLE, MCRXMLConstants.FALSE);
        titles.stream()
            .map(MCRMetaLangText::createXML)
            .forEach(titEl::addContent);
        return titEl;
    }

    private Element createClassificationElement() {
        Element clazzElement = new Element(ELEMENT_CLASSIFICATIONS);
        clazzElement.setAttribute(CLASS, MCRMetaClassification.class.getSimpleName());
        clazzElement.setAttribute(HERITABLE, MCRXMLConstants.FALSE);

        classifications.stream()
            .map(MCRMetaClassification::createXML)
            .forEach(clazzElement::addContent);
        return clazzElement;
    }

    private Element createFilesetElement() {
        Element fileset = new Element(ELEMENT_FILESET);
        if (this.derivateURN != null) {
            fileset.setAttribute(ATTRIBUTE_FILESET_URN, this.derivateURN);
        }
        Collections.sort(files);
        for (MCRFileMetadata file : files) {
            fileset.addContent(file.createXML());
        }
        return fileset;
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if <br>
     * <ul>
     * <li>the linkmeta exist and the XLink type of linkmeta is not "arc"</li>
     * <li>no information in the external AND internal tags</li>
     * </ul>
     *
     * @return a boolean value
     */
    public final boolean isValid() {
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The <derivate> part of the mycorederivate '{}' is invalid.", derivateID, exc);
        }
        return false;
    }

    /**
     * Validates this MCRObjectDerivate. This method throws an exception if:
     *  <ul>
     *  <li>the linkmeta is null</li>
     *  <li>the linkmeta xlink:type is not 'locator'</li>
     *  <li>the internals and the externals are empty</li>
     *  </ul>
     *
     * @throws MCRException the MCRObjectDerivate is invalid
     */
    public void validate() throws MCRException {
        if (linkmeta == null) {
            throw new MCRException("linkmeta == null");
        }
        if (!linkmeta.getXLinkType().equals(MCRXlink.TYPE_LOCATOR)) {
            throw new MCRException("linkmeta type != locator");
        }
        if ((internals == null) && (externals.isEmpty())) {
            throw new MCRException("(internals == null) && (externals.size() == 0)");
        }
    }

    public void setURN(String urn) {
        derivateURN = urn;
    }

    public String getURN() {
        return derivateURN;
    }

    void setDerivateID(MCRObjectID id) {
        this.derivateID = id;
    }

    public List<MCRMetaClassification> getClassifications() {
        return classifications;
    }

    public List<MCRMetaLangText> getTitles() {
        return titles;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classifications, derivateID, derivateURN, externals, files, internals, linkmeta, titles);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRObjectDerivate other = (MCRObjectDerivate) obj;
        return Objects.equals(classifications, other.classifications)
            && Objects.equals(derivateID, other.derivateID)
            && Objects.equals(derivateURN, other.derivateURN)
            && Objects.equals(externals, other.externals)
            && Objects.equals(files, other.files)
            && Objects.equals(internals, other.internals)
            && Objects.equals(linkmeta, other.linkmeta)
            && Objects.equals(titles, other.titles);
    }
}
