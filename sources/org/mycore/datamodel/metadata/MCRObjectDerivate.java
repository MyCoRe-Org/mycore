/*
 * $RCSfile$
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.mycore.common.MCRException;

/**
 * This class implements all methode for handling one derivate data.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRObjectDerivate {
    
    private static final Logger LOGGER=Logger.getLogger(MCRObjectDerivate.class);

    // derivate data
    private MCRMetaLinkID linkmeta = null;

    private ArrayList<MCRMetaLink> externals = null;

    private MCRMetaIFS internals = null;

    private ArrayList<MCRMetaLangText> titles = null;
    /**
     * This is the constructor of the MCRObjectDerivate class. All data are set
     * to null.
     */
    public MCRObjectDerivate() {
        linkmeta = null;
        externals = new ArrayList<MCRMetaLink>();
        internals = null;
        titles = new ArrayList<MCRMetaLangText>();
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
        org.jdom.Element linkmeta_element = derivate_element.getChild("linkmetas").getChild("linkmeta");
        MCRMetaLinkID link = new MCRMetaLinkID();
        link.setDataPart("linkmeta");
        link.setFromDOM(linkmeta_element);
        linkmeta=link;

        // External part
        org.jdom.Element externals_element = derivate_element.getChild("externals");
        externals.clear();
        if (externals_element != null) {
            List external_element_list = externals_element.getChildren();
            int external_len = external_element_list.size();
            for (int i = 0; i < external_len; i++) {
                org.jdom.Element external_element = (org.jdom.Element) external_element_list.get(i);
                MCRMetaLink eLink = new MCRMetaLink();
                eLink.setDataPart("external");
                eLink.setFromDOM(external_element);
                externals.add(eLink);
            }
        }

        // Internal part
        org.jdom.Element internals_element = derivate_element.getChild("internals");
        if (internals_element != null) {
            org.jdom.Element internal_element = internals_element.getChild("internal");
            if (internal_element != null) {
                internals = new MCRMetaIFS();
                internals.setDataPart("internal");
                internals.setFromDOM(internal_element);
            }
        }
        
        // Title part
        org.jdom.Element titles_element = derivate_element.getChild("titles");
        titles.clear();
        if (titles_element != null) {
            List title_element_list = titles_element.getChildren();
            int title_len = title_element_list.size();
            for (int i = 0; i < title_len; i++) {
                org.jdom.Element title_element = (org.jdom.Element) title_element_list.get(i);
                MCRMetaLangText text = new MCRMetaLangText();
                text.setDataPart("title");
                text.setFromDOM(title_element);
                if (text.isValid()) {
                    titles.add(text);
                }
            }            
        }
    }

    /**
     * This method return the size of the linkmeta array.
     * @deprecated
     * @see #getMetaLink()
     */
    /**
    public final int getLinkMetaSize() {
        return 0;
    }
*/

    /**
     * This method get a single link from the linkmeta list as a
     * MCRMetaLinkMCRObject.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @deprecated
     * @see #getMetaLink()
     * @return a metadata link as MCRMetaLinkID
     */
    /**
    public final MCRMetaLinkID getLinkMeta(int index) throws IndexOutOfBoundsException {
        if ((index != 0)) {
            throw new IndexOutOfBoundsException("Index error in getLinkMeta.");
        }
        return getMetaLink();
    }
    */

    /**
     * returns link to the MCRObject.
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
        linkmeta=in_link;
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
            throw new IndexOutOfBoundsException("Index error in getExternal("+Integer.toString(index)+").");
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
            throw new IndexOutOfBoundsException("Index error in getTitle("+Integer.toString(index)+").");
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
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("derivate");

        org.jdom.Element linkmetas = new org.jdom.Element("linkmetas");
        linkmetas.setAttribute("class", "MCRMetaLinkID");
        linkmetas.setAttribute("heritable", "false");
        linkmetas.addContent(linkmeta.createXML());
        elm.addContent(linkmetas);

        if (externals.size() != 0) {
            org.jdom.Element extEl = new org.jdom.Element("externals");
            extEl.setAttribute("class", "MCRMetaLink");
            extEl.setAttribute("heritable", "false");
            for (int i = 0; i < externals.size(); i++) {
                extEl.addContent(externals.get(i).createXML());
            }
            elm.addContent(extEl);
        }

        if (internals != null) {
            org.jdom.Element intEl = new org.jdom.Element("internals");
            intEl.setAttribute("class", "MCRMetaIFS");
            intEl.setAttribute("heritable", "false");
            intEl.addContent(internals.createXML());
            elm.addContent(intEl);
        }

        if (titles.size() != 0) {
            org.jdom.Element titEl = new org.jdom.Element("titles");
            titEl.setAttribute("class", "MCRMetaLangText");
            titEl.setAttribute("heritable", "false");
            for (int i = 0; i < titles.size(); i++) {
                titEl.addContent(titles.get(i).createXML());
            }
            elm.addContent(titEl);
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
        if (linkmeta == null){
            LOGGER.warn("linkmeta == null");
            return false;
        }
        if (!linkmeta.getXLinkType().equals("locator")){
            LOGGER.warn("linkmeta type != locator");
            return false;
        }

        if ((internals == null) && (externals.size() == 0)) {
            LOGGER.warn("(internals == null) && (externals.size() == 0)");
            return false;
        }

        return true;
    }
}
