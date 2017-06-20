/*
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.metadata;

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
import org.mycore.datamodel.niofs.MCRPath;

/**
 * This class implements all methode for handling one derivate data.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06. Feb
 *          2008) $
 */
public class MCRObjectDerivate {

    private static final Logger LOGGER = LogManager.getLogger();

    // derivate data
    private MCRMetaLinkID linkmeta;

    private final ArrayList<MCRMetaLink> externals;

    private MCRMetaIFS internals;

    private final ArrayList<MCRMetaLangText> titles;

    private String derivateURN;

    private boolean display;

    private List<MCRFileMetadata> files;

    private MCRObjectID derivateID;

    /**
     * This is the constructor of the MCRObjectDerivate class. All data are set
     * to null.
     */
    public MCRObjectDerivate(MCRObjectID derivateID) {
        linkmeta = null;
        externals = new ArrayList<MCRMetaLink>();
        internals = null;
        titles = new ArrayList<MCRMetaLangText>();
        files = Collections.emptyList();
        display = true;
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
    private void setFromDOM(org.jdom2.Element derivate) {
        // Link to Metadata part
        org.jdom2.Element linkmeta_element = derivate.getChild("linkmetas").getChild("linkmeta");
        MCRMetaLinkID link = new MCRMetaLinkID();
        link.setFromDOM(linkmeta_element);
        linkmeta = link;

        // External part
        org.jdom2.Element externalsElement = derivate.getChild("externals");
        externals.clear();
        if (externalsElement != null) {
            List<Element> externalList = externalsElement.getChildren();
            for (Element externalElement : externalList) {
                MCRMetaLink eLink = new MCRMetaLink();
                eLink.setFromDOM(externalElement);
                externals.add(eLink);
            }
        }

        // Internal part
        org.jdom2.Element internals_element = derivate.getChild("internals");
        if (internals_element != null) {
            org.jdom2.Element internal_element = internals_element.getChild("internal");
            if (internal_element != null) {
                internals = new MCRMetaIFS();
                internals.setFromDOM(internal_element);
            }
        }

        // Title part
        org.jdom2.Element titlesElement = derivate.getChild("titles");
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

        // fileset part
        Element filesetElements = derivate.getChild("fileset");
        if (filesetElements != null) {
            String mainURN = filesetElements.getAttributeValue("urn");
            if (mainURN != null) {
                this.derivateURN = mainURN;
            }
            List<Element> filesInList = filesetElements.getChildren();
            if (!filesInList.isEmpty()) {
                files = new ArrayList<MCRFileMetadata>(filesInList.size());
                for (Element file : filesInList) {
                    files.add(new MCRFileMetadata(file));
                }
            }
        }

        String displayVal = derivate.getAttributeValue("display");
        if (displayVal != null && displayVal.length() > 3) {
            this.display = Boolean.valueOf(displayVal);
        }
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
     * @param in_link
     *            the MCRMetaLinkID object
     */
    public final void setLinkMeta(MCRMetaLinkID in_link) {
        linkmeta = in_link;
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
            throw new IndexOutOfBoundsException("Index error in getExternal(" + Integer.toString(index) + ").");
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
            throw new IndexOutOfBoundsException("Index error in getTitle(" + Integer.toString(index) + ").");
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
        String path = "/" + file.subpathComplete().toString();
        return getOrCreateFileMetadata(path, urn, handle);
    }

    /**
     * @param path
     * @param urn
     * @param handle
     * @return
     */
    private MCRFileMetadata getOrCreateFileMetadata(String path, String urn, String handle) {
        if (path == null) {
            throw new NullPointerException("path may not be null");
        }
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
            files = new ArrayList<MCRFileMetadata>();
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

    /**
     * @param path
     * @param urn
     * @param handle
     * @return
     */
    private MCRFileMetadata createFileMetadata(String path, String urn, String handle) {
        MCRPath mcrFile = MCRPath.getPath(derivateID.toString(), path);
        if (!Files.exists(mcrFile)) {
            throw new MCRPersistenceException("File does not exist: " + mcrFile);
        }
        MCRFileMetadata newFileMetadata = new MCRFileMetadata(path, urn, handle, null);
        return newFileMetadata;
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
     * @param in_ifs
     *            the MCRMetaIFS object
     */
    public final void setInternals(MCRMetaIFS in_ifs) {
        if (in_ifs == null) {
            return;
        }

        internals = in_ifs;
    }

    /**
     * This methode create a XML stream for all derivate data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final org.jdom2.Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }
        org.jdom2.Element elm = new org.jdom2.Element("derivate");
        elm.setAttribute("display", String.valueOf(display));

        org.jdom2.Element linkmetas = new org.jdom2.Element("linkmetas");
        linkmetas.setAttribute("class", "MCRMetaLinkID");
        linkmetas.setAttribute("heritable", "false");
        linkmetas.addContent(linkmeta.createXML());
        elm.addContent(linkmetas);

        if (externals.size() != 0) {
            org.jdom2.Element extEl = new org.jdom2.Element("externals");
            extEl.setAttribute("class", "MCRMetaLink");
            extEl.setAttribute("heritable", "false");
            for (MCRMetaLink external : externals) {
                extEl.addContent(external.createXML());
            }
            elm.addContent(extEl);
        }

        if (internals != null) {
            org.jdom2.Element intEl = new org.jdom2.Element("internals");
            intEl.setAttribute("class", "MCRMetaIFS");
            intEl.setAttribute("heritable", "false");
            intEl.addContent(internals.createXML());
            elm.addContent(intEl);
        }

        if (titles.size() != 0) {
            org.jdom2.Element titEl = new org.jdom2.Element("titles");
            titEl.setAttribute("class", "MCRMetaLangText");
            titEl.setAttribute("heritable", "false");
            for (MCRMetaLangText title : titles) {
                titEl.addContent(title.createXML());
            }
            elm.addContent(titEl);
        }

        if (this.derivateURN != null || !files.isEmpty()) {
            Element fileset = new Element("fileset");

            if (this.derivateURN != null) {
                fileset.setAttribute("urn", this.derivateURN);
            }
            Collections.sort(files);
            for (MCRFileMetadata file : files) {
                fileset.addContent(file.createXML());
            }
            elm.addContent(fileset);
        }

        return elm;
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
            LOGGER.warn("The <derivate> part of the mycorederivate '" + derivateID.toString() + "' is invalid.", exc);
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
        if (!linkmeta.getXLinkType().equals("locator")) {
            throw new MCRException("linkmeta type != locator");
        }
        if ((internals == null) && (externals.size() == 0)) {
            throw new MCRException("(internals == null) && (externals.size() == 0)");
        }
    }

    /**
     * @return <code>true</code> if the display attribute is set to true, <code>false</code> otherwise 
     */
    public boolean isDisplayEnabled() {
        return display;
    }

    /**
     * Sets the display attribute of the derivate object
     * @param display pass <code>true</code> if you want to have the derivate displayed or <code>false</code> if not  
     */
    public void setDisplayEnabled(boolean display) {
        this.display = display;
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

}
