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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class is designed to to have a basic class for all metadata. The class
 * has inside a ArrayList that holds all metaddata elements for one XML tag.
 * Furthermore, this class supports the linking of a document owing this
 * metadata element to another document, the id of which is given in the
 * xlink:href attribute of the MCRMetaLink representing the link. The class name
 * of such a metadata element must be MCRMetaLink, and the metadata element is
 * considered to be a folder of links.
 * 
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
 */
public class MCRMetaElement implements Iterable<MCRMetaInterface>, Cloneable {
    // common data
    protected final static MCRConfiguration CONFIG = MCRConfiguration.instance();

    public final static String DEFAULT_LANGUAGE = CONFIG.getString("MCR.Metadata.DefaultLang",
        MCRConstants.DEFAULT_LANG);

    public final static boolean DEFAULT_HERITABLE = CONFIG.getBoolean("MCR.MetaElement.defaults.heritable", false);

    public final static boolean DEFAULT_NOT_INHERIT = CONFIG.getBoolean("MCR.MetaElement.defaults.notinherit", true);

    private String META_PACKAGE_NAME = "org.mycore.datamodel.metadata.";

    // logger
    static Logger LOGGER = LogManager.getLogger();

    private Class<? extends MCRMetaInterface> clazz = null;

    private String tag = null;

    private boolean heritable;

    private boolean notinherit;

    private ArrayList<MCRMetaInterface> list = null;

    /**
     * This is the constructor of the MCRMetaElement class. The default language
     * for the element was set to <b>MCR.Metadata.DefaultLang</b>.
     */
    public MCRMetaElement() {
        list = new ArrayList<MCRMetaInterface>();
        heritable = DEFAULT_HERITABLE;
        notinherit = DEFAULT_NOT_INHERIT;
    }

    /**
     * This is the constructor of the MCRMetaElement class.
     * @param set_tag
     *            the name of this tag
     * @param set_heritable
     *            set this flag to true if all child objects of this element can
     *            inherit this data
     * @param set_notinherit
     *            set this flag to true if this element should not inherit from
     *            his parent object
     * @param set_list
     *            a list of MCRMeta... data lines to add in this element (can be null)
     */
    public MCRMetaElement(Class<? extends MCRMetaInterface> clazz, String set_tag, boolean set_heritable,
        boolean set_notinherit,
        List<? extends MCRMetaInterface> set_list) {
        this();
        this.clazz = clazz;
        setTag(set_tag);
        heritable = set_heritable;
        notinherit = set_notinherit;

        if (set_list != null) {
            list.addAll(set_list);
        }
    }

    /**
     * This methode return the name of this metadata class as string.
     * 
     * @return the name of this metadata class as string
     */
    public final String getClassName() {
        return getClazz().getSimpleName();
    }

    /**
     * This method returns the instance of an element from the list with index
     * i.
     * 
     * @return the instance of an element, if index is out of range return null
     */
    public final MCRMetaInterface getElement(int index) {
        if ((index < 0) || (index > list.size())) {
            return null;
        }

        return (MCRMetaInterface) list.get(index);
    }

    /**
     * This method returns the instance of an element from the list with the given 
     * name
     * 
     * @return the instance of the element with the given name or null if there is no such element
     * */
    public final MCRMetaInterface getElementByName(String name) {
        for (MCRMetaInterface sub : this) {
            if (sub.getSubTag().equals(name)) {
                return sub;
            }
        }
        return null;
    }

    /**
     * This methode return the heritable flag of this metadata as boolean value.
     * 
     * @return the heritable flag of this metadata class
     */
    public final boolean isHeritable() {
        return heritable;
    }

    /**
     * This methode return the nonherit flag of this metadata as boolean value.
     * 
     * @return the notherit flag of this metadata class
     */
    public final boolean inheritsNot() {
        return notinherit;
    }

    /**
     * This methode return the tag of this metadata class as string.
     * 
     * @return the tag of this metadata class as string
     */
    public final String getTag() {
        return tag;
    }

    /**
     * This methode set the heritable flag for the metadata class.
     * 
     * @param heritable
     *            the heritable flag as boolean value
     */
    public void setHeritable(boolean heritable) {
        this.heritable = heritable;
    }

    /**
     * This methode set the notinherit flag for the metadata class.
     * 
     * @param notinherit
     *            the notinherit flag as boolean value
     */
    public void setNotInherit(boolean notinherit) {
        this.notinherit = notinherit;
    }

    /**
     * This methode set the tag for the metadata class.
     * 
     * @param tag
     *            the tag for the metadata class
     */
    public void setTag(String tag) {
        if (tag == null || (tag = tag.trim()).length() == 0) {
            return;
        }

        this.tag = tag;
    }

    /**
     * This methode set the element class for the metadata elements.
     * 
     * @param clazz
     *            the class for the metadata elements
     */
    public final void setClass(Class<? extends MCRMetaInterface> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends MCRMetaInterface> getClazz() {
        return clazz;
    }

    /**
     * <em>size</em> returns the number of elements in this instance.
     * 
     * @return int the size of "list"
     */
    public final int size() {
        return list.size();
    }

    /**
     * The method add a metadata object, that implements the MCRMetaInterface to
     * this element.
     * 
     * @param obj
     *            a metadata object
     * @exception MCRException
     *                if the class name of the object is not the same like the
     *                name of all store metadata in this element.
     */
    public final void addMetaObject(MCRMetaInterface obj) {
        list.add(obj);
    }

    /**
     * This method remove the instance of an element from the list with index
     * i.
     * 
     * @return true if the instance is removed, otherwise return else
     */
    public final boolean removeElement(int index) {
        if ((index < 0) || (index > list.size())) {
            return false;
        }
        list.remove(index);
        return true;
    }

    /**
     * The method remove a metadata object, that implements the MCRMetaInterface to
     * this element.
     * 
     * @param obj
     *            a metadata object
     * @exception MCRException
     *                if the class name of the object is not the same like the
     *                name of all store metadata in this element.
     * @return true if this <code>MCRMetaElement</code> contained the specified
     *              <code>MCRMetaInterface</code>
     */
    public final boolean removeMetaObject(MCRMetaInterface obj) {
        return list.remove(obj);
    }

    /**
     * The method removes all inherited metadata objects of this MCRMetaElement.
     */
    public final void removeInheritedMetadata() {
        for (int i = 0; i < size(); i++) {
            if ((list.get(i)).getInherited() > 0) {
                list.remove(i);
                i--;
            }
        }
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     * @exception MCRException
     *                if the class can't loaded
     */
    @SuppressWarnings("unchecked")
    public final void setFromDOM(org.jdom2.Element element) throws MCRException {

        String fullname;
        Class<? extends MCRMetaInterface> forName;
        try {
            String classname = element.getAttributeValue("class");
            if (classname == null) {
                throw new MCRException("Missing required class attribute in element " + element.getName());
            }
            fullname = META_PACKAGE_NAME + classname;
            forName = (Class<? extends MCRMetaInterface>) Class.forName(fullname);
            setClass(forName);
        } catch (ClassNotFoundException e) {
            throw new MCRException(e);
        }
        tag = element.getName();
        String heritable = element.getAttributeValue("heritable");
        if (heritable != null)
            setHeritable(Boolean.valueOf(heritable));

        String notInherit = element.getAttributeValue("notinherit");
        if (notInherit != null)
            setNotInherit(Boolean.valueOf(notInherit));

        List<Element> element_list = element.getChildren();
        for (Element anElement_list : element_list) {
            Element subtag = (Element) anElement_list;
            MCRMetaInterface obj;

            try {
                obj = forName.newInstance();
                obj.setFromDOM(subtag);
            } catch (IllegalAccessException e) {
                throw new MCRException(fullname + " IllegalAccessException");
            } catch (InstantiationException e) {
                throw new MCRException(fullname + " InstantiationException");
            }

            list.add(obj);
        }
    }

    /**
     * This methode create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRLangText definition for the given subtag.
     * 
     * @param flag
     *            true if all inherited data should be include, else false
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML Element part
     */
    public final Element createXML(boolean flag) throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            debug();
            throw new MCRException("MCRMetaElement : The content is not valid: Tag=" + this.tag, exc);
        }
        Element elm = new Element(tag);
        elm.setAttribute("class", getClassName());
        elm.setAttribute("heritable", String.valueOf(heritable));
        elm.setAttribute("notinherit", String.valueOf(notinherit));
        list
            .stream()
            .filter(metaInterface -> (flag || metaInterface.getInherited() == 0))
            .map(MCRMetaInterface::createXML)
            .forEachOrdered(elm::addContent);
        return elm;
    }

    /**
     * Creates the JSON representation of this metadata element.
     * 
     * <pre>
     *   {
     *      class: 'MCRMetaLangText',
     *      heritable: true,
     *      notinherit: false,
     *      data: [
     *        {@link MCRMetaInterface#createJSON()},
     *        ...
     *      ]
     *   }
     * </pre>
     * 
     * @return a json gson representation of this metadata element
     */
    public JsonObject createJSON(boolean flag) {
        JsonObject meta = new JsonObject();
        meta.addProperty("class", getClassName());
        meta.addProperty("heritable", isHeritable());
        meta.addProperty("notinherit", notinherit);
        JsonArray data = new JsonArray();
        list
            .stream()
            .filter(metaInterface -> (flag || metaInterface.getInherited() == 0))
            .map(MCRMetaInterface::createJSON)
            .forEachOrdered(data::add);
        meta.add("data", data);
        return meta;
    }

    /**
     * This methode check the validation of the content of this class. The
     * methode returns <em>true</em> if
     * <ul>
     * <li>the classname is not null or empty
     * <li>the tag is not null or empty
     * <li>if the list is empty
     * <li>the lang value was supported
     * </ul>
     * otherwise the methode return <em>false</em>
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The '" + getTag() + "' is invalid.", exc);
        }
        return false;
    }

    /**
     * Validates this MCRMetaElement. This method throws an exception if:
     * <ul>
     * <li>the classname is not null or empty</li>
     * <li>the tag is not null or empty</li>
     * <li>if the list is empty</li>
     * <li>the lang value was supported</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaElement is invalid
     */
    public void validate() throws MCRException {
        if (tag == null || (tag = tag.trim()).length() == 0) {
            throw new MCRException("No tag name defined!");
        }
        if (clazz == null) {
            throw new MCRException(getTag() + ": @class is not defined");
        }
        if (!clazz.getPackage().getName().equals(META_PACKAGE_NAME.substring(0, META_PACKAGE_NAME.length() - 1))) {
            throw new MCRException(
                getTag() + ": package " + clazz.getPackage().getName() + " does not equal " + META_PACKAGE_NAME);
        }
        if (list.size() == 0) {
            throw new MCRException(getTag() + ": does not contain any sub elements");
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public final MCRMetaElement clone() {
        MCRMetaElement out = new MCRMetaElement();
        out.setClass(getClazz());
        out.setTag(tag);
        out.setHeritable(heritable);
        out.setNotInherit(notinherit);

        for (int i = 0; i < size(); i++) {
            MCRMetaInterface mif = (MCRMetaInterface) (list.get(i)).clone();
            out.addMetaObject(mif);
        }

        return out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ClassName          = " + getClassName());
            LOGGER.debug("Tag                = " + tag);
            LOGGER.debug("Heritable          = " + String.valueOf(heritable));
            LOGGER.debug("NotInherit         = " + String.valueOf(notinherit));
            LOGGER.debug("Elements           = " + String.valueOf(list.size()));
            LOGGER.debug(" ");
            for (MCRMetaInterface aList : list) {
                aList.debug();
            }
        }
    }

    @Override
    public Iterator<MCRMetaInterface> iterator() {
        return list.iterator();
    }

}
