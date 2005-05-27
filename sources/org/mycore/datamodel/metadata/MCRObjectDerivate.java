/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.metadata;

import java.text.*;
import java.util.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class implements all methode for handling one derivate data.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRObjectDerivate {
    // common data
    private String NL;

    // derivate data
    private ArrayList linkmetas = null;

    private ArrayList externals = null;

    private MCRMetaIFS internals = null;

    /**
     * This is the constructor of the MCRObjectDerivate class. All data are set
     * to null.
     */
    public MCRObjectDerivate() {
        NL = new String((System.getProperties()).getProperty("line.separator"));
        linkmetas = new ArrayList();
        externals = new ArrayList();
        internals = null;
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * structure data of the document.
     * 
     * @param derivate_element
     *            a list of relevant DOM elements for the derivate
     */
    public final void setFromDOM(org.jdom.Element derivate_element) {
        // Link to Metadata part
        org.jdom.Element linkmetas_element = derivate_element
                .getChild("linkmetas");
        if (linkmetas_element != null) {
            List linkmeta_element_list = linkmetas_element.getChildren();
            int linkmeta_len = linkmeta_element_list.size();
            for (int i = 0; i < linkmeta_len; i++) {
                org.jdom.Element linkmeta_element = (org.jdom.Element) linkmeta_element_list
                        .get(i);
                MCRMetaLinkID link = new MCRMetaLinkID();
                link.setDataPart("linkmeta");
                link.setFromDOM(linkmeta_element);
                linkmetas.add(link);
            }
        }
        // External part
        org.jdom.Element externals_element = derivate_element
                .getChild("externals");
        if (externals_element != null) {
            List external_element_list = externals_element.getChildren();
            int external_len = external_element_list.size();
            for (int i = 0; i < external_len; i++) {
                org.jdom.Element external_element = (org.jdom.Element) external_element_list
                        .get(i);
                MCRMetaLink link = new MCRMetaLink();
                link.setDataPart("external");
                link.setFromDOM(external_element);
                externals.add(link);
            }
        }
        // Internal part
        org.jdom.Element internals_element = derivate_element
                .getChild("internals");
        if (internals_element != null) {
            org.jdom.Element internal_element = internals_element
                    .getChild("internal");
            if (internal_element != null) {
                internals = new MCRMetaIFS();
                internals.setDataPart("internal");
                internals.setFromDOM(internal_element);
            }
        }
    }

    /**
     * This method return the size of the linkmeta array.
     */
    public final int getLinkMetaSize() {
        return linkmetas.size();
    }

    /**
     * This method get a single link from the linkmeta list as a
     * MCRMetaLinkMCRObject.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a metadata link as MCRMetaLinkID
     */
    public final MCRMetaLinkID getLinkMeta(int index)
            throws IndexOutOfBoundsException {
        if ((index < 0) || (index > linkmetas.size())) {
            throw new IndexOutOfBoundsException("Index error in getLinkMeta.");
        }
        return (MCRMetaLinkID) linkmetas.get(index);
    }

    /**
     * This method set the metadata link
     * 
     * @param in_link
     *            the MCRMetaLinkID object
     */
    public final void setLinkMeta(MCRMetaLinkID in_link) {
        if (in_link == null)
            return;
        linkmetas.add(in_link);
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
    public final MCRMetaLink getExternal(int index)
            throws IndexOutOfBoundsException {
        if ((index < 0) || (index > externals.size())) {
            throw new IndexOutOfBoundsException("Index error in getExternal.");
        }
        return (MCRMetaLink) externals.get(index);
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
     * This method set the metadata internals (the IFS data)
     * 
     * @param in_ifs
     *            the MCRMetaIFS object
     */
    public final void setInternals(MCRMetaIFS in_ifs) {
        if (in_ifs == null)
            return;
        internals = in_ifs;
    }

    /**
     * This methode create a XML stream for all derivate data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }
        org.jdom.Element elm = new org.jdom.Element("derivate");
        if (linkmetas.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("linkmetas");
            elmm.setAttribute("class", "MCRMetaLinkID");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");
            for (int i = 0; i < linkmetas.size(); i++) {
                elmm.addContent(((MCRMetaLinkID) linkmetas.get(i)).createXML());
            }
            elm.addContent(elmm);
        }
        if (externals.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("externals");
            elmm.setAttribute("class", "MCRMetaLink");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");
            for (int i = 0; i < externals.size(); i++) {
                elmm.addContent(((MCRMetaLink) externals.get(i)).createXML());
            }
            elm.addContent(elmm);
        }
        if (internals != null) {
            org.jdom.Element elmm = new org.jdom.Element("internals");
            elmm.setAttribute("class", "MCRMetaIFS");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("parasearch", "false");
            elmm.setAttribute("textsearch", "false");
            elmm.addContent(internals.createXML());
            elm.addContent(elmm);
        }
        return elm;
    }

    /**
     * This methode create a typed content list for all derivate data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a MCRTypedContent with the data of the metadata part
     */
    public final MCRTypedContent createTypedContent() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }
        MCRTypedContent tc = new MCRTypedContent();
        tc.addTagElement(MCRTypedContent.TYPE_MASTERTAG, "derivate");
        tc.addTagElement(MCRTypedContent.TYPE_TAG, "linkmetas");
        for (int i = 0; i < linkmetas.size(); i++) {
            tc.addMCRTypedContent(((MCRMetaLinkID) linkmetas.get(i))
                    .createTypedContent(true));
        }
        return tc;
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
        if (linkmetas.size() != 0) {
            for (int i = 0; i < linkmetas.size(); i++) {
                if (!((MCRMetaLinkID) linkmetas.get(i)).getXLinkType().equals(
                        "locator")) {
                    return false;
                }
            }
        }
        if ((internals == null) && (externals.size() == 0)) {
            return false;
        }
        return true;
    }

}

