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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;

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
 */
public class MCRMetaElement implements Iterable<MCRMetaInterface>, Cloneable {
    // common data
    public static final String DEFAULT_LANGUAGE = MCRConfiguration2.getString("MCR.Metadata.DefaultLang")
        .orElse(MCRConstants.DEFAULT_LANG);

    public static final boolean DEFAULT_HERITABLE = MCRConfiguration2.getBoolean("MCR.MetaElement.defaults.heritable")
        .orElse(false);

    public static final boolean DEFAULT_NOT_INHERIT = MCRConfiguration2
        .getBoolean("MCR.MetaElement.defaults.notinherit").orElse(true);

    private static final String META_PACKAGE_NAME = "org.mycore.datamodel.metadata.";

    private static final Logger LOGGER = LogManager.getLogger();

    private Class<? extends MCRMetaInterface> clazz;

    private String tag;

    private boolean heritable;

    private boolean notinherit;

    private List<MCRMetaInterface> list;

    /**
     * This is the constructor of the MCRMetaElement class. The default language
     * for the element was set to <b>MCR.Metadata.DefaultLang</b>.
     */
    public MCRMetaElement() {
        list = new ArrayList<>();
        heritable = DEFAULT_HERITABLE;
        notinherit = DEFAULT_NOT_INHERIT;
    }

    /**
     * This is the constructor of the MCRMetaElement class.
     * @param tag
     *            the name of this tag
     * @param heritable
     *            set this flag to true if all child objects of this element can
     *            inherit this data
     * @param notinherit
     *            set this flag to true if this element should not inherit from
     *            his parent object
     * @param list
     *            a list of MCRMeta... data lines to add in this element (can be null)
     */
    public MCRMetaElement(Class<? extends MCRMetaInterface> clazz, String tag, boolean heritable, boolean notinherit,
        List<? extends MCRMetaInterface> list) {
        this();
        this.clazz = clazz;
        setTag(tag);
        this.heritable = heritable;
        this.notinherit = notinherit;

        if (list != null) {
            this.list.addAll(list);
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
        return list.get(index);
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
        MCRUtils.filterTrimmedNotEmpty(tag)
            .ifPresent(s -> this.tag = s);
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
     * returns true if there are no elements in this instance.
     *
     * @return true, if the "list" is empty
     */
    public final boolean isEmpty() {
        return list.isEmpty();
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
        list.removeIf(mi -> mi.getInherited() > 0);
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
    public final void setFromDOM(Element element) throws MCRException {
        String fullName;
        Class<? extends MCRMetaInterface> forName;
        try {
            String classname = element.getAttributeValue(MCRXMLConstants.CLASS);
            if (classname == null) {
                throw new MCRException("Missing required class attribute in element " + element.getName());
            }
            fullName = META_PACKAGE_NAME + classname;
            forName = MCRClassTools.forName(fullName);
            setClass(forName);
        } catch (ClassNotFoundException e) {
            throw new MCRException(e);
        }
        tag = element.getName();
        String heritable = element.getAttributeValue(MCRXMLConstants.HERITABLE);
        if (heritable != null) {
            setHeritable(Boolean.parseBoolean(heritable));
        }

        String notInherit = element.getAttributeValue(MCRXMLConstants.NOT_INHERIT);
        if (notInherit != null) {
            setNotInherit(Boolean.parseBoolean(notInherit));
        }

        List<Element> elementList = element.getChildren();
        for (Element subtag : elementList) {
            MCRMetaInterface obj;
            try {
                obj = forName.getDeclaredConstructor().newInstance();
                obj.setFromDOM(subtag);
            } catch (ReflectiveOperationException e) {
                throw new MCRException(fullName + " ReflectiveOperationException", e);
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
        elm.setAttribute(MCRXMLConstants.CLASS, getClassName());
        elm.setAttribute(MCRXMLConstants.HERITABLE, String.valueOf(heritable));
        elm.setAttribute(MCRXMLConstants.NOT_INHERIT, String.valueOf(notinherit));
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
            LOGGER.warn(() -> "The '" + getTag() + "' is invalid.", exc);
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
        tag = MCRUtils.filterTrimmedNotEmpty(tag).orElse(null);
        if (tag == null) {
            throw new MCRException("No tag name defined!");
        }
        if (clazz == null) {
            throw new MCRException(getTag() + ": @class is not defined");
        }
        if (!clazz.getPackage().getName().equals(META_PACKAGE_NAME.substring(0, META_PACKAGE_NAME.length() - 1))) {
            throw new MCRException(
                getTag() + ": package " + clazz.getPackage().getName() + " does not equal " + META_PACKAGE_NAME);
        }
        if (list.isEmpty()) {
            throw new MCRException(getTag() + ": does not contain any sub elements");
        }
    }

    @Override
    public MCRMetaElement clone() {
        MCRMetaElement clone = MCRClassTools.clone(getClass(), super::clone);

        clone.clazz = clazz;
        clone.tag = tag;
        clone.heritable = heritable;
        clone.notinherit = notinherit;
        clone.list = list.stream().map(MCRMetaInterface::clone)
            .collect(Collectors.toCollection(ArrayList::new));

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ClassName          = {}", getClassName());
            LOGGER.debug("Tag                = {}", tag);
            LOGGER.debug("Heritable          = {}", String.valueOf(heritable));
            LOGGER.debug("NotInherit         = {}", String.valueOf(notinherit));
            LOGGER.debug("Elements           = {}", String.valueOf(list.size()));
            LOGGER.debug(" ");
            for (MCRMetaInterface aList : list) {
                aList.debug();
            }
        }
    }

    /**
     * Streams the {@link MCRMetaInterface} of this element.
     *
     * @return stream of MCRMetaInterface's
     */
    public Stream<MCRMetaInterface> stream() {
        return list.stream();
    }

    @Override
    public Iterator<MCRMetaInterface> iterator() {
        return list.iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClassName(), heritable, list, notinherit, tag);
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
        final MCRMetaElement other = (MCRMetaElement) obj;
        return Objects.equals(getClassName(), other.getClassName())
            && heritable == other.heritable && notinherit == other.notinherit
            && Objects.equals(tag, other.tag) && Objects.equals(list, other.list);
    }

}
