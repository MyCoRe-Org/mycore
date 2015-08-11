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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

/**
 * This class implements all methode for handling one object metadata part. This
 * class uses only metadata type classes of the general datamodel code of
 * MyCoRe.
 * 
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 */
public class MCRObjectMetadata implements Iterable<MCRMetaElement> {
    private static final Logger LOGGER = Logger.getLogger(MCRObjectMetadata.class);

    // common data
    private boolean herited_xml = true;

    // metadata list
    private final ArrayList<MCRMetaElement> meta_list;

    /**
     * This is the constructor of the MCRObjectMetadata class. It set the
     * default language for all metadata to the value from the configuration
     * propertie <em>MCR.Metadata.DefaultLang</em>.
     * 
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRObjectMetadata() throws MCRConfigurationException {
        herited_xml = MCRConfiguration.instance().getBoolean("MCR.Metadata.HeritedForXML", true);
        meta_list = new ArrayList<MCRMetaElement>();
    }

    /**
     * <em>size</em> returns the number of tag names in the ArrayList.
     * 
     * @return int number of tags and meta elements
     */
    public int size() {
        return meta_list.size();
    }

    /**
     * <em>getHeritableMetadata</em> returns an instance of MCRObjectMetadata
     * containing all the heritable MetaElement's of this object.
     * 
     * @return MCRObjectMetadata the heritable part of this MCRObjectMetadata
     */
    public final MCRObjectMetadata getHeritableMetadata() {
        MCRObjectMetadata heritMeta = new MCRObjectMetadata();

        for (int i = 0; i < size(); ++i) {
            MCRMetaElement me = meta_list.get(i);

            if (me.isHeritable()) {
                MCRMetaElement nme = (MCRMetaElement) me.clone();

                for (int j = 0; j < nme.size(); j++) {
                    nme.getElement(j).incrementInherited();
                }

                heritMeta.setMetadataElement(nme);
            }
        }

        return heritMeta;
    }

    /**
     * <em>removeInheritedMetadata</em> removes all inherited metadata elements  
     * TODO check necessary of <code>counter</code>
     */
    public final void removeInheritedMetadata() {
        Iterator<MCRMetaElement> elements = meta_list.iterator();
        while (elements.hasNext()) {
            MCRMetaElement me = elements.next();
            me.removeInheritedMetadata();
            //remove meta element if empty (else isValid() will fail)
            if (me.size() == 0) {
                elements.remove();
            }
        }
    }

    /**
     * This method append MCRMetaElement's from a given MCRObjectMetadata to
     * this data set.
     * 
     * @param input
     *            the MCRObjectMetadata, that should merged into this data set
     */
    public final void appendMetadata(MCRObjectMetadata input) {

        for (MCRMetaElement newelm : input) {
            int pos = -1;

            for (int j = 0; j < size(); j++) {
                if (meta_list.get(j).getTag().equals(newelm.getTag())) {
                    pos = j;
                }
            }

            if (pos != -1) {
                if (!meta_list.get(pos).inheritsNot()) {
                    meta_list.get(pos).setHeritable(true);

                    for (int j = 0; j < newelm.size(); j++) {
                        MCRMetaInterface obj = newelm.getElement(j);
                        meta_list.get(pos).addMetaObject(obj);
                    }
                }
            } else {
                meta_list.add(newelm);
            }
        }
    }

    /**
     * This method adds MCRMetaElement's from a given MCRObjectMetadata to
     * this data set if there are any differences between the data sets.
     * 
     * @param input
     *            the MCRObjectMetadata, that should merged into this data set
     */
    public final void mergeMetadata(MCRObjectMetadata input) {

        for (MCRMetaElement metaElement : input) {
            int pos = -1;
            for (int j = 0; j < size(); j++) {
                if (meta_list.get(j).getTag().equals(metaElement.getTag())) {
                    pos = j;
                }
            }
            if (pos != -1) {
                for (int j = 0; j < metaElement.size(); j++) {
                    boolean found = false;
                    for (MCRMetaInterface mcrMetaInterface : meta_list.get(pos)) {
                        Element xml = mcrMetaInterface.createXML();
                        Element xmlNEW = metaElement.getElement(j).createXML();
                        List<Element> childrenXML = xml.getChildren();
                        if (childrenXML.size() > 0 && xmlNEW.getChildren().size() > 0) {
                            int i = 0;
                            for (Element element : childrenXML) {
                                Element elementNew = xmlNEW.getChild(element.getName());

                                if (elementNew != null && element != null) {
                                    if (element.getText().equals(elementNew.getText())) {
                                        i++;
                                    }
                                }
                            }
                            if (i == childrenXML.size()) {
                                found = true;
                            }
                        } else {
                            if (xml.getText().equals(xmlNEW.getText())) {
                                found = true;
                            } else if (!found) {
                                int i = 0;
                                List<Attribute> attributes = xml.getAttributes();
                                for (Attribute attribute : attributes) {
                                    Attribute attr = xmlNEW.getAttribute(attribute.getName());
                                    if ((attr != null) && attr.equals(attribute)) {
                                        i++;
                                    }
                                }
                                if (i == attributes.size()) {
                                    found = true;
                                }

                            }
                        }
                    }
                    MCRMetaInterface obj = metaElement.getElement(j);
                    if (!found) {
                        meta_list.get(pos).addMetaObject(obj);
                    } else if (LOGGER.isDebugEnabled()) {
                        LOGGER.info("Found equal tags: \n\r" + new XMLOutputter(Format.getPrettyFormat()).outputString(obj.createXML()));
                    }
                }
            } else {
                meta_list.add(metaElement);
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
        for (MCRMetaElement sub : this) {
            if (sub.getTag().equals(tag)) {
                return sub;
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
        return meta_list.get(index);
    }

    /**
     * sets the given MCRMetaElement to the list. If the tag exists
     * the MCRMetaElement was replaced.
     * 
     * @param obj
     *            the MCRMetaElement object
     */
    public final void setMetadataElement(MCRMetaElement obj) {
        MCRMetaElement old = getMetadataElement(obj.getTag());

        if (old == null) {
            meta_list.add(obj);
            return;
        }
        int i = meta_list.indexOf(old);
        meta_list.remove(i);
        meta_list.add(i, obj);
    }

    /**
     * This methode remove the MCRMetaElement selected by tag from the list.
     * 
     * @return true if set was succesful, otherwise false
     */
    public final MCRMetaElement removeMetadataElement(String tag) {
        MCRMetaElement old = getMetadataElement(tag);

        if (old != null) {
            meta_list.remove(old);
            return old;
        }
        return null;
    }

    /**
     * This methode remove the MCRMetaElement selected a index from the list.
     * 
     * @return true if set was succesful, otherwise false
     */
    public final MCRMetaElement removeMetadataElement(int index) {
        if (index < 0 || index > size()) {
            return null;
        }

        return meta_list.remove(index);
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a list of relevant DOM elements for the metadata
     * @exception MCRException
     *                if a problem occured
     */
    public final void setFromDOM(org.jdom2.Element element) throws MCRException {
        List<Element> elements_list = element.getChildren();
        meta_list.clear();
        for (Element sub : elements_list) {
            MCRMetaElement obj = new MCRMetaElement();
            obj.setFromDOM(sub);
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
    public final org.jdom2.Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("MCRObjectMetadata : The content is not valid.");
        }

        Element elm = new Element("metadata");
        for (MCRMetaElement e : this) {
            elm.addContent(e.createXML(herited_xml));
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
        for (MCRMetaElement e : this) {
            if (!e.isValid()) {
                Logger.getLogger(MCRObjectMetadata.class).warn(e.getTag() + " is not valid.");
                return false;
            }
        }
        return true;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        for (MCRMetaElement sub : this) {
            sub.debug();
        }
    }

    @Override
    public Iterator<MCRMetaElement> iterator() {
        return meta_list.iterator();
    }
}
