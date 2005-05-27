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

import org.jdom.Content;
import org.jdom.Namespace;
import org.mycore.common.MCRException;

/**
 * This class implements all methods for handling with the MCRMetaPersonName
 * part of a metadata object. The MCRMetaPersonName class represents a natural
 * person specified by a list of names.
 * 
 * @author J. Vogler
 * @version $Revision$ $Date$
 */
final public class MCRMetaPersonName extends MCRMetaDefault implements
        MCRMetaInterface {

    // MetaPerson data
    private String firstname;

    private String callname;

    private String surname;

    private String fullname;

    private String academic;

    private String peerage;

    private String prefix;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts are set to
     * an empty string.
     */
    public MCRMetaPersonName() {
        super();
        firstname = "";
        callname = "";
        surname = "";
        fullname = "";
        academic = "";
        peerage = "";
        prefix = "";
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type<em>, if it is null, an empty string was set
     * to the type element. The firstname, callname, surname, academic and
     * peerage element was set to the value of <em>set_...<em>, if they are null,
     * an empty string was set to this element.
     *
     * @param set_datapart     the global part of the elements like 'metadata'
     *                         or 'service'
     * @param set_subtag      the name of the subtag
     * @param default_lang    the default language
     * @param set_type        the optional type string
     * @param set_inherted    a value >= 0
     * @param set_firstname   the first name
     * @param set_callname    the callname
     * @param set_surname     the surname
     * @param set_fullname    the fullname
     * @param set_academic    the academic title
     * @param set_peerage     the peerage title
     * @param set_prefix      the prefix title
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaPersonName(String set_datapart, String set_subtag,
            String default_lang, String set_type, int set_inherted,
            String set_firstname, String set_callname, String set_surname,
            String set_fullname, String set_academic, String set_peerage,
            String set_prefix) throws MCRException {
        super(set_datapart, set_subtag, default_lang, set_type, set_inherted);
        firstname = "";
        callname = "";
        surname = "";
        fullname = "";
        academic = "";
        peerage = "";
        prefix = "";
        set(set_firstname, set_callname, set_surname, set_fullname,
                set_academic, set_peerage, set_prefix);
    }

    /**
     * This methode set all name componets.
     * 
     * @param set_firstname
     *            the first name
     * @param set_callname
     *            the callname
     * @param set_surname
     *            the surname
     * @param set_fullname
     *            the fullname
     * @param set_academic
     *            the academic title
     * @param set_peerage
     *            the peerage title
     * @param set_prefix
     *            the prefix title
     */
    public final void set(String set_firstname, String set_callname,
            String set_surname, String set_fullname, String set_academic,
            String set_peerage, String set_prefix) {
        if ((set_firstname == null) || (set_callname == null)
                || (set_surname == null) || (set_fullname == null)
                || (set_academic == null) || (set_peerage == null)
                || (set_prefix == null)) {
            throw new MCRException("One or more parameter(s) are null.");
        }
        firstname = set_firstname.trim();
        callname = set_callname.trim();
        surname = set_surname.trim();
        fullname = set_fullname.trim();
        academic = set_academic.trim();
        peerage = set_peerage.trim();
        prefix = set_prefix.trim();
    }

    /**
     * This method get the firstname text element.
     * 
     * @return the firstname
     */
    public final String getFirstName() {
        return firstname;
    }

    /**
     * This method get the callname text element.
     * 
     * @return the callname
     */
    public final String getCallName() {
        return callname;
    }

    /**
     * This method get the surname text element.
     * 
     * @return the surname
     */
    public final String getSurName() {
        return surname;
    }

    /**
     * This method get the fullname text element.
     * 
     * @return the fullname
     */
    public final String getFullName() {
        return fullname;
    }

    /**
     * This method get the academic text element.
     * 
     * @return the academic
     */
    public final String getAcademic() {
        return academic;
    }

    /**
     * This method get the peerage text element.
     * 
     * @return the peerage
     */
    public final String getPeerage() {
        return peerage;
    }

    /**
     * This method get the prefix text element.
     * 
     * @return the prefix
     */
    public final String getPrefix() {
        return prefix;
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    public final void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);
        firstname = element.getChildTextTrim("firstname");
        if (firstname == null) {
            firstname = "";
        }
        callname = element.getChildTextTrim("callname");
        if (callname == null) {
            callname = "";
        }
        surname = element.getChildTextTrim("surname");
        if (surname == null) {
            surname = "";
        }
        fullname = element.getChildTextTrim("fullname");
        if (fullname == null) {
            fullname = "";
        }
        academic = element.getChildTextTrim("academic");
        if (academic == null) {
            academic = "";
        }
        peerage = element.getChildTextTrim("peerage");
        if (peerage == null) {
            peerage = "";
        }
        prefix = element.getChildTextTrim("prefix");
        if (prefix == null) {
            prefix = "";
        }
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaPersonName definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaPersonName part
     */
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            debug();
            throw new MCRException(
                    "The content of MCRMetaPersonName is not valid.");
        }
        org.jdom.Element elm = new org.jdom.Element(subtag);
        elm.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
        elm.setAttribute("inherited", (new Integer(inherited)).toString());
        if ((type != null) && ((type = type.trim()).length() != 0)) {
            elm.setAttribute("type", type);
        }
        if ((firstname = firstname.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("firstname")
                    .addContent(firstname));
        }
        if ((callname = callname.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("callname")
                    .addContent(callname));
        }
        if ((fullname = fullname.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("fullname")
                    .addContent(fullname));
        }
        if ((surname = surname.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("surname")
                    .addContent(surname));
        }
        if ((academic = academic.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("academic")
                    .addContent(academic));
        }
        if ((peerage = peerage.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("peerage")
                    .addContent(peerage));
        }
        if ((prefix = prefix.trim()).length() != 0) {
            elm.addContent((Content) new org.jdom.Element("prefix")
                    .addContent(prefix));
        }
        return elm;
    }

    /**
     * This methode create a typed content list for all data in this instance.
     * 
     * @param parasearch
     *            true if the data should parametric searchable
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a MCRTypedContent with the data of the MCRObject data
     */
    public final MCRTypedContent createTypedContent(boolean parasearch)
            throws MCRException {
        if (!isValid()) {
            debug();
            throw new MCRException(
                    "The content of MCRMetaPersonName is not valid.");
        }
        MCRTypedContent tc = new MCRTypedContent();
        if (!parasearch) {
            return tc;
        }
        tc.addTagElement(MCRTypedContent.TYPE_SUBTAG, subtag);
        tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE, "lang", lang);
        if ((type = type.trim()).length() != 0) {
            tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE, "type", type);
        }
        if ((firstname = firstname.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "firstname");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, firstname);
        }
        if ((callname = callname.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "callname");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, callname);
        }
        if ((surname = surname.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "surname");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, surname);
        }
        if ((fullname = fullname.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "fullname");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, fullname);
        }
        if ((academic = academic.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "academic");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, academic);
        }
        if ((peerage = peerage.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "peerage");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, peerage);
        }
        if ((prefix = prefix.trim()).length() != 0) {
            tc.addTagElement(MCRTypedContent.TYPE_SUB2TAG, "prefix");
            tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, prefix);
        }
        return tc;
    }

    /**
     * This methode create a String for all text searchable data in this
     * instance.
     * 
     * @param textsearch
     *            true if the data should text searchable
     * @exception MCRException
     *                if the content of this class is not valid
     * @return an empty String, because the content is not text searchable.
     */
    public final String createTextSearch(boolean textsearch)
            throws MCRException {
        if (textsearch) {
            StringBuffer sb = new StringBuffer(128);
            sb.append(academic).append(' ').append(peerage).append(' ').append(
                    firstname).append(' ').append(prefix).append(' ').append(
                    surname).append(NL);
            return sb.toString();
        }
        return "";
    }

    /**
     * This method checks the validation of the content of this class. The
     * method returns <em>false</em> if
     * <ul>
     * <li>the firstname is empty and
     * <li>the surname is empty
     * </ul>
     * otherwise the method returns <em>true</em>.
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        if ((firstname = firstname.trim()).length() == 0) {
            firstname = callname;
        }
        if ((callname = callname.trim()).length() == 0) {
            callname = firstname;
        }
        if (((firstname = firstname.trim()).length() == 0)
                && ((surname = surname.trim()).length() == 0)) {
            return false;
        }
        if ((fullname = fullname.trim()).length() == 0) {
            StringBuffer sb = new StringBuffer(128);
            sb.append(academic).append(' ').append(peerage).append(' ').append(
                    firstname).append(' ').append(prefix).append(' ').append(
                    surname);
            fullname = sb.toString();
        }
        return true;
    }

    /**
     * This method make a clone of this class.
     */
    public final Object clone() {
        MCRMetaPersonName out = new MCRMetaPersonName(datapart, subtag, lang,
                type, inherited, firstname, callname, surname, fullname,
                academic, peerage, prefix);
        return (Object) out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    public final void debug() {
        logger.debug("Start Class : MCRMetaPersonName");
        super.debugDefault();
        logger.debug("Firstname          = " + firstname);
        logger.debug("Callname           = " + callname);
        logger.debug("Surname            = " + surname);
        logger.debug("Fullname           = " + fullname);
        logger.debug("Academic           = " + academic);
        logger.debug("Peerage            = " + peerage);
        logger.debug("Prefix             = " + prefix);
        logger.debug("Stop");
        logger.debug("");
    }

}

