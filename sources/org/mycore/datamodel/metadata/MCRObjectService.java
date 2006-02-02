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
import java.util.Date;
import java.util.List;

import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;

/**
 * This class implements all methode for handling one document service data. The
 * service data are to use to handel the database with batch jobs automatical
 * changes. The service class holds two types of data, dates and flags. The
 * flags are text strings and are optional.
 * <p>
 * 
 * The dates are represent by a date and a type. Two types are in service data
 * at every time and can't remove:
 * <ul>
 * <li>createdate - for the creating date of the object, this was set only one
 * time</li>
 * <li>modifydate - for the accepting date of the object, this was set at every
 * changes</li>
 * </ul>
 * Other date types are optional, but as example in Dublin Core:
 * <ul>
 * <li>submitdate - for the submiting date of the object</li>
 * <li>acceptdate - for the accepting date of the object</li>
 * <li>validfromdate - for the date of the object, at this the object is valid
 * to use</li>
 * <li>validtodate - for the date of the object, at this the object is no more
 * valid to use</li>
 * </ul>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRObjectService {
    // service data
    private String lang = null;

    private ArrayList dates = null;

    private ArrayList rules = null;

    private ArrayList flags = null;

    /**
     * This is the constructor of the MCRObjectService class. All data are set
     * to null.
     */
    public MCRObjectService() {
        lang = MCRConfiguration.instance().getString("MCR.metadata_default_lang", "en");
        dates = new ArrayList();
        
        Date curTime=new Date();
        
        MCRMetaISO8601Date d = new MCRMetaISO8601Date("service", "servdate","createdate",0);
        d.setDate(curTime);
        dates.add(d);
        d = new MCRMetaISO8601Date("service", "servdate","modifydate",0);
        d.setDate(curTime);
        dates.add(d);

        rules = new ArrayList();
        flags = new ArrayList();
    }

    /**
     * This methode read the XML input stream part from a DOM part for the
     * structure data of the document.
     * 
     * @param dom_element_list
     *            a list of relevant DOM elements for the metadata
     */
    public final void setFromDOM(org.jdom.Element service_element) {
        // Date part
        org.jdom.Element dates_element = service_element.getChild("servdates");
        dates.clear();

        if (dates_element != null) {
            List date_element_list = dates_element.getChildren();
            int date_len = date_element_list.size();

            for (int i = 0; i < date_len; i++) {
                org.jdom.Element date_element = (org.jdom.Element) date_element_list.get(i);
                String date_element_name = date_element.getName();

                if (!date_element_name.equals("servdate")) {
                    continue;
                }

                MCRMetaISO8601Date date = new MCRMetaISO8601Date();
                date.setDataPart(service_element.getName());
                date.setFromDOM(date_element);
                
                setDate(date);
            }
        }

        // Rule part
        org.jdom.Element rules_element = service_element.getChild("servacls");
        if (rules_element != null) {
            List rule_element_list = rules_element.getChildren();
            int rule_len = rule_element_list.size();
            for (int i = 0; i < rule_len; i++) {
                org.jdom.Element rule_element = (org.jdom.Element) rule_element_list.get(i);
                String rule_element_name = rule_element.getName();
                if (!rule_element_name.equals("servacl")) {
                    continue;
                }
                MCRMetaAccessRule user = new MCRMetaAccessRule();
                user.setLang(lang);
                user.setDataPart("service");
                user.setFromDOM(rule_element);
                rules.add(user);
            }
        }

        // Flag part
        org.jdom.Element flags_element = service_element.getChild("servflags");
        if (flags_element != null) {
            List flag_element_list = flags_element.getChildren();
            int flag_len = flag_element_list.size();
            for (int i = 0; i < flag_len; i++) {
                org.jdom.Element flag_element = (org.jdom.Element) flag_element_list.get(i);
                String flag_element_name = flag_element.getName();

                if (!flag_element_name.equals("servflag")) {
                    continue;
                }
                MCRMetaLangText flag = new MCRMetaLangText();
                flag.setLang(lang);
                flag.setDataPart("service");
                flag.setFromDOM(flag_element);
                flags.add(flag);
            }
        }
    }

    /**
     * This method return the size of the date list.
     * 
     * @return the size of the date list
     */
    public final int getDateSize() {
        return dates.size();
    }

    /**
     * This method get a date for a given type. If the type was not found, an
     * null was returned.
     * 
     * @param type
     *            the type of the date
     * @return the date as GregorianCalendar
     */
    public final Date getDate(String type) {
    	MCRMetaISO8601Date isoDate = getISO8601Date(type);
    	if(isoDate != null)
    		return isoDate.getDate();
    	else
    		return null;
    }
    
    private final MCRMetaISO8601Date getISO8601Date(String type){
        if ((type == null) || (type.length() == 0)) {
            return null;
        }

        int i = -1;

        for (int j = 0; j < dates.size(); j++) {
            if (((MCRMetaISO8601Date) dates.get(j)).getType().equals(type)) {
                i = j;
                break;
            }
        }

        if (i == -1) {
            return null;
        }

        return (MCRMetaISO8601Date) dates.get(i);
    }

    /**
     * This methode set a date element in the dates list to a actual date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     */
    public final void setDate(String type) {
        setDate(type,new Date());
    }

    /**
     * This methode set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     * @param date
     *            set time to this Calendar
     */
    public final void setDate(String type, Date date) {
        MCRMetaISO8601Date d=getISO8601Date(type); //search date in ArrayList
        if (d == null) {
            d = new MCRMetaISO8601Date("service", "servdate", type, 0);
            d.setDate(date);
            dates.add(d);
        } else {
            d.setDate(date); // alter date found in ArrayList
        }
    }

    /**
     * This methode set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     * @param date
     *            set time to this Calendar
     */
    private final void setDate(MCRMetaISO8601Date date) {
        MCRMetaISO8601Date d=getISO8601Date(date.getType()); //search date in ArrayList

        if (d == null) {
            dates.add(date);
        } else {
            d.setDate(date.getDate()); // alter date found in ArrayList
        }
    }

    /**
     * This methode add a flag to the flag list.
     * 
     * @param value -
     *            the new flag as string
     */
    public final void addFlag(String value) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return;
        }

        MCRMetaLangText flag = new MCRMetaLangText("service", "servflag", null, null, 0, null, value);
        flags.add(flag);
    }

    /**
     * This methode get all flags from the flag list as a string.
     * 
     * @return the flags string
     */
    public final String getFlags() {
        StringBuffer sb = new StringBuffer("");

        for (int i = 0; i < flags.size(); i++) {
            sb.append(((MCRMetaLangText) flags.get(i)).getText()).append(" ");
        }

        return sb.toString();
    }

    /**
     * This method return the size of the flag list.
     * 
     * @return the size of the flag list
     */
    public final int getFlagSize() {
        return flags.size();
    }

    /**
     * This methode get a single flag from the flag list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a flag string
     */
    public final String getFlag(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > flags.size())) {
            throw new IndexOutOfBoundsException("Index error in getFlag.");
        }

        return ((MCRMetaLangText) flags.get(index)).getText();
    }

    /**
     * This methode return a boolean value if the given flag is set or not.
     * 
     * @param value
     *            a searched flag
     * @return true if the flag was found in the list
     */
    public final boolean isFlagSet(String value) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return false;
        }

        for (int i = 0; i < flags.size(); i++) {
            if (((MCRMetaLangText) flags.get(i)).getText().equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This methode remove a flag from the flag list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeFlag(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > flags.size())) {
            throw new IndexOutOfBoundsException("Index error in removeFlag.");
        }

        flags.remove(index);
    }

    /**
     * This methode set a flag in the flag list.
     * 
     * @param index
     *            a index in the list
     * @param value
     *            the value of a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void replaceFlag(int index, String value) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > flags.size())) {
            throw new IndexOutOfBoundsException("Index error in replaceFlag.");
        }
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return;
        }
        MCRMetaLangText flag = new MCRMetaLangText("service", "servflag", null, null, 0, null, value);
        flags.set(index, flag);
    }

    /**
     * This methode add a rule to the rules list.
     * 
     * @param permission -
     *            the new permission as string
     * @param condition -
     *            the new rule as JDOM tree Element
     */
    public final void addRule(String permission, org.jdom.Element condition) {
        if (condition == null) {
            return;
        }
        if ((permission == null) || ((permission = permission.trim()).length() == 0)) {
            return;
        }
        if (getRuleIndex(permission) == -1) {
            MCRMetaAccessRule user = new MCRMetaAccessRule("service", "servuser", null, null, 0, permission, condition);
            rules.add(user);
        }
    }

    /**
     * This method return the size of the rules list.
     * 
     * @return the size of the rules list
     */
    public final int getRulesSize() {
        return rules.size();
    }

    /**
     * This method return the index of a permission in the rules list.
     * 
     * @return the index of a permission in the rules list
     */
    public final int getRuleIndex(String permission) {
        int ret = -1;
        if ((permission == null) || (permission.trim().length() == 0))
            return ret;
        for (int i = 0; i < rules.size(); i++) {
            if (((MCRMetaAccessRule) rules.get(i)).getPermission().equals(permission)) {
                ret = i;
                break;
            }
        }
        return ret;
    }

    /**
     * This methode get a single rule from the rules list as a JDOM Element.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a the MCRMetaAccessRule instance
     */
    public final MCRMetaAccessRule getRule(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > rules.size())) {
            throw new IndexOutOfBoundsException("Index error in getRule.");
        }
        return (MCRMetaAccessRule) rules.get(index);
    }

    /**
     * This methode get a single permission name of rule from the rules list as a
     * string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a rule permission string
     */
    public final String getRulePermission(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > rules.size())) {
            throw new IndexOutOfBoundsException("Index error in getRulePermission.");
        }
        return ((MCRMetaAccessRule) rules.get(index)).getPermission();
    }

    /**
     * This methode remove a rule from the rules list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeRule(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > rules.size())) {
            throw new IndexOutOfBoundsException("Index error in removeRule.");
        }
        rules.remove(index);
    }

    /**
     * This methode create a XML stream for all structure data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final org.jdom.Element createXML() throws MCRException {
        if (!isValid()) {
            throw new MCRException("The content is not valid.");
        }

        org.jdom.Element elm = new org.jdom.Element("service");

        if (dates.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servdates");
            elmm.setAttribute("class", "MCRMetaISO8601Date");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < dates.size(); i++) {
                elmm.addContent(((MCRMetaISO8601Date) dates.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        if (rules.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servusers");
            elmm.setAttribute("class", "MCRMetaAccessRule");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < rules.size(); i++) {
                elmm.addContent(((MCRMetaAccessRule) rules.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        if (flags.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servflags");
            elmm.setAttribute("class", "MCRMetaLangText");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < flags.size(); i++) {
                elmm.addContent(((MCRMetaLangText) flags.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        return elm;
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the date value of "createdate" is not null or empty
     * <li>the date value of "modifydate" is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public final boolean isValid() {
        if (getISO8601Date("createdate") == null) {
            return false;
        }

        if (getISO8601Date("modifydate") == null) {
            return false;
        }

        return true;
    }

    /**
     * This methode returns the index for the given flag value.
     * 
     * @param value
     *            the value of a flag as string
     * @param type
     *            the permission if an ip as string
     * 
     */
    public final int getFlagIndex(String value) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return -1;
        }
        for (int i = 0; i < flags.size(); i++) {
            if (((MCRMetaLangText) flags.get(i)).getText().equals(value)) {
                return i;
            }
        }
        return -1;
    }

}
