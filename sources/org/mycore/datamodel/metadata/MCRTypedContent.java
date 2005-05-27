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
import org.mycore.common.MCRUtils;

/**
 * This class is an internal MyCoRe interface to storing the metadata in the
 * persistence layer. With this interface the persistence layer can read all
 * data for create or update a search data tree like store as TextSearch or
 * store in a parametric database and a text search. The data are in an internal
 * format that knows the data format of them. This is the different to the
 * simple XML string.
 * <p>
 * The XML data was stored in a table they has rows of <br />
 * <ul>
 * <li>type - the types MASTERTAG, TAG, SUBTAG, VALUE, ATTRIBUTE</li>
 * <li>name - the name of this MASTERTAG, TAG, SUBTAG, ATTRIBUTE</li>
 * <li>format - the data format of the VALUE or ATTRIBUTE</li>
 * <li>value - the value of the VALUE or ATTRIBUTE</li>
 * <li>tsflag - the flag for using this data in Text Search</li>
 * </ul>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRTypedContent {

    // const

    public final static int TYPE_UNDEFINED = 0;

    public final static int TYPE_VALUE = 1;

    public final static int TYPE_ATTRIBUTE = 2;

    public final static int TYPE_MASTERTAG = 3;

    public final static int TYPE_TAG = 4;

    public final static int TYPE_SUBTAG = 5;

    public final static int TYPE_SUB2TAG = 6;

    public final static int TYPE_LASTTAG = 6;

    public final static int FORMAT_UNDEFINED = 0;

    public final static int FORMAT_STRING = 1;

    public final static int FORMAT_DATE = 2;

    public final static int FORMAT_NUMBER = 3;

    public final static int FORMAT_BOOLEAN = 4;

    public final static int FORMAT_LINK = 5;

    public final static int FORMAT_CLASSID = 6;

    public final static int FORMAT_CATEGID = 7;

    private final static int FORMAT_LAST = 7;

    private final static int FORMAT_XML = 8;

    // static
    private static String default_lang;

    // data
    private ArrayList type;

    private ArrayList name;

    private ArrayList format;

    private ArrayList value;

    /**
     * The static part that read the configuration
     */
    static {
        MCRConfiguration conf = MCRConfiguration.instance();
        default_lang = conf.getString("MCR.metadata_default_lang", "en");
    }

    /**
     * This is the constructor for MCRTypedContent. The list of content is
     * empty.
     */
    public MCRTypedContent() {
        type = new ArrayList();
        name = new ArrayList();
        format = new ArrayList();
        value = new ArrayList();
    }

    /**
     * This method add a MCRTypedContent to the own data list.
     * 
     * @param in -
     *            the MCRTypedContent input list
     */
    public final void addMCRTypedContent(MCRTypedContent in) {
        for (int i = 0; i < in.getSize(); i++) {
            type.add((Object) new Integer(in.getTypeElement(i)));
            name.add(in.getNameElement(i));
            format.add((Object) new Integer(in.getFormatElement(i)));
            value.add(in.getValueElement(i));
        }
    }

    /**
     * This method add an element of the typed content list to this list.
     * 
     * @param intype
     *            one of the type constante of this class
     * @param inname
     *            a name as a String
     * @param informat
     *            one of the format constante of this class
     * @param invalue
     *            the value as an Object
     * @return false if an error was occured, else return true
     */
    public final boolean addElement(int intype, String inname, int informat,
            Object invalue) {
        if ((intype < 1) || (intype > TYPE_LASTTAG)) {
            return false;
        }
        if ((informat < 1) || (intype > FORMAT_LAST)) {
            return false;
        }
        type.add((Object) new Integer(intype));
        name.add(inname);
        format.add((Object) new Integer(informat));
        if (invalue == null) {
            value.add("");
        } else
            value.add(invalue);
        return true;
    }

    /**
     * This method add a tag name to this list.
     * 
     * @param intype
     *            TYPE_MASTERTAG or TYPE_TAG or TYPE_SUBTAG
     * @param inname
     *            a name as a String
     * @return false if an error was occured, else return true
     */
    public final boolean addTagElement(int intype, String inname) {
        if ((intype < TYPE_MASTERTAG) || (intype > TYPE_LASTTAG)) {
            return false;
        }
        if (inname == null) {
            return false;
        }
        type.add((Object) (new Integer(intype)));
        name.add(inname);
        format.add(new Integer(FORMAT_UNDEFINED));
        value.add(new String(" "));
        return true;
    }

    /**
     * This method add a Boolean value to this list.
     * 
     * @param intype
     *            TYPE_VALUE
     * @param inname
     *            a name as a String
     * @param invalue
     *            the value as a GregorianCalendar date
     * @return false if an error was occured, else return true
     */
    public final boolean addBooleanElement(boolean invalue) {
        type.add(new Integer(TYPE_VALUE));
        name.add("");
        format.add(new Integer(FORMAT_BOOLEAN));
        if (invalue) {
            value.add("true");
        } else {
            value.add("false");
        }
        return true;
    }

    /**
     * This method add a ClassificationID value to this list.
     * 
     * @param invalue
     *            the value as an String
     * @return false if an error was occured, else return true
     */
    public final boolean addClassElement(String invalue) {
        if (invalue == null) {
            return false;
        }
        type.add(new Integer(TYPE_ATTRIBUTE));
        name.add("classid");
        format.add(new Integer(FORMAT_CLASSID));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a CategoryID value to this list.
     * 
     * @param invalue
     *            the value as an String
     * @return false if an error was occured, else return true
     */
    public final boolean addCategElement(String invalue) {
        if (invalue == null) {
            return false;
        }
        type.add(new Integer(TYPE_ATTRIBUTE));
        name.add("categid");
        format.add(new Integer(FORMAT_CATEGID));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a Date value to this list.
     * 
     * @param invalue
     *            the value as a GregorianCalendar date
     * @return false if an error was occured, else return true
     */
    public final boolean addDateElement(GregorianCalendar invalue) {
        if (invalue == null) {
            return false;
        }
        type.add(new Integer(TYPE_VALUE));
        name.add("");
        format.add(new Integer(FORMAT_DATE));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a Date value to this list.
     * 
     * @param invalue
     *            the value as a GregorianCalendar date
     * @return false if an error was occured, else return true
     */
    public final boolean addDateElement(int intype, String inname,
            GregorianCalendar invalue) {
        if (invalue == null) {
            return false;
        }
        if (inname == null) {
            return false;
        }
        if (intype != TYPE_ATTRIBUTE) {
            return false;
        }
        type.add(new Integer(intype));
        name.add(inname);
        format.add(new Integer(FORMAT_DATE));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a Double value to this list.
     * 
     * @param invalue
     *            the value as a double
     * @return false if an error was occured, else return true
     */
    public final boolean addDoubleElement(double invalue) {
        type.add(new Integer(TYPE_VALUE));
        name.add("");
        format.add(new Integer(FORMAT_NUMBER));
        value.add(new Double(invalue));
        return true;
    }

    /**
     * This method add a Link value to this list.
     * 
     * @param inname
     *            the name as an String
     * @param invalue
     *            the value as an String
     * @return false if an error was occured, else return true
     */
    public final boolean addLinkElement(String inname, String invalue) {
        type.add(new Integer(TYPE_ATTRIBUTE));
        name.add(inname);
        format.add(new Integer(FORMAT_LINK));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a String value or attribute to this list.
     * 
     * @param intype
     *            TYPE_VALUE or TYPE_ATTRIBUTE
     * @param inname
     *            a name as a String
     * @param invalue
     *            the value as an String
     * @return false if an error was occured, else return true
     */
    public final boolean addStringElement(int intype, String inname,
            String invalue) {
        if ((intype != TYPE_VALUE) && (intype != TYPE_ATTRIBUTE)) {
            return false;
        }
        if (intype == TYPE_VALUE) {
            inname = "";
        } else {
            if (inname == null) {
                return false;
            }
        }
        if (invalue == null) {
            return false;
        }
        type.add(new Integer(intype));
        name.add(inname);
        format.add(new Integer(FORMAT_STRING));
        value.add(invalue);
        return true;
    }

    /**
     * This method add a XML String to this list.
     * 
     * @param intype
     *            TYPE_VALUE
     * @param inname
     *            a name as a String
     * @param invalue
     *            the value as an String
     * @return false if an error was occured, else return true
     */
    public final boolean addXMLElement(int intype, String inname, String invalue) {
        if (intype != TYPE_VALUE) {
            return false;
        }
        if (intype == TYPE_VALUE) {
            inname = "";
        } else {
            if (inname == null) {
                return false;
            }
        }
        if (invalue == null) {
            return false;
        }
        type.add(new Integer(intype));
        name.add(inname);
        format.add(new Integer(FORMAT_XML));
        value.add(invalue);
        return true;
    }

    /**
     * The method return true if the type is a MASTERTAG.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a MASTERTAG, else false
     */
    public final boolean isMasterTag(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_MASTERTAG) {
            return true;
        }
        return false;
    }

    /**
     * The method return true if the type is a TAG.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a TAG, else false
     */
    public final boolean isTag(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_TAG) {
            return true;
        }
        return false;
    }

    /**
     * The method return true if the type is a SUBTAG.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a SUBTAG, else false
     */
    public final boolean isSubTag(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_SUBTAG) {
            return true;
        }
        return false;
    }

    /**
     * The method return true if the type is a SUB2TAG.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a SUB2TAG, else false
     */
    public final boolean isSub2Tag(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_SUB2TAG) {
            return true;
        }
        return false;
    }

    /**
     * The method return true if the type is a VALUE.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a VALUE, else false
     */
    public final boolean isValue(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_VALUE) {
            return true;
        }
        return false;
    }

    /**
     * The method return true if the type is a ATTRIBUTE.
     * 
     * @param index
     *            the index number of the element
     * @return true if the type is a ATTRIBUTE, else false
     */
    public final boolean isAttribute(int index) {
        if ((index < 0) || (index > type.size())) {
            return false;
        }
        if (((Integer) type.get(index)).intValue() == TYPE_ATTRIBUTE) {
            return true;
        }
        return false;
    }

    /**
     * The method return the type number for the element.
     * 
     * @param index
     *            the type number of the element
     * @return the type number of the element
     */
    public final int getTypeElement(int index) {
        if ((index < 0) || (index > type.size())) {
            return TYPE_UNDEFINED;
        }
        return ((Integer) type.get(index)).intValue();
    }

    /**
     * The method return the name of the element with a index.
     * 
     * @param index
     *            the index of the element
     * @return the name of the element
     */
    public final String getNameElement(int index) {
        if ((index < 0) || (index > type.size())) {
            return "";
        }
        return (String) name.get(index);
    }

    /**
     * The method return the format number of the element with a index.
     * 
     * @param index
     *            the index of the element
     * @return the format number of the element
     */
    public final int getFormatElement(int index) {
        if ((index < 0) || (index > type.size())) {
            return FORMAT_UNDEFINED;
        }
        return ((Integer) format.get(index)).intValue();
    }

    /**
     * The method return the value of the element with a index.
     * 
     * @param index
     *            the index of the element
     * @return the value of the element
     */
    public final Object getValueElement(int index) {
        if ((index < 0) || (index > type.size())) {
            return null;
        }
        return value.get(index);
    }

    /**
     * The method return the size of the MCRTypedContent array.
     * 
     * @return the size of the array
     */
    public final int getSize() {
        return type.size();
    }

    /**
     * The method return the a new MCRTypedContent array as data of this
     * MCRTypedContent in the index range of (from;to).
     * 
     * @param from
     *            the start index
     * @param to
     *            the stop index
     * @return a MCRTypedContent object with data in this range
     */
    public final MCRTypedContent getElements(int from, int to) {
        if (from >= to) {
            return null;
        }
        if ((from < 0) || (from > type.size())) {
            return null;
        }
        if ((to < 0) || (to > type.size())) {
            return null;
        }
        MCRTypedContent out = new MCRTypedContent();
        for (int i = from; i < to + 1; i++) {
            out.addElement(getTypeElement(i), getNameElement(i),
                    getFormatElement(i), getValueElement(i));
        }
        return out;
    }

    /**
     * The method print the array.
     */
    public final void debug() {
        System.out.println("\nMCRTypedContent table");
        for (int i = 0; i < type.size(); i++) {
            System.out.println(i + "  " + type.get(i) + "   " + name.get(i)
                    + "   " + format.get(i) + "   " + value.get(i));
        }
        System.out.println();
    }

}