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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRISO8601Date;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This class implements all methods for handling MCRObject service data.
 * The service data stores technical information that is no metadata.
 * The service data holds six types of data (dates, rules  flags, messages,
 * classifications and states). The flags and messages are text strings
 * and are optional.
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
 */
public class MCRObjectService {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String XML_NAME = "service";

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

    public static final String ELEMENT_SERVDATE = "servdate";

    public static final String ELEMENT_SERVDATES = "servdates";

    public static final String ELEMENT_SERVACLS = "servacls";

    public static final String ELEMENT_SERVFLAGS = "servflags";

    public static final String ELEMENT_SERVCLASSES = "servclasses";

    public static final String ELEMENT_SERVMESSAGES = "servmessages";

    public static final String ELEMENT_SERVSTATES = "servstates";

    public static final String ELEMENT_SERVACL = "servacl";

    public static final String ELEMENT_SERVFLAG = "servflag";

    public static final String ELEMENT_SERVCLASS = "servclass";

    public static final String ELEMENT_SERVMESSAGE = "servmessage";

    public static final String ELEMENT_SERVSTATE = "servstate";

    private final List<MCRMetaISO8601Date> dates = new ArrayList<>();

    private final List<MCRMetaAccessRule> rules = new ArrayList<>();

    private final List<MCRMetaLangText> flags = new ArrayList<>();

    private final List<MCRMetaDateLangText> messages = new ArrayList<>();

    private final List<MCRMetaClassification> classifications = new ArrayList<>();

    private MCRCategoryID state;

    /**
     * This is the constructor of the MCRObjectService class. All data are set
     * to null.
     */
    public MCRObjectService() {

        Date now = new Date();

        MCRMetaISO8601Date createDate = new MCRMetaISO8601Date(ELEMENT_SERVDATE, DATE_TYPE_CREATEDATE, 0);
        createDate.setDate(now);
        dates.add(createDate);

        MCRMetaISO8601Date modifyDate = new MCRMetaISO8601Date(ELEMENT_SERVDATE, DATE_TYPE_MODIFYDATE, 0);
        modifyDate.setDate(now);
        dates.add(modifyDate);

    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * structure data of the document.
     *
     * @param service
     *            a list of relevant DOM elements for the metadata
     */
    public final void setFromDOM(Element service) {

        Element datesElement = service.getChild(ELEMENT_SERVDATES);
        Element rulesElement = service.getChild(ELEMENT_SERVACLS);
        Element flagsElement = service.getChild(ELEMENT_SERVFLAGS);
        Element classificationsElement = service.getChild(ELEMENT_SERVCLASSES);
        Element messagesElement = service.getChild(ELEMENT_SERVMESSAGES);
        Element statesElement = service.getChild(ELEMENT_SERVSTATES);

        addContent(rulesElement, ELEMENT_SERVACL, MCRMetaAccessRule.class, rules);
        addContent(flagsElement, ELEMENT_SERVFLAG, MCRMetaLangText.class, flags);
        addContent(classificationsElement, ELEMENT_SERVCLASS, MCRMetaClassification.class, classifications);
        addContent(messagesElement, ELEMENT_SERVMESSAGE, MCRMetaDateLangText.class, messages);

        // date part
        dates.clear();
        if (datesElement != null) {
            List<Element> dateElements = datesElement.getChildren();
            for (Element dateElement : dateElements) {
                String dateElementName = dateElement.getName();
                if (!Objects.equals(dateElementName, ELEMENT_SERVDATE)) {
                    continue;
                }
                MCRMetaISO8601Date date = new MCRMetaISO8601Date();
                date.setFromDOM(dateElement);
                setDate(date);
            }
        }
        // States part
        if (statesElement != null) {
            List<Element> stateElements = statesElement.getChildren();
            for (Element stateElement : stateElements) {
                if (!stateElement.getName().equals(ELEMENT_SERVSTATE)) {
                    continue;
                }
                MCRMetaClassification stateClass = new MCRMetaClassification();
                stateClass.setFromDOM(stateElement);
                state = new MCRCategoryID(stateClass.getClassId(), stateClass.getCategId());
            }
        }
    }

    private <T extends MCRMetaDefault> void addContent(Element element, String name,
        Class<T> itemClass, List<T> items) {
        if (element != null) {
            List<Element> elements = element.getChildren();
            for (Element elm : elements) {
                if (!elm.getName().equals(name)) {
                    continue;
                }
                try {
                    T newItem = itemClass.getDeclaredConstructor().newInstance();
                    newItem.setFromDOM(elm);
                    items.add(newItem);
                } catch (ReflectiveOperationException e) {
                    LOGGER.error(() -> "Could not instantiate " + itemClass.getSimpleName(), e);
                }
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
    protected List<MCRMetaISO8601Date> getDates() {
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
        if (type == null || type.isEmpty()) {
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
     * This method set a date element in the dates list to a given date value.
     * If the given type exists, the date was update.
     *
     * @param type
     *            the type of the date
     * @param date
     *            set time to this Calendar
     *            null means the actual date
     */
    public final void setDate(String type, Date date) {
        MCRMetaISO8601Date d = getISO8601Date(type); //search date in ArrayList
        if (d == null) {
            d = new MCRMetaISO8601Date(ELEMENT_SERVDATE, type, 0);
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
     * This method removes the date of the specified type from
     * the date list.
     *
     * @param type
     *            a type as string
     */
    public final void removeDate(String type) {
        if (DATE_TYPE_CREATEDATE.equals(type) || DATE_TYPE_MODIFYDATE.equals(type)) {
            LOGGER.error(() -> "Cannot delete built-in date: " + type);
        } else {
            MCRMetaISO8601Date d = getISO8601Date(type);
            if (d != null) {
                dates.remove(d);
            }
        }
    }

    /**
     * This method sets the status classification
     */
    public final void setState(MCRCategoryID state) {
        if (state == null) {
            this.state = state;
        } else {
            if (MCRCategoryDAOFactory.obtainInstance().exist(state)) {
                this.state = state;
            } else {
                MCRException logEx = new MCRException("The category " + state + " does not exist.");
                LOGGER.warn("Error at setting servstate classification.", logEx);
            }
        }
    }

    /**
     * This method sets the status classification with the given string as categid
     * and the default classid ('state')
     */
    public final void setState(String state) {
        if (state == null) {
            this.state = null;
        } else {
            MCRCategoryID categState = new MCRCategoryID(
                MCRConfiguration2.getString("MCR.Metadata.Service.State.Classification.ID").orElse("state"),
                state);
            setState(categState);
        }
    }

    /**
     * This method removes the current state
     */
    public final void removeState() {
        this.state = null;
    }

    /**
     * This method add a flag to the flag list.
     *
     * @param value -
     *            the new flag as string
     */
    public final void addFlag(String value) {
        addFlag(null, value);
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
        String lType = StringUtils.trim(type);
        MCRUtils.filterTrimmedNotEmpty(value)
            .map(flagValue -> new MCRMetaLangText(ELEMENT_SERVFLAG, null, lType, 0, null, flagValue))
            .ifPresent(flags::add);
    }

    /**
     * This method get all flags from the flag list as a string.
     *
     * @return the flags string
     */
    public final String getFlags() {
        StringBuilder sb = new StringBuilder();

        for (MCRMetaLangText flag : flags) {
            sb.append(flag.getText()).append(' ');
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
    protected final List<MCRMetaLangText> getFlagsAsMCRMetaLangText(String type) {
        return flags.stream()
            .filter(metaLangText -> StringUtils.equals(type, metaLangText.getType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all flag values of the specified type.
     *
     * @param type
     *              a type as string.
     * @return a list of flag values
     */
    public final List<String> getFlags(String type) {
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
     *                throw this exception, if the index is invalid
     * @return a flag string
     */
    public final String getFlag(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= flags.size()) {
            throw new IndexOutOfBoundsException("Index error in getFlag.");
        }
        return flags.get(index).getText();
    }

    /**
     * This method gets a single flag type from the flag list as a string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a flag type
     */
    public final String getFlagType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= flags.size()) {
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
        return MCRUtils.filterTrimmedNotEmpty(value)
            .map(flagValue -> flags.stream().anyMatch(flag -> flagValue.equals(flag.getText())))
            .orElse(false);
    }

    /**
     * Proves if the type is set in the flag list.
     * @param type
     *              a type as string
     * @return  true if the flag list contains flags with this type,
     *          otherwise false
     */
    public final boolean isFlagTypeSet(String type) {
        return flags.stream().anyMatch(flag -> StringUtils.equals(type, flag.getType()));
    }

    /**
     * This method remove a flag from the flag list.
     *
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void removeFlag(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= flags.size()) {
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
        flags.removeIf(f -> StringUtils.equals(type, f.getType()));
    }

    /**
     * This method set a flag in the flag list.
     *
     * @param index
     *            a index in the list
     * @param value
     *            the value of a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceFlag(int index, String value) throws IndexOutOfBoundsException {
        MCRUtils.filterTrimmedNotEmpty(value)
            .ifPresent(flagValue -> updateFlag(index, flag -> flag.setText(value)));
    }

    private void updateFlag(int index, Consumer<MCRMetaLangText> flagUpdater) {
        MCRMetaLangText flag = flags.get(index);
        flagUpdater.accept(flag);
    }

    /**
     * This method sets the type value of a flag at the specified index.
     *
     * @param index
     *            a index in the list
     * @param type
     *            the type a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceFlagType(int index, String type) throws IndexOutOfBoundsException {
        String lType = StringUtils.trim(type);
        updateFlag(index, flag -> flag.setType(lType));
    }

    /**
     * This method add a rule to the rules list.
     *
     * @param permission -
     *            the new permission as string
     * @param condition -
     *            the new rule as JDOM tree Element
     */
    public final void addRule(String permission, Element condition) {
        if (condition == null) {
            return;
        }
        MCRUtils.filterTrimmedNotEmpty(permission)
            .filter(p -> getRuleIndex(p) == -1)
            .map(p -> new MCRMetaAccessRule(ELEMENT_SERVACL, null, 0, p, condition))
            .ifPresent(rules::add);
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
        if (permission == null || permission.isBlank()) {
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
     *                throw this exception, if the index is invalid
     * @return a the MCRMetaAccessRule instance
     */
    public final MCRMetaAccessRule getRule(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= rules.size()) {
            throw new IndexOutOfBoundsException("Index error in getRule.");
        }
        return rules.get(index);
    }

    /**
     * This method get a single permission name of rule from the rules list as a
     * string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a rule permission string
     */
    public final String getRulePermission(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= rules.size()) {
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
     *                throw this exception, if the index is invalid
     */
    public final void removeRule(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= rules.size()) {
            throw new IndexOutOfBoundsException("Index error in removeRule.");
        }
        rules.remove(index);
    }

    /**
     * Returns the rules.
     *
     * @return list of rules
     */
    protected final List<MCRMetaAccessRule> getRules() {
        return rules;
    }

    /**
     * This method create a XML stream for all structure data.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML data of the structure data part
     */
    public final Element createXML() throws MCRException {
        try {
            validate();
        } catch (MCRException exc) {
            throw new MCRException("The content is not valid.", exc);
        }

        Element elm = new Element(XML_NAME);

        // Create XML elements for each list
        addContentIfNotEmpty(elm, dates, ELEMENT_SERVDATES, MCRMetaISO8601Date.class.getSimpleName());
        addContentIfNotEmpty(elm, rules, ELEMENT_SERVACLS, MCRMetaAccessRule.class.getSimpleName());
        addContentIfNotEmpty(elm, flags, ELEMENT_SERVFLAGS, MCRMetaLangText.class.getSimpleName());
        addContentIfNotEmpty(elm, messages, ELEMENT_SERVMESSAGES, MCRMetaDateLangText.class.getSimpleName());
        addContentIfNotEmpty(elm, classifications, ELEMENT_SERVCLASSES, MCRMetaClassification.class.getSimpleName());

        // Handle the state separately
        if (state != null) {
            Element servStates = new Element(ELEMENT_SERVSTATES);
            servStates.setAttribute(MCRXMLConstants.CLASS, MCRMetaClassification.class.getSimpleName());
            MCRMetaClassification stateClass = new MCRMetaClassification(ELEMENT_SERVSTATE, 0, null, state);
            servStates.addContent(stateClass.createXML());
            elm.addContent(servStates);
        }

        return elm;
    }

    private <T extends MCRMetaDefault> void addContentIfNotEmpty(Element parent, List<T> items, String elementName,
        String className) {
        if (items.isEmpty()) {
            return;
        }

        Element elmm = new Element(elementName);
        elmm.setAttribute(MCRXMLConstants.CLASS, className);

        for (T item : items) {
            elmm.addContent(item.createXML());
        }

        parent.addContent(elmm);
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
     *      messages: [
     *          {@link MCRMetaDateLangText#createJSON()},
     *          ...
     *      ],
     *      classifications: [
     *          {@link MCRMetaClassification#createJSON()},
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

        // messages
        if (!getMessages().isEmpty()) {
            JsonArray messages = new JsonArray();
            getMessagesAsList()
                .stream()
                .map(MCRMetaDateLangText::createJSON)
                .forEachOrdered(messages::add);
            service.add("messages", messages);
        }

        // classifications
        if (!getClassifications().isEmpty()) {
            JsonArray classifications = new JsonArray();
            getClassificationsAsList()
                .stream()
                .map(MCRMetaClassification::createJSON)
                .forEachOrdered(classifications::add);
            service.add("classifications", classifications);
        }

        // state
        Optional.ofNullable(getState()).ifPresent(stateId -> {
            JsonObject state = new JsonObject();
            if (stateId.getId() != null) {
                state.addProperty("id", stateId.getId());
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
        return MCRUtils.filterTrimmedNotEmpty(value)
            .map(v -> {
                for (int i = 0; i < flags.size(); i++) {
                    if (flags.get(i).getText().equals(v)) {
                        return i;
                    }
                }
                return -1;
            })
            .orElse(-1);
    }

    /**
     * This method return the size of the message list.
     *
     * @return the size of the message list
     */
    public final int getMessagesSize() {
        return messages.size();
    }

    /**
     * Returns the messages as list.
     *
     * @return messages as list
     */
    protected final List<MCRMetaDateLangText> getMessagesAsList() {
        return messages;
    }

    /**
     * This method returns all message values of the specified type.
     *
     * @param type
     *              a type as string.
     * @return a list of message values
     */
    protected final List<MCRMetaDateLangText> getMessagesAsMCRMetaDateLangText(String type) {
        return messages.stream()
            .filter(metaLangText -> type.equals(metaLangText.getType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all messages values of any type.
     *
     * @return a list of message values
     */
    public final List<String> getMessages() {
        return messages.stream()
            .map(MCRMetaDateLangText::getText)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all messages values of the specified type.
     *
     * @param type
     *              a type as string.
     * @return a list of message values
     */
    public final List<String> getMessages(String type) {
        return getMessagesAsMCRMetaDateLangText(type).stream()
            .map(MCRMetaDateLangText::getText)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method get a single messages from the message list as a string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a message string
     */
    public final String getMessage(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= messages.size()) {
            throw new IndexOutOfBoundsException("Index error in getMessage.");
        }
        return messages.get(index).getText();
    }

    /**
     * This method gets a single message type from the message list as a string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a message type
     */
    public final String getMessageType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= messages.size()) {
            throw new IndexOutOfBoundsException("Index error in getMessageType.");
        }
        return messages.get(index).getType();
    }

    /**
     * This method gets a single message date from the message list as a {@link MCRISO8601Date}.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a message value
     */
    public final MCRISO8601Date getMessageDate(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= messages.size()) {
            throw new IndexOutOfBoundsException("Index error in getMessageMCRMetaLangText.");
        }
        return messages.get(index).getDate();
    }

    /**
     * This method add a message to the flag list.
     *
     * @param value -
     *            the new messages as string
     */
    public final void addMessage(String value) {
        addMessage(null, value);
    }

    /**
     * This method adds a message to the message list.
     *
     * @param type
     *              a type as string
     * @param value
     *              the new message value as string
     */
    public final void addMessage(String type, String value) {
        addMessage(type, value, null);
    }

    /**
     * This method adds a message to the message list.
     *
     * @param type
     *              a type as string
     * @param value
     *              the new message value as string
     * @param form
     *              a form as string, defaults to 'plain'
     */
    public final void addMessage(String type, String value, String form) {
        String lType = StringUtils.trim(type);
        MCRUtils.filterTrimmedNotEmpty(value)
            .map(messageValue -> {
                MCRMetaDateLangText message = new MCRMetaDateLangText(ELEMENT_SERVMESSAGE, null, lType, 0, form,
                    messageValue);
                message.setDate(MCRISO8601Date.now());
                return message;
            })
            .ifPresent(messages::add);
    }

    /**
     * This method set a message in the message list.
     *
     * @param index
     *            a index in the list
     * @param value
     *            the value of a message as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceMessage(int index, String value) throws IndexOutOfBoundsException {
        MCRUtils.filterTrimmedNotEmpty(value)
            .ifPresent(messageValue -> updateMessage(index, message -> message.setText(value)));
    }

    /**
     * This method sets the type value of a message at the specified index.
     *
     * @param index
     *            a index in the list
     * @param type
     *            the type of a message as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceMessageType(int index, String type) throws IndexOutOfBoundsException {
        String lType = StringUtils.trim(type);
        updateMessage(index, message -> message.setType(lType));
    }

    private void updateMessage(int index, Consumer<MCRMetaDateLangText> messageUpdater) {
        MCRMetaDateLangText message = messages.get(index);
        messageUpdater.accept(message);
    }

    /**
     * This method remove a message from the message list.
     *
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void removeMessage(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= messages.size()) {
            throw new IndexOutOfBoundsException("Index error in removeMessage.");
        }
        messages.remove(index);
    }

    /**
     * This method removes all messages of the specified type from
     * the message list.
     *
     * @param type
     *            a type as string
     */
    public final void removeMessages(String type) {
        List<MCRMetaDateLangText> internalList = getMessagesAsMCRMetaDateLangText(type);
        messages.removeAll(internalList);
    }

    /**
     * This method return the size of the classification list.
     *
     * @return the size of the classification list
     */
    public final int getClassificationsSize() {
        return this.classifications.size();
    }

    /**
     * This method returns all classification values.
     *
     * @return classifications as list
     */
    protected final List<MCRMetaClassification> getClassificationsAsList() {
        return classifications;
    }

    /**
     * This method returns all classification values of the specified type.
     *
     * @param type
     *              a type as string.
     * @return a list of classification values
     */
    protected final List<MCRMetaClassification> getClassificationsAsMCRMetaClassification(String type) {
        return classifications.stream()
            .filter(metaLangText -> type.equals(metaLangText.getType()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all classification values of any type.
     *
     * @return a list of classification values
     */
    public final List<MCRCategoryID> getClassifications() {
        return classifications.stream()
            .map(c -> new MCRCategoryID(c.getClassId(), c.getCategId()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method returns all classification values of the specified type.
     *
     * @param type
     *              a type as string.
     * @return a list of classification values
     */
    public final List<MCRCategoryID> getClassifications(String type) {
        return getClassificationsAsMCRMetaClassification(type).stream()
            .map(c -> new MCRCategoryID(c.getClassId(), c.getCategId()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * This method get a single classification from the classification list as a string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a classification string
     */
    public final MCRCategoryID getClassification(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= classifications.size()) {
            throw new IndexOutOfBoundsException("Index error in getClassification.");
        }
        MCRMetaClassification classification = classifications.get(index);
        return new MCRCategoryID(classification.getCategId(), classification.getClassId());
    }

    /**
     * This method gets a single classification type from the classification list as a string.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a classification type
     */
    public final String getClassificationType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= classifications.size()) {
            throw new IndexOutOfBoundsException("Index error in getClassification.");
        }
        return classifications.get(index).getType();
    }

    /**
     * This method get a single classification from the classification list as.
     *
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     * @return a classification value
     */
    public final MCRMetaClassification getClassificationAsMCRMetaClassification(int index)
        throws IndexOutOfBoundsException {
        if (index < 0 || index >= classifications.size()) {
            throw new IndexOutOfBoundsException("Index error in getClassificationAsMCRMetaClassification.");
        }
        return classifications.get(index);
    }

    /**
     * This method adds a classification to the classification list.
     *
     * @param value -
     *            the new classification
     */
    public final void addClassification(MCRCategoryID value) {
        addClassification(null, value);
    }

    /**
     * This method adds a classification to the classification list.
     *
     * @param type
     *              a type as string
     * @param value
     *              the new classification value as {@link MCRCategoryID}
     */
    public final void addClassification(String type, MCRCategoryID value) {
        String lType = StringUtils.trim(type);
        classifications.add(new MCRMetaClassification(ELEMENT_SERVCLASS, 0, lType, value));
    }

    /**
     * This method set a classification in the classification list.
     *
     * @param index
     *            a index in the list
     * @param value
     *            the classification as {@link MCRCategoryID}
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceClassification(int index, MCRCategoryID value) throws IndexOutOfBoundsException {
        updateClassification(index,
            classificationValue -> classificationValue.setValue(value.getRootID(), value.getId()));
    }

    /**
     * This method sets the type value of a classification at the specified index.
     *
     * @param index
     *            a index in the list
     * @param type
     *            the type of a flag as string
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void replaceClassificationType(int index, String type) throws IndexOutOfBoundsException {
        String lType = StringUtils.trim(type);
        updateClassification(index, classification -> classification.setType(lType));
    }

    private void updateClassification(int index, Consumer<MCRMetaClassification> classificationUpdater) {
        MCRMetaClassification classification = classifications.get(index);
        classificationUpdater.accept(classification);
    }

    /**
     * This method remove a classification from the classification list.
     *
     * @param index
     *            a index in the list
     * @exception IndexOutOfBoundsException
     *                throw this exception, if the index is invalid
     */
    public final void removeClassification(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= classifications.size()) {
            throw new IndexOutOfBoundsException("Index error in removeClassification.");
        }
        classifications.remove(index);
    }

    /**
     * This method removes all classification with the specified type from
     * the classification list.
     *
     * @param type
     *            a type as string
     */
    public final void removeClassifications(String type) {
        String lType = StringUtils.trim(type);
        classifications.removeIf(c -> StringUtils.equals(lType, c.getType()));
    }

}
