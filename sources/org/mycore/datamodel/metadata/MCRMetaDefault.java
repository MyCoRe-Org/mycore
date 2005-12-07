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

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

/**
 * This class implements any methods for handling the basic data for all
 * metadata classes of the metadata objects. The methods createXML() and
 * createTypedContent() and createTextSearch() are abstract methods.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public abstract class MCRMetaDefault {
    // public data
    public static final int DEFAULT_LANG_LENGTH = 12;

    public static final int DEFAULT_TYPE_LENGTH = 256;

    public static final int DEFAULT_STRING_LENGTH = 4096;

    // common data
    protected static String NL = new String((System.getProperties()).getProperty("line.separator"));

    protected static String DEFAULT_LANGUAGE = "en";

    protected static String DEFAULT_DATAPART = "metadata";

    protected static int DEFAULT_INHERITED = 0;

    // logger
    static Logger LOGGER = Logger.getLogger(MCRMetaDefault.class.getName());

    // MetaLangText data
    protected String subtag;

    protected String lang;

    protected String type;

    protected int inherited;

    protected String datapart;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The datapart element was set
     * to <b>metadata <b>All other elemnts was set to an empty string. The
     * inherited value is set to 0!
     */
    public MCRMetaDefault() {
        lang = DEFAULT_LANGUAGE;
        subtag = "";
        type = "";
        inherited = DEFAULT_INHERITED;
        datapart = DEFAULT_DATAPART;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * empty or false <b>en </b> was set. The datapart was set to default. All
     * other elemnts was set to an empty string. The inherited value is set to
     * 0!
     * 
     * @param default_lang
     *            the default language
     */
    public MCRMetaDefault(String default_lang) {
        lang = DEFAULT_LANGUAGE;

        if ((default_lang != null) && ((default_lang = default_lang.trim()).length() != 0)) {
            lang = default_lang;
        }

        subtag = "";
        type = "";
        inherited = DEFAULT_INHERITED;
        datapart = DEFAULT_DATAPART;
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type<em>, if it is null, an empty string was set
     * to the type element. The datapart element was set. If the value of
     * <em>set_datapart,/em> is null or empty the default was set.
     *
     * @param set_datapart     the data part name
     * @param set_subtag       the name of the subtag
     * @param default_lang     the default language
     * @param set_type         the optional type string
     * @param set_inherted     a int value , > 0 if the data are inherited,
     *                         else = 0.
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaDefault(String set_datapart, String set_subtag, String default_lang, String set_type, int set_inherited) throws MCRException {
        lang = DEFAULT_LANGUAGE;
        subtag = "";
        type = "";
        setInherited(set_inherited);

        if ((set_subtag == null) || ((set_subtag = set_subtag.trim()).length() == 0)) {
            throw new MCRException("The set_subtag is null or empty.");
        }

        subtag = set_subtag;

        if ((default_lang != null) && ((default_lang = default_lang.trim()).length() != 0)) {
            lang = default_lang;
        }

        if (set_type != null) {
            type = set_type;
        }

        if ((set_datapart != null) && ((set_datapart = set_datapart.trim()).length() != 0)) {
            datapart = set_datapart;
        }
    }

    /**
     * This method set the inherited level. This can be 0 or an integer higher
     * 0.
     * 
     * @param value
     *            the inherited level value, if it is < 0, 0 was set
     */
    public final void setInherited(int value) {
        if (value < 0) {
            inherited = 0;
        } else {
            inherited = value;
        }
    }

    /**
     * This method increments the inherited value with 1.
     */
    public final void incrementInherited() {
        inherited++;
    }

    /**
     * This method decrements the inherited value with 1.
     */
    public final void decrementInherited() {
        if (inherited > 0) {
            inherited--;
        }
    }

    /**
     * This method set the language element. If the value of
     * <em>default_lang</em> is null, empty or false nothing was changed.
     * 
     * @param default_lang
     *            the default language
     */
    public final void setLang(String default_lang) {
        if ((default_lang == null) || ((default_lang = default_lang.trim()).length() == 0)) {
            lang = DEFAULT_LANGUAGE;
        } else {
            lang = default_lang;
        }
    }

    /**
     * This method set the subtag element. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed.
     * 
     * @param set_subtag
     *            the subtag
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    public final void setSubTag(String set_subtag) throws MCRException {
        if ((set_subtag == null) || ((set_subtag = set_subtag.trim()).length() == 0)) {
            throw new MCRException("The set_subtag is null or empty.");
        }

        subtag = set_subtag;
    }

    /**
     * This method set the type element. If the value of <em>set_type</em> is
     * null or empty nothing was changed.
     * 
     * @param set_type
     *            the optional type
     */
    public final void setType(String set_type) {
        if (set_type == null) {
            return;
        }

        type = set_type;
    }

    /**
     * This method set the datapart element. If the value of
     * <em>set_datapart</em> is null, empty or false nothing was changed.
     * 
     * @param set_datapart
     *            the data part name
     */
    public final void setDataPart(String set_datapart) {
        if ((set_datapart == null) || ((set_datapart = set_datapart.trim()).length() == 0)) {
            datapart = DEFAULT_DATAPART;
        } else {
            datapart = set_datapart;
        }
    }

    /**
     * This method get the inherited element.
     * 
     * @return the inherited flag as int
     */
    public final int getInherited() {
        return inherited;
    }

    /**
     * This method get the inherited element.
     * 
     * @return the inherited flag as string
     */
    public final String getInheritedToString() {
        return (new Integer(inherited)).toString();
    }

    /**
     * This method get the language element.
     * 
     * @return the language
     */
    public final String getLang() {
        return lang;
    }

    /**
     * This method get the subtag element.
     * 
     * @return the subtag
     */
    public final String getSubTag() {
        return subtag;
    }

    /**
     * This method get the type element.
     * 
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * This method get the datapart element.
     * 
     * @return the datapart
     */
    public final String getDataPart() {
        return datapart;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant DOM element for the metadata
     * @exception MCRException
     *                if the set_subtag value is null or empty
     */
    public void setFromDOM(org.jdom.Element element) throws MCRException {
        if (element == null) {
            return;
        }

        String temp_subtag = element.getName();

        if ((temp_subtag == null) || ((temp_subtag = temp_subtag.trim()).length() == 0)) {
            throw new MCRException("The subtag is null or empty.");
        }

        subtag = temp_subtag;

        String temp_lang = element.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);

        if ((temp_lang != null) && ((temp_lang = temp_lang.trim()).length() != 0)) {
            lang = temp_lang;
        }

        String temp_type = (String) element.getAttributeValue("type");

        if ((temp_type != null) && ((temp_type = temp_type.trim()).length() != 0)) {
            type = temp_type;
        }

        String temp_herit = (String) element.getAttributeValue("inherited");

        if ((temp_herit != null) && ((temp_herit = temp_herit.trim()).length() != 0)) {
            try {
                inherited = Integer.parseInt(temp_herit);
            } catch (NumberFormatException e) {
                inherited = 0;
            }
        }
    }

    /**
     * This abstract method create a XML stream for all data in this class,
     * defined by the MyCoRe XML MCRMeta... definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMeta... part
     */
    public abstract org.jdom.Element createXML() throws MCRException;

    /**
     * This abstract method create a String for the text searchable data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a String with the text search data
     */
    public abstract String createTextSearch(boolean textsearch) throws MCRException;

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the subtag is not null or empty
     * <li>the lang value was supported
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public boolean isValid() {
        if ((subtag == null) || ((subtag = subtag.trim()).length() == 0)) {
            return false;
        }

        if (!MCRUtils.isSupportedLang(lang)) {
            return false;
        }

        return true;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debugDefault() {
        LOGGER.debug("SubTag             = " + subtag);
        LOGGER.debug("Language           = " + lang);
        LOGGER.debug("Type               = " + type);
        LOGGER.debug("DataPart           = " + datapart);
        LOGGER.debug("Inhreited          = " + String.valueOf(inherited));
    }
}
