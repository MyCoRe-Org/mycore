/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class implements all methods for handling MCRObject service data. 
 * The service data stores technical information that is no metadata. 
 * The service data holds three types of data, dates, flags and states. 
 * The flags are text strings and are optional.
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
 * The state is optional and represented by a MyCoRe classification object.
 * 
 * @author Jens Kupferschmidt
 * @author Matthias Eichner
 * @author Robert Stephan
 * @version $Revision$ $Date$
 */
public class MCRObjectService {
    private static Logger LOGGER = LogManager.getLogger();

    /**
     * constant for create date
     */
    public static final String DATE_TYPE_CREATEDATE = "createdate";

    /**
     * constant for modify date
     */
    public static final String DATE_TYPE_MODIFYDATE = "modifydate";

    /**
     * constant for create user
     */
    public static final String FLAG_TYPE_CREATEDBY = "createdby";

    /**
     * constant for modify user
     */
    public static final String FLAG_TYPE_MODIFIEDBY = "modifiedby";

    private final ArrayList<MCRMetaISO8601Date> dates;

    private final ArrayList<MCRMetaAccessRule> rules;

    private final ArrayList<MCRMetaLangText> flags;

    private MCRCategoryID state;

    /**
     * This is the constructor of the MCRObjectService class. All data are set
     * to null.
     */
    public MCRObjectService() {
        dates = new ArrayList<>();

        Date curTime = new Date();

        MCRMetaISO8601Date d = new MCRMetaISO8601Date("servdate", DATE_TYPE_CREATEDATE, 0);
        d.setDate(curTime);
        dates.add(d);
        d = new MCRMetaISO8601Date("servdate", DATE_TYPE_MODIFYDATE, 0);
        d.setDate(curTime);
        dates.add(d);

        rules = new ArrayList<>();
        flags = new ArrayList<>();
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * structure data of the document.
     * 
     * @param service
     *            a list of relevant DOM elements for the metadata
     */
    public final void setFromDOM(Element service) {
        // Date part
        org.jdom2.Element dates_element = service.getChild("servdates");
        dates.clear();

        if (dates_element != null) {
            List<Element> dateList = dates_element.getChildren();

            for (Element dateElement : dateList) {
                String date_element_name = dateElement.getName();

                if (!date_element_name.equals("servdate")) {
                    continue;
                }

                MCRMetaISO8601Date date = new MCRMetaISO8601Date();
                date.setFromDOM(dateElement);

                setDate(date);
            }
        }

        // Rule part
        Element servacls = service.getChild("servacls");
        if (servacls != null) {
            List<Element> ruleList = servacls.getChildren();
            for (Element ruleElement : ruleList) {
                if (!ruleElement.getName().equals("servacl")) {
                    continue;
                }
                MCRMetaAccessRule user = new MCRMetaAccessRule();
                user.setFromDOM(ruleElement);
                rules.add(user);
            }
        }

        // Flag part
        org.jdom2.Element flagsElement = service.getChild("servflags");
        if (flagsElement != null) {
            List<Element> flagList = flagsElement.getChildren();
            for (Element flagElement : flagList) {
                if (!flagElement.getName().equals("servflag")) {
                    continue;
                }
                MCRMetaLangText flag = new MCRMetaLangText();
                flag.setFromDOM(flagElement);
                flags.add(flag);
            }
        }

        org.jdom2.Element statesElement = service.getChild("servstates");
        if (statesElement != null) {
            List<Element> flagList = statesElement.getChildren();
            for (Element stateElement : flagList) {
                if (!stateElement.getName().equals("servstate")) {
                    continue;
                }
                MCRMetaClassification stateClass = new MCRMetaClassification();
                stateClass.setFromDOM(stateElement);
                state = new MCRCategoryID(stateClass.getClassId(), stateClass.getCategId());
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
     * Returns the dates.
     * 
     * @return list of dates
     */
    protected ArrayList<MCRMetaISO8601Date> getDates() {
        return dates;
    }

    /**
     * This method returns the status classification
     * 
     * @return the status as MCRMetaClassification,
     *         can return null
     * 
     */
    public final MCRCategoryID getState() {
        return state;
    }

    /**
     * This method get a date for a given type. If the type was not found, an
     * null was returned.
     * 
     * @param type
     *            the type of the date 
     * @return the date as GregorianCalendar
     * 
     * @see MCRObjectService#DATE_TYPE_CREATEDATE
     * @see MCRObjectService#DATE_TYPE_MODIFYDATE 
     */
    public final Date getDate(String type) {
        MCRMetaISO8601Date isoDate = getISO8601Date(type);
        if (isoDate == null) {
            return null;
        }
        return isoDate.getDate();
    }

    private MCRMetaISO8601Date getISO8601Date(String type) {
        if (type == null || type.length() == 0) {
            return null;
        }

        return IntStream.range(0, dates.size())
            .mapToObj(dates::get)
            .filter(d -> d.getType().equals(type))
            .findAny()
            .orElse(null);
    }

    /**
     * This method set a date element in the dates list to a actual date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     */
    public final void setDate(String type) {
        setDate(type, new Date());
    }

    /**
     * This method sets the status classification
     */
    public final void setState(MCRCategoryID state) {
        if (state == null) {
            this.state = state;
        } else {
            if (MCRCategoryDAOFactory.getInstance().exist(state)) {
                this.state = state;
            } else {
                LOGGER.error("Error at setting servstate classification.",
                    new MCRException("The category " + state + " does not exist."));
            }
        }
    }

    /**
     * This method set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     * 
     * @param type
     *            the type of the date
     * @param date
     *            set time to this Calendar
     */
    public final void setDate(String type, Date date) {
        MCRMetaISO8601Date d = getISO8601Date(type); //search date in ArrayList
        if (d == null) {
            d = new MCRMetaISO8601Date("servdate", type, 0);
            d.setDate(date);
            dates.add(d);
        } else {
            d.setDate(date); // alter date found in ArrayList
        }
    }

    /**
     * This method set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     * 
     * @param date
     *            set time to this Calendar
     */
    private void setDate(MCRMetaISO8601Date date) {
        MCRMetaISO8601Date d = getISO8601Date(date.getType()); //search date in ArrayList

        if (d == null) {
            dates.add(date);
        } else {
            d.setDate(date.getDate()); // alter date found in ArrayList
        }
    }

    /**
     * This method add a flag to the flag list.
     * 
     * @param value -
     *            the new flag as string
     */
    public final void addFlag(String value) {
        if (value == null || (value = value.trim()).length() == 0) {
            return;
        }

        MCRMetaLangText flag = new MCRMetaLangText("servflag", null, null, 0, null, value);
        flags.add(flag);
    }

    /**
     * This method adds a flag to the flag list.
     * 
     * @param type
     *              a type as string
     * @param value
     *              the new flag value as string
     */
    public final void addFlag(String type, String value) {
        if (value == null || (value = value.trim()).length() == 0) {
            return;
        }
        if (type == null || (type = type.trim()).length() == 0) {
            type = null;
        }

        MCRMetaLangText flag = new MCRMetaLangText("servflag", null, type, 0, null, value);
        flags.add(flag);
    }

    /**
     * This method get all flags from the flag list as a string.
     * 
     * @return the flags string
     */
    public final String getFlags() {
        StringBuilder sb = new StringBuilder("");

        for (MCRMetaLangText flag : flags) {
            sb.append(flag.getText()).append(" ");
        }

        return sb.toString();
    }

    /**
     * Returns the flags as list.
     * 
     * @return flags as list
     */
    protected final List<MCRMetaLangText> getFlagsAsList() {
        return flags;
    }

    /**
     * This method returns all flag values of the specified type.
     * 
     * @param type
     *              a type as string.
     * @return a list of flag values
     */
    protected final ArrayList<MCRMetaLangText> getFlagsAsMCRMetaLangText(String type) {
        return flags.stream()
            .filter(metaLangText -> type.equals(metaLangText.getType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all flag values of the specified type.
     * 
     * @param type
     *              a type as string.
     * @return a list of flag values
     */
    public final ArrayList<String> getFlags(String type) {
        return getFlagsAsMCRMetaLangText(type).stream()
            .map(MCRMetaLangText::getText)
            .collect(Collectors.toCollection(ArrayList::new));
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
     * This method get a single flag from the flag list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a flag string
     */
    public final String getFlag(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > flags.size()) {
            throw new IndexOutOfBoundsException("Index error in getFlag.");
        }
        return flags.get(index).getText();
    }

    /**
     * This method gets a single flag type from the flag list as a string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a flag type
     */
    public final String getFlagType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > flags.size()) {
            throw new IndexOutOfBoundsException("Index error in getFlagType.");
        }
        return flags.get(index).getType();
    }

    /**
     * This method return a boolean value if the given flag is set or not.
     * 
     * @param value
     *            a searched flag
     * @return true if the flag was found in the list
     */
    public final boolean isFlagSet(String value) {
        if (value == null || (value = value.trim()).length() == 0) {
            return false;
        }

        for (MCRMetaLangText flag : flags) {
            if (flag.getText().equals(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Proves if the type is set in the flag list.
     * @param type
     *              a type as string
     * @return  true if the flag list contains flags with this type,
     *          otherwise false
     */
    public final boolean isFlagTypeSet(String type) {
        ArrayList<MCRMetaLangText> internalList = getFlagsAsMCRMetaLangText(type);
        return internalList.size() > 0;
    }

    /**
     * This method remove a flag from the flag list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeFlag(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > flags.size()) {
            throw new IndexOutOfBoundsException("Index error in removeFlag.");
        }

        flags.remove(index);
    }

    /**
     * This method removes all flags of the specified type from
     * the flag list.
     * 
     * @param type
     *            a type as string
     */
    public final void removeFlags(String type) {
        ArrayList<MCRMetaLangText> internalList = getFlagsAsMCRMetaLangText(type);
        flags.removeAll(internalList);
    }

    /**
     * This method set a flag in the flag list.
     * 
     * @param index
     *            a index in the list
     * @param value
     *            the value of a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void replaceFlag(int index, String value) throws IndexOutOfBoundsException {
        if (index < 0 || index > flags.size()) {
            throw new IndexOutOfBoundsException("Index error in replaceFlag.");
        }
        if (value == null || (value = value.trim()).length() == 0) {
            return;
        }
        MCRMetaLangText oldFlag = flags.get(index);
        MCRMetaLangText flag = new MCRMetaLangText("servflag", null, oldFlag.getType(), 0, null, value);
        flags.set(index, flag);
    }

    /**
     * This method sets the type value of a flag at the specified index.
     * 
     * @param index
     *            a index in the list
     * @param value
     *            the value of a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void replaceFlagType(int index, String value) throws IndexOutOfBoundsException {
        if (index < 0 || index > flags.size()) {
            throw new IndexOutOfBoundsException("Index error in replaceFlag.");
        }
        if (value == null || (value = value.trim()).length() == 0) {
            return;
        }
        MCRMetaLangText oldFlag = flags.get(index);
        MCRMetaLangText flag = new MCRMetaLangText("servflag", null, value, 0, null, oldFlag.getText());
        flags.set(index, flag);
    }

    /**
     * This method add a rule to the rules list.
     * 
     * @param permission -
     *            the new permission as string
     * @param condition -
     *            the new rule as JDOM tree Element
     */
    public final void addRule(String permission, org.jdom2.Element condition) {
        if (condition == null) {
            return;
        }
        if (permission == null || (permission = permission.trim()).length() == 0) {
            return;
        }
        if (getRuleIndex(permission) == -1) {
            MCRMetaAccessRule acl = new MCRMetaAccessRule("servacl", null, 0, permission, condition);
            rules.add(acl);
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
        int notFound = -1;
        if (permission == null || permission.trim().length() == 0) {
            return notFound;
        }
        return IntStream.range(0, rules.size())
            .filter(i -> rules.get(i).getPermission().equals(permission))
            .findAny()
            .orElse(notFound);
    }

    /**
     * This method get a single rule from the rules list as a JDOM Element.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a the MCRMetaAccessRule instance
     */
    public final MCRMetaAccessRule getRule(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > rules.size()) {
            throw new IndexOutOfBoundsException("Index error in getRule.");
        }
        return rules.get(index);
    }

    /**
     * This method get a single permission name of rule from the rules list as a
     * string.
     * 
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     * @return a rule permission string
     */
    public final String getRulePermission(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > rules.size()) {
            throw new IndexOutOfBoundsException("Index error in getRulePermission.");
        }
        return rules.get(index).getPermission();
    }

    /**
     * This method remove a rule from the rules list.
     * 
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is false
     */
    public final void removeRule(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index > rules.size()) {
            throw new IndexOutOfBoundsException("Index error in removeRule.");
        }
        rules.remove(index);
    }

    /**
     * Returns the rules.
     * 
     * @return list of rules
     */
    protected final ArrayList<MCRMetaAccessRule> getRules() {
        return rules;
    }

    /**
     * This method create a XML stream for all structure data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final org.jdom2.Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }
        org.jdom2.Element elm = new org.jdom2.Element("service");

        if (dates.size() != 0) {
            org.jdom2.Element elmm = new org.jdom2.Element("servdates");
            elmm.setAttribute("class", "MCRMetaISO8601Date");

            for (MCRMetaISO8601Date date : dates) {
                elmm.addContent(date.createXML());
            }

            elm.addContent(elmm);
        }

        if (rules.size() != 0) {
            org.jdom2.Element elmm = new org.jdom2.Element("servacls");
            elmm.setAttribute("class", "MCRMetaAccessRule");

            for (MCRMetaAccessRule rule : rules) {
                elmm.addContent(rule.createXML());
            }

            elm.addContent(elmm);
        }

        if (flags.size() != 0) {
            org.jdom2.Element elmm = new org.jdom2.Element("servflags");
            elmm.setAttribute("class", "MCRMetaLangText");

            for (MCRMetaLangText flag : flags) {
                elmm.addContent(flag.createXML());
            }

            elm.addContent(elmm);
        }
        if (state != null) {
            org.jdom2.Element elmm = new org.jdom2.Element("servstates");
            elmm.setAttribute("class", "MCRMetaClassification");
            MCRMetaClassification stateClass = new MCRMetaClassification("servstate", 0, null, state);
            elmm.addContent(stateClass.createXML());
            elm.addContent(elmm);
        }

        return elm;
    }

    /**
     * Creates the JSON representation of this service.
     * 
     * <pre>
     *   {
     *      dates: [
     *          {@link MCRMetaISO8601Date#createJSON()},
     *          ...
     *      ],
     *      rules: [
     *          {@link MCRMetaAccessRule#createJSON()},
     *          ...
     *      ],
     *      flags: [
     *          {@link MCRMetaLangText#createJSON()},
     *          ...
     *      ],
     *      state: {
     *          
     *      }
     *   }
     * </pre>
     * 
     * @return a json gson representation of this service
     */
    public final JsonObject createJSON() {
        JsonObject service = new JsonObject();
        // dates
        if (!getDates().isEmpty()) {
            JsonObject dates = new JsonObject();
            getDates()
                .stream()
                .forEachOrdered(date -> {
                    JsonObject jsonDate = date.createJSON();
                    jsonDate.remove("type");
                    dates.add(date.getType(), jsonDate);
                });
            service.add("dates", dates);
        }
        // rules
        if (!getRules().isEmpty()) {
            JsonArray rules = new JsonArray();
            getRules()
                .stream()
                .map(MCRMetaAccessRule::createJSON)
                .forEachOrdered(rules::add);
            service.add("rules", rules);
        }
        // flags
        if (!getFlags().isEmpty()) {
            JsonArray flags = new JsonArray();
            getFlagsAsList()
                .stream()
                .map(MCRMetaLangText::createJSON)
                .forEachOrdered(flags::add);
            service.add("flags", flags);
        }
        // state
        Optional.ofNullable(getState()).ifPresent(stateId -> {
            JsonObject state = new JsonObject();
            if (stateId.getID() != null) {
                state.addProperty("id", stateId.getID());
            }
            state.addProperty("rootId", stateId.getRootID());
        });
        return service;
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
        try {
            validate();
            return true;
        } catch (MCRException exc) {
            LOGGER.warn("The <service> part is invalid.");
        }
        return false;
    }

    /**
     * Validates the content of this class. This method throws an exception if:
     *  <ul>
     *  <li>the date value of "createdate" is not null or empty</li>
     *  <li>the date value of "modifydate" is not null or empty</li>
     *  </ul>
     * 
     * @throws MCRException the content is invalid
     */
    public void validate() {
        // TODO: this makes no sense - there is nothing to validate
        if (getISO8601Date(DATE_TYPE_CREATEDATE) == null) {
            setDate(DATE_TYPE_CREATEDATE);
        }
        if (getISO8601Date(DATE_TYPE_MODIFYDATE) == null) {
            setDate(DATE_TYPE_MODIFYDATE);
        }
    }

    /**
     * This method returns the index for the given flag value.
     * 
     * @param value
     *            the value of a flag as string
     * @return the index number or -1 if the value was not found
     */
    public final int getFlagIndex(String value) {
        if (value == null || (value = value.trim()).length() == 0) {
            return -1;
        }
        for (int i = 0; i < flags.size(); i++) {
            if (flags.get(i).getText().equals(value)) {
                return i;
            }
        }
        return -1;
    }

}
