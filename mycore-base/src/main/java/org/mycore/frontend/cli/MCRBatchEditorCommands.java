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

package org.mycore.frontend.cli;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Commands to batch add/remove/replace values
 * like identifiers, categories, tags, flags etc. within XML.
 * Supported fields are completely configurable using XPath expressions.
 *
 * @author Frank L\u00FCtzenkirchen
 */
@MCRCommandGroup(name = "Batch Editor")
public class MCRBatchEditorCommands extends MCRAbstractCommands {

    private static final Collection<Namespace> NS = MCRConstants.getStandardNamespaces();

    private static final Filter<Element> FE = Filters.element();

    private static final String CFG_PREFIX = "MCR.BatchEditor.";

    private static final String CFG_PREFIX_BASE = CFG_PREFIX + "BaseLevel.";

    private static final String CFG_SUFFIX_REMOVE = ".Path2Remove";

    private static final String CFG_SUFFIX_ADD = ".Path2Add";

    private enum Action {
        ADD, ADD_IF, REMOVE, REMOVE_IF, REPLACE
    }

    @MCRCommand(syntax = "edit {0} at {1} add {2} {3}",
        help = "Edit XML elements in object {0} at level {1} in object {1}, add field {2} with value {3}",
        order = 2)
    public static void batchAdd(String oid, String level, String field, String value)
        throws JaxenException, MCRPersistenceException, MCRAccessException, IOException {
        edit(oid, level, Action.ADD, field, value, null, null);
    }

    @MCRCommand(syntax = "edit {0} at {1} if {2} {3} add {4} {5}",
        help = "Edit XML elements in object {0} at level {1}, if there is a field {2} with value {3}, "
            + "add field {4} with value {5}",
        order = 1)
    public static void batchAddIf(String oid, String level, String fieldIf, String valueIf, String field2Add,
        String value2Add)
        throws JaxenException, MCRPersistenceException, MCRAccessException, IOException {
        edit(oid, level, Action.ADD_IF, fieldIf, valueIf, field2Add, value2Add);
    }

    @MCRCommand(syntax = "edit {0} at {1} remove {2} {3}",
        help = "Edit XML elements at in object {0} at level {1}, remove field {2} where value is {3}",
        order = 2)
    public static void batchRemove(String oid, String level, String field, String value)
        throws MCRPersistenceException, MCRAccessException, JaxenException, IOException {
        edit(oid, level, Action.REMOVE, field, value, null, null);
    }

    @MCRCommand(syntax = "edit {0} at {1} if {2} {3} remove {4} {5}",
        help = "Edit XML elements in object {0} at level {1}, if there is a field {2} with value {3}, "
            + "remove field {4} where value is {5}",
        order = 1)
    public static void batchRemoveIf(String oid, String level, String fieldIf, String valueIf, String field2Rem,
        String value2Rem)
        throws MCRPersistenceException, MCRAccessException, JaxenException, IOException {
        edit(oid, level, Action.REMOVE_IF, fieldIf, valueIf, field2Rem, value2Rem);
    }

    @MCRCommand(syntax = "edit {0} at {1} replace {2} {3} with {4} {5}",
        help = "Edit XML elements in object {0} at level {1}, replace field {2} value {3} by field {4} with value {5}",
        order = 1)
    public static void batchReplace(String oid, String level, String oldField, String oldValue, String newField,
        String newValue)
        throws JaxenException, MCRPersistenceException, MCRAccessException, IOException {
        edit(oid, level, Action.REPLACE, oldField, oldValue, newField, newValue);
    }

    public static void edit(String oid, String level, Action a, String field1, String value1, String field2,
        String value2)
        throws JaxenException, MCRPersistenceException, MCRAccessException, IOException {
        Document xmlOld = getObjectXML(oid);
        Document xmlNew = xmlOld.clone();

        for (Element base : getlevelElements(xmlNew, level)) {
            if (a == Action.ADD) {
                add(base, field1, value1);
            } else if (a == Action.REMOVE) {
                find(base, field1, value1).forEach(Element::detach);
            } else if (a == Action.REPLACE) {
                List<Element> found = find(base, field1, value1);
                found.forEach(Element::detach);
                if (!found.isEmpty()) {
                    add(base, field2, value2);
                }
            } else if (a == Action.ADD_IF) {
                if (!find(base, field1, value1).isEmpty()) {
                    add(base, field2, value2);
                }
            } else if (a == Action.REMOVE_IF) {
                if (!find(base, field1, value1).isEmpty()) {
                    find(base, field2, value2).forEach(Element::detach);
                }
            }
        }

        saveIfModified(xmlOld, xmlNew);
    }

    private static Document getObjectXML(String objectID) {
        MCRObjectID oid = MCRObjectID.getInstance(objectID);
        MCRObject obj = MCRMetadataManager.retrieveMCRObject(oid);
        return obj.createXML();
    }

    private static void saveIfModified(Document xmlOld, Document xmlNew) throws MCRAccessException, IOException {
        String oldData = new MCRJDOMContent(xmlOld).asString();
        String newData = new MCRJDOMContent(xmlNew).asString();
        if (!oldData.equals(newData)) {
            MCRObject obj = new MCRObject(xmlNew);
            MCRMetadataManager.update(obj);
        }
    }

    private static List<Element> getlevelElements(Document xml, String level) {
        String path = MCRConfiguration2.getStringOrThrow(CFG_PREFIX_BASE + level);
        XPathExpression<Element> xPath = XPathFactory.instance().compile(path, FE, null, NS);
        return xPath.evaluate(xml);
    }

    private static void add(Element base, String field, String value) throws JaxenException {
        String path = MCRConfiguration2.getStringOrThrow(CFG_PREFIX + field + CFG_SUFFIX_ADD);
        path = new MessageFormat(path, Locale.ROOT).format(new String[] { value });
        new MCRNodeBuilder().buildNode(path, null, base);
    }

    private static List<Element> find(Element base, String field, String value) {
        String path = MCRConfiguration2.getStringOrThrow(CFG_PREFIX + field + CFG_SUFFIX_REMOVE);
        path = new MessageFormat(path, Locale.ROOT).format(new String[] { value });
        XPathExpression<Element> fPath = XPathFactory.instance().compile(path, FE, null, NS);
        List<Element> selected = fPath.evaluate(base);
        return selected;
    }
}
