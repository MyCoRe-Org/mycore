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
import java.util.GregorianCalendar;
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

    private ArrayList users = null;

    private ArrayList groups = null;

    private ArrayList ips = null;

    private ArrayList flags = null;

    /**
     * This is the constructor of the MCRObjectService class. All data are set
     * to null.
     */
    public MCRObjectService() {
        lang = MCRConfiguration.instance().getString("MCR.metadata_default_lang", "en");
        dates = new ArrayList();

        MCRMetaDate d = new MCRMetaDate("service", "servdate", lang, "createdate", 0, new GregorianCalendar());
        dates.add(d);
        d = new MCRMetaDate("service", "servdate", lang, "modifydate", 0, new GregorianCalendar());
        dates.add(d);
        users = new ArrayList();
        groups = new ArrayList();
        ips = new ArrayList();
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

        if (dates_element != null) {
            List date_element_list = dates_element.getChildren();
            int date_len = date_element_list.size();

            for (int i = 0; i < date_len; i++) {
                org.jdom.Element date_element = (org.jdom.Element) date_element_list.get(i);
                String date_element_name = date_element.getName();

                if (!date_element_name.equals("servdate")) {
                    continue;
                }

                MCRMetaDate date = new MCRMetaDate();
                date.setDataPart("service");
                date.setLang(lang);
                date.setFromDOM(date_element);

                int k = -1;

                for (int j = 0; j < dates.size(); j++) {
                    if (((MCRMetaDate) dates.get(j)).getType().equals(date.getType())) {
                        k = j;

                        break;
                    }
                }

                if (k == -1) {
                    dates.add(date);
                } else {
                    dates.set(k, date);
                }
            }
        }

        // User part
        org.jdom.Element users_element = service_element.getChild("servusers");
        if (users_element != null) {
            List user_element_list = users_element.getChildren();
            int user_len = user_element_list.size();
            for (int i = 0; i < user_len; i++) {
                org.jdom.Element user_element = (org.jdom.Element) user_element_list.get(i);
                String user_element_name = user_element.getName();
                if (!user_element_name.equals("servuser")) {
                    continue;
                }
                MCRMetaAccessRule user = new MCRMetaAccessRule();
                user.setLang(lang);
                user.setDataPart("service");
                user.setFromDOM(user_element);
                users.add(user);
            }
        }

        // Group part
        org.jdom.Element groups_element = service_element.getChild("servgroups");
        if (groups_element != null) {
            List group_element_list = groups_element.getChildren();
            int group_len = group_element_list.size();
            for (int i = 0; i < group_len; i++) {
                org.jdom.Element group_element = (org.jdom.Element) group_element_list.get(i);
                String group_element_name = group_element.getName();
                if (!group_element_name.equals("servgroup")) {
                    continue;
                }
                MCRMetaAccessRule group = new MCRMetaAccessRule();
                group.setLang(lang);
                group.setDataPart("service");
                group.setFromDOM(group_element);
                groups.add(group);
            }
        }

        // IP part
        org.jdom.Element ips_element = service_element.getChild("servips");
        if (ips_element != null) {
            List ip_element_list = ips_element.getChildren();
            int ip_len = ip_element_list.size();
            for (int i = 0; i < ip_len; i++) {
                org.jdom.Element ip_element = (org.jdom.Element) ip_element_list.get(i);
                String ip_element_name = ip_element.getName();
                if (!ip_element_name.equals("servip")) {
                    continue;
                }
                MCRMetaAccessRule ip = new MCRMetaAccessRule();
                ip.setLang(lang);
                ip.setDataPart("service");
                ip.setFromDOM(ip_element);
                ips.add(ip);
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
    public final GregorianCalendar getDate(String type) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return null;
        }

        int i = -1;

        for (int j = 0; j < dates.size(); j++) {
            if (((MCRMetaDate) dates.get(j)).getType().equals(type)) {
                i = j;
            }
        }

        if (i == -1) {
            return null;
        }

        MCRMetaDate d = (MCRMetaDate) dates.get(i);

        return d.getDate();
    }

    /**
     * This method remove a date for a given type.
     * 
     * @param type
     *            the type of the date
     */
    public final void removeDate(String type) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return;
        }

        int i = -1;

        for (int j = 0; j < dates.size(); j++) {
            if (((MCRMetaDate) dates.get(j)).getType().equals(type)) {
                i = j;
            }
        }

        if (i == -1) {
            return;
        }

        dates.remove(i);
    }

    /**
     * This methode set a date element in the dates list to a actual date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     */
    public final void setDate(String type) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return;
        }

        int i = -1;

        for (int j = 0; j < dates.size(); j++) {
            if (((MCRMetaDate) dates.get(j)).getType().equals(type)) {
                i = j;
            }
        }

        if (i == -1) {
            MCRMetaDate d = new MCRMetaDate("service", "servdate", null, type, 0, new GregorianCalendar());
            dates.add(d);
        } else {
            MCRMetaDate d = (MCRMetaDate) dates.get(i);
            d.setDate(new GregorianCalendar());
            dates.set(i, d);
        }
    }

    /**
     * This methode set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     * @param date
     *            the given date
     */
    public final void setDate(String type, GregorianCalendar date) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return;
        }

        if (date == null) {
            return;
        }

        int i = -1;

        for (int j = 0; j < dates.size(); j++) {
            if (((MCRMetaDate) dates.get(j)).getType().equals(type)) {
                i = j;
            }
        }

        if (i == -1) {
            MCRMetaDate d = new MCRMetaDate("service", "servdate", null, type, 0, date);
            dates.add(d);
        } else {
            MCRMetaDate d = (MCRMetaDate) dates.get(i);
            d.setDate(date);
            dates.set(i, d);
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
     * This methode add a user to the users list.
     * 
     * @param value -
     *            the new user as string
     * @param pool -
     *            the new pool as string
     */
    public final void addUser(String value, String pool) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return;
        }
        if (getUserIndex(value, pool) == -1) {
            MCRMetaAccessRule user = new MCRMetaAccessRule("service", "servuser", null, null, 0, pool, value);
            users.add(user);
        }
    }

    /**
     * This method return the size of the user list.
     * 
     * @return the size of the user list
     */
    public final int getUserSize() {
        return users.size();
    }

    /**
     * This methode get a single user from the user list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a user string
     */
    public final String getUser(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > users.size())) {
            throw new IndexOutOfBoundsException("Index error in getUser.");
        }
        return ((MCRMetaAccessRule) users.get(index)).getText();
    }

    /**
     * This methode get a single type of user from the user list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a user pool string
     */
    public final String getUserPool(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > users.size())) {
            throw new IndexOutOfBoundsException("Index error in getUserPool.");
        }
        return ((MCRMetaAccessRule) users.get(index)).getPool();
    }

    /**
     * This methode remove a user from the user list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeUser(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > users.size())) {
            throw new IndexOutOfBoundsException("Index error in removeUser.");
        }
        users.remove(index);
    }

    /**
     * This methode add a group to the groups list.
     * 
     * @param value -
     *            the new group as string
     * @param pool -
     *            the new pool as string
     */
    public final void addGroup(String value, String pool) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return;
        }
        if (getGroupIndex(value, pool) == -1) {
            MCRMetaAccessRule group = new MCRMetaAccessRule("service", "servgroup", null, null, 0, pool, value);
            groups.add(group);
        }
    }

    /**
     * This method return the size of the group list.
     * 
     * @return the size of the group list
     */
    public final int getGroupSize() {
        return groups.size();
    }

    /**
     * This methode get a single group from the group list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a group string
     */
    public final String getGroup(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > groups.size())) {
            throw new IndexOutOfBoundsException("Index error in getGroup.");
        }
        return ((MCRMetaAccessRule) groups.get(index)).getText();
    }

    /**
     * This methode get a single type of group from the group list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a group pool string
     */
    public final String getGroupPool(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > groups.size())) {
            throw new IndexOutOfBoundsException("Index error in getGroupPool.");
        }
        return ((MCRMetaAccessRule) groups.get(index)).getPool();
    }

    /**
     * This methode remove a group from the group list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeGroup(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > groups.size())) {
            throw new IndexOutOfBoundsException("Index error in removeGroup.");
        }
        groups.remove(index);
    }

    /**
     * This methode add a ip to the ips list.
     * 
     * @param value -
     *            the new ip as string
     * @param pool -
     *            the new pool of ip ip/netmask or domain name
     * @param type -
     *            the new type of ip, it can be 'ip' or 'domain'
     */
    public final void addIP(String value, String pool, String type) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return;
        }
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return;
        }
        if ((!pool.equals("ip")) && (!pool.equals("domain"))) {
            return;
        }
        if (getIPIndex(value, pool, type) == -1) {
            MCRMetaAccessRule ip = new MCRMetaAccessRule("service", "servip", null, type, 0, pool, value);
            ips.add(ip);
        }
    }

    /**
     * This method return the size of the ip list.
     * 
     * @return the size of the ip list
     */
    public final int getIPSize() {
        return ips.size();
    }

    /**
     * This methode get a single ip from the ip list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a ip string
     */
    public final String getIP(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > ips.size())) {
            throw new IndexOutOfBoundsException("Index error in getIP.");
        }
        return ((MCRMetaAccessRule) ips.get(index)).getText();
    }

    /**
     * This methode get a single type of ip from the ip list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a ip type string
     */
    public final String getIPType(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > ips.size())) {
            throw new IndexOutOfBoundsException("Index error in getIPType.");
        }
        return ((MCRMetaAccessRule) ips.get(index)).getType();
    }

    /**
     * This methode get a single pool of ip from the ip list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a ip pool string
     */
    public final String getIPPool(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > ips.size())) {
            throw new IndexOutOfBoundsException("Index error in getIPPool.");
        }
        return ((MCRMetaAccessRule) ips.get(index)).getPool();
    }

    /**
     * This methode remove an ip from the ip list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeIP(int index) throws IndexOutOfBoundsException {
        if ((index < 0) || (index > ips.size())) {
            throw new IndexOutOfBoundsException("Index error in removeIP.");
        }
        ips.remove(index);
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
            elmm.setAttribute("class", "MCRMetaDate");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < dates.size(); i++) {
                elmm.addContent(((MCRMetaDate) dates.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        if (users.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servusers");
            elmm.setAttribute("class", "MCRMetaAccessRule");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < users.size(); i++) {
                elmm.addContent(((MCRMetaAccessRule) users.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        if (groups.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servgroups");
            elmm.setAttribute("class", "MCRMetaAccessRule");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < groups.size(); i++) {
                elmm.addContent(((MCRMetaAccessRule) groups.get(i)).createXML());
            }

            elm.addContent(elmm);
        }

        if (ips.size() != 0) {
            org.jdom.Element elmm = new org.jdom.Element("servips");
            elmm.setAttribute("class", "MCRMetaAccessRule");
            elmm.setAttribute("heritable", "false");
            elmm.setAttribute("notinherit", "false");
            elmm.setAttribute("parasearch", "true");
            elmm.setAttribute("textsearch", "false");

            for (int i = 0; i < ips.size(); i++) {
                elmm.addContent(((MCRMetaAccessRule) ips.get(i)).createXML());
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
        if (getDate("createdate") == null) {
            return false;
        }

        if (getDate("modifydate") == null) {
            return false;
        }

        return true;
    }

    /**
     * This methode returns the index for the given user value.
     * 
     * @param value
     *            the value of a user as string
     * @param pool
     *            the pool of an ip as string
     * 
     */
    public final int getUserIndex(String value, String pool) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return -1;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return -1;
        }
        for (int i = 0; i < users.size(); i++) {
            if (((MCRMetaAccessRule) users.get(i)).getText().equals(value) && ((MCRMetaAccessRule) users.get(i)).getPool().equals(pool)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This methode returns the index for the given group value.
     * 
     * @param value
     *            the value of a group as string
     * @param pool
     *            the pool of an ip as string
     * 
     */
    public final int getGroupIndex(String value, String pool) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return -1;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return -1;
        }
        for (int i = 0; i < groups.size(); i++) {
            if (((MCRMetaAccessRule) groups.get(i)).getText().equals(value) && ((MCRMetaAccessRule) groups.get(i)).getPool().equals(pool)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This methode returns the index for the given ip value.
     * 
     * @param value
     *            the value of an ip as string
     * @param type
     *            the type of an ip as string
     * @param pool
     *            the pool of an ip as string
     * 
     */
    public final int getIPIndex(String value, String pool, String type) {
        if ((value == null) || ((value = value.trim()).length() == 0)) {
            return -1;
        }
        if ((pool == null) || ((pool = pool.trim()).length() == 0)) {
            return -1;
        }
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return -1;
        }
        for (int i = 0; i < ips.size(); i++) {
            if (((MCRMetaAccessRule) ips.get(i)).getText().equals(value) && ((MCRMetaAccessRule) ips.get(i)).getPool().equals(pool) && ((MCRMetaAccessRule) ips.get(i)).getType().equals(type)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This methode returns the index for the given flag value.
     * 
     * @param value
     *            the value of a flag as string
     * @param type
     *            the pool if an ip as string
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
