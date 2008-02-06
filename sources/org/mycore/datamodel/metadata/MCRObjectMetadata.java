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

import java.util.ArrayList;
import java.util.List;

import org.jdom.Namespace;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This class implements all methode for handling one object metadata part. This
 * class uses only metadata type classes of the general datamodel code of
 * MyCoRe.
 * 
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 */
public class MCRObjectMetadata {
    // common data
    private String default_lang = null;

    private boolean herited_xml = false;

    // metadata list
    private ArrayList<MCRMetaElement> meta_list = null;

    private ArrayList<String> tag_names = null;

    /**
     * This is the constructor of the MCRObjectMetadata class. It set the
     * default language for all metadata to the value from the configuration
     * propertie <em>MCR.Metadata.DefaultLang</em>.
     * 
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRObjectMetadata() throws MCRConfigurationException {
        default_lang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang");
        herited_xml = MCRConfiguration.instance().getBoolean("MCR.Metadata.HeritedForXML", false);
        meta_list = new ArrayList<MCRMetaElement>();
        tag_names = new ArrayList<String>();
    }

    /**
     * <em>size</em> returns the number of tag names in the ArrayList.
     * 
     * @return int number of tags and meta elements
     */
    public int size() {
        return tag_names.size();
    }

    /**
     * The method returns the tag name at a given index.
     * 
     * @param i
     *            given index
     * @return String the associated tag name
     */
    public final String getMetadataTagName(int i) {
        return tag_names.get(i);
    }

    /**
     * <em>getHeritableMetadata</em> returns an instance of MCRObjectMetadata
     * containing all the heritable MetaElement's of this object.
     * 
     * @return MCRObjectMetadata the heritable part of this MCRObjectMetadata
     * @exception MCRConfigurationException
     */
    public final MCRObjectMetadata getHeritableMetadata() throws MCRConfigurationException {
        MCRObjectMetadata heritMeta = new MCRObjectMetadata();

        for (int i = 0; i < size(); ++i) {
            MCRMetaElement me = meta_list.get(i);

            if (me.getHeritable()) {
                MCRMetaElement nme = (MCRMetaElement) me.clone();

                for (int j = 0; j < nme.size(); j++) {
                    nme.getElement(j).incrementInherited();
                }

                heritMeta.setMetadataElement(nme, getMetadataTagName(i));
            }
        }

        return heritMeta;
    }

    /**
     * This method append MCRMetaElement's from a given MCRObjectMetadata to
     * this data set.
     * 
     * @param input
     *            the MCRObjectMetadata, that should merged into this data set
     */
    public final void appendMetadata(MCRObjectMetadata input) {
        MCRMetaElement newelm = null;
        String newtag = "";

        for (int i = 0; i < input.size(); i++) {
            newelm = input.getMetadataElement(i);
            newtag = newelm.getTag();

            int pos = -1;

            for (int j = 0; j < size(); j++) {
                if (tag_names.get(j).equals(newtag)) {
                    pos = j;
                }
            }

            if (pos != -1) {
                if (!meta_list.get(pos).getNotInherit()) {
                    meta_list.get(pos).setHeritable(true);

                    for (int j = 0; j < newelm.size(); j++) {
                        MCRMetaInterface obj = newelm.getElement(j);
                        meta_list.get(pos).addMetaObject(obj);
                    }
                }
            } else {
                tag_names.add(newtag);
                newelm.setHeritable(true);
                meta_list.add(newelm);
            }
        }
    }

    /**
     * This methode return the MCRMetaElement selected by tag. If this was not
     * found, null was returned.
     * 
     * @param tag
     *            the element tag
     * @return the MCRMetaElement for the tag
     */
    public final MCRMetaElement getMetadataElement(String tag) {
        if ((tag == null) || ((tag = tag.trim()).length() == 0)) {
            return null;
        }

        int len = tag_names.size();

        for (int i = 0; i < len; i++) {
            if (tag_names.get(i).equals(tag)) {
                return meta_list.get(i);
            }
        }

        return null;
    }

    /**
     * This methode return the MCRMetaElement selected by an index. If this was
     * not found, null was returned.
     * 
     * @param index
     *            the element index
     * @return the MCRMetaElement for the index
     */
    public final MCRMetaElement getMetadataElement(int index) {
        if ((index < 0) || (index > meta_list.size())) {
            return null;
        }

        return meta_list.get(index);
    }

    /**
     * This methode set the given MCRMetaElement to the list. If the tag exists
     * the MCRMetaElement was replaced.
     * 
     * @param obj
     *            the MCRMetaElement object
     * @param tag
     *            the MCRMetaElement tag
     * @return true if set was succesful, otherwise false
     */
    public final boolean setMetadataElement(MCRMetaElement obj, String tag) {
        if (obj == null) {
            return false;
        }

        if ((tag == null) || ((tag = tag.trim()).length() == 0)) {
            return false;
        }

        int len = tag_names.size();
        int fl = -1;

        for (int i = 0; i < len; i++) {
            if (tag_names.get(i).equals(tag)) {
                fl = i;
            }
        }

        if (fl == -1) {
            meta_list.add(obj);
            tag_names.add(tag);

            return true;
        }

        meta_list.remove(fl);
        meta_list.add(obj);

        return false;
    }

    /**
     * This methode remove the MCRMetaElement selected by tag from the list.
     * 
     * @return true if set was succesful, otherwise false
     */
    public final boolean removeMetadataElement(String tag) {
        if ((tag == null) || ((tag = tag.trim()).length() == 0)) {
            return false;
        }

        int len = tag_names.size();

        for (int i = 0; i < len; i++) {
            if (tag_names.get(i).equals(tag)) {
                meta_list.remove(i);
                tag_names.remove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * This methode remove the MCRMetaElement selected a index from the list.
     * 
     * @return true if set was succesful, otherwise false
     */
    public final boolean removeMetadataElement(int index) {
        if ((index < 0) || (index > size())) {
            return false;
        }

        meta_list.remove(index);
        tag_names.remove(index);

        return true;
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a list of relevant DOM elements for the metadata
     * @exception MCRException
     *                if a problem is occured
     */
    public final void setFromDOM(org.jdom.Element element) throws MCRException {
        String temp_lang = element.getAttributeValue("lang");

        if ((temp_lang != null) && ((temp_lang = temp_lang.trim()).length() != 0)) {
            default_lang = temp_lang;
        }

        List elements_list = element.getChildren();
        int len = elements_list.size();
        String temp_tag = "";

        for (int i = 0; i < len; i++) {
            org.jdom.Element subtag = (org.jdom.Element) elements_list.get(i);
            temp_tag = subtag.getName();

            if ((temp_tag == null) || ((temp_tag = temp_tag.trim()).length() == 0)) {
                throw new MCRException("MCRObjectMetadata : The tag is null or empty.");
            }

            tag_names.add(temp_tag);

            MCRMetaElement obj = new MCRMetaElement(default_lang);
            obj.setFromDOM(subtag);
            meta_list.add(obj);
        }
    }

    /**
     * This methode create a XML stream for all metadata.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the metadata part
     */
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("MCRObjectMetadata : The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("metadata");
        elm.setAttribute("lang", default_lang, Namespace.XML_NAMESPACE);

        int len = meta_list.size();

        for (int i = 0; i < len; i++) {
            elm.addContent(meta_list.get(i).createXML(herited_xml));
        }

        return elm;
    }

    /**
     * This methode check the validation of the content of this class. The
     * methode returns <em>true</em> if
     * <ul>
     * <li>the array is empty
     * <li>the default lang value was supported
     * </ul>
     * otherwise the methode return <em>false</em>
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        if (meta_list.size() == 0) {
            return false;
        }

        if (!MCRUtils.isSupportedLang(default_lang)) {
            return false;
        }

        return true;
    }
}
