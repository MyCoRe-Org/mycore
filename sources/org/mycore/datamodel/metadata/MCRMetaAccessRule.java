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

import org.jdom.Namespace;
import org.mycore.common.MCRException;

/**
 * This class implements all method for handling with the MCRMetaAccessRule part
 * of a metadata object. The MCRMetaAccessRule class present a single item,
 * which hold an ACL condition for a defined pool.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRMetaAccessRule extends MCRMetaDefault implements MCRMetaInterface {
    // MCRMetaAccessRule data
    protected org.jdom.Element condition;

    protected String pool;

    /**
     * This is the constructor. <br>
     * The constructor of the MCRMetaDefault runs. The <em>pool</em> Attribute
     * is set to 'READ'. The <b>condition</b> is set to 'null'.
     */
    public MCRMetaAccessRule() {
        super();
        condition = null;
        pool = "READ";
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en</b> was set. This is not use in other methods
     * of this class. The subtag element was set to the value of
     * <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception will be throwed. The type element was set to
     * the value of <em>set_type<em>, if it is null, an empty string was set
     * to the type element. The condition element was set to the value of
     * <em>set_condition<em>, if it is null, an exception will be throwed.
     *
     * @param set_datapart     the global part of the elements like 'metadata'
     *                         or 'service'
     * @param set_subtag       the name of the subtag
     * @param default_lang     the default language
     * @param set_type         the optional type string
     * @param set_inherted     a value >= 0
     * @param_set_pool         the format string, if it is empty 'READ' will be set.
     * @param set_condition    the JDOM Element included the condition tree
     * @exception MCRException if the set_subtag value or set_condition is null or empty
     */
    public MCRMetaAccessRule(String set_datapart, String set_subtag, String default_lang, String set_type, int set_inherted, String set_pool, org.jdom.Element set_condition) throws MCRException {
        super(set_datapart, set_subtag, default_lang, set_type, set_inherted);
        pool = set_pool;
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            pool = "READ";
        } else {
            pool = pool.trim();
        }
        if ((set_condition == null) || (!set_condition.getName().equals("condition"))) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        condition = set_condition;
    }

    /**
     * This method set the pool and the condition.
     * 
     * @param_set_pool the format string, if it is empty 'READ' will be set.
     * @param set_condition
     *            the JDOM Element included the condition tree
     * @exception MCRException
     *                if the set_condition is null or empty
     */
    public final void set(String set_pool, org.jdom.Element set_condition) throws MCRException {
        setLang("en");
        setType("");
        pool = set_pool;
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            pool = "READ";
        } else {
            pool = pool.trim();
        }
        if ((set_condition == null) || (!set_condition.getName().equals("condition"))) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        condition = set_condition;
    }

    /**
     * This method set the condition.
     * 
     * @param set_condition
     *            the JDOM Element included the condition tree
     * @exception MCRException
     *                if the set_condition is null or empty
     */
    public final void setCondition(org.jdom.Element set_condition) throws MCRException {
        if ((set_condition == null) || (!set_condition.getName().equals("condition"))) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        condition = set_condition;
    }

    /**
     * This method set the pool attribute.
     * 
     * @param set_pool
     *            the new pool string, if it is empty 'READ' will be set.
     */
    public final void setPool(String set_pool) {
        pool = set_pool;
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            pool = "READ";
        } else {
            pool = pool.trim();
        }
    }

    /**
     * This method get the condition.
     * 
     * @return the condition as JDOM Element
     */
    public final org.jdom.Element getCondition() {
        return condition;
    }

    /**
     * This method get the pool attribute.
     * 
     * @return the pool attribute
     */
    public final String getPool() {
        return pool;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);

        org.jdom.Element temp_condition = element.getChild("condition");
        if (temp_condition == null) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        condition = (org.jdom.Element)temp_condition.detach();

        String temp_pool = (String) element.getAttributeValue("pool");
        if (temp_pool == null) {
            temp_pool = "READ";
        }
        pool = temp_pool.trim();
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaAccessRule definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaAccessRule part
     */
    public org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            debug();
            throw new MCRException("The content of MCRMetaAccessRule is not valid.");
        }
        org.jdom.Element elm = new org.jdom.Element(subtag);
        elm.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        elm.setAttribute("inherited", (new Integer(inherited)).toString());
        if ((pool != null) && ((pool = pool.trim()).length() != 0)) {
            elm.setAttribute("pool", pool);
        }
        if ((type != null) && ((type = type.trim()).length() != 0)) {
            elm.setAttribute("type", type);
        }
        elm.addContent(condition.detach());
        return elm;
    }

    /**
     * This methode create a String for all text searchable data in this
     * instance.
     * 
     * @param textsearch
     *            true if the data should text searchable
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a String with the text value
     */
    public String createTextSearch(boolean textsearch) throws MCRException {
        return "";
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the subtag is not null or empty
     * <li>the text is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }
        if (condition == null) {
            return false;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return false;
        }
        return true;
    }

    /**
     * This method make a clone of this class.
     */
    public Object clone() {
        MCRMetaAccessRule out = new MCRMetaAccessRule(datapart, subtag, lang, type, inherited, pool, condition);
        return (Object) out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        LOGGER.debug("Start Class : MCRMetaAccessRule");
        super.debugDefault();
        LOGGER.debug("Pool               = " + pool);
        LOGGER.debug("Rule               = " + "condition");
    }
}
