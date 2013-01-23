/*
 * 
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

package org.mycore.frontend.editor;

import java.util.*;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRConstants;
import org.mycore.frontend.editor.validation.MCRValidator;
import org.mycore.frontend.editor.validation.MCRValidatorBuilder;
import org.mycore.frontend.editor.validation.value.MCRRequiredValidator;

/**
 * Container class that holds all data and files edited and submitted from an
 * HTML page that contains a MyCoRe XML editor form.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2010-11-16 09:59:21 +0100 (Di, 16 Nov
 *          2010) $
 */
public class MCREditorSubmission {
    private final static Logger LOGGER = Logger.getLogger(MCREditorSubmission.class);

    private List variables = new ArrayList();

    private List repeats = new ArrayList();

    private List files = new ArrayList();

    private Hashtable failed = new Hashtable();

    private Hashtable node2file = new Hashtable();

    private Hashtable file2node = new Hashtable();

    private MCRRequestParameters parms;

    private Document xml;

    private String rootName;

    public final static String ATTR_SEP = "__";

    public final static String BLANK = " ";

    public final static String BLANK_ESCAPED = "_-_";

    public final static String SLASH = "/";

    public final static String SLASH_ESCAPED = "_--_";

    /**
     * Set variables from source xml file that should be edited
     * 
     * @param input
     *            the root element of the XML input
     * @param editor
     *            the editor definition
     */
    MCREditorSubmission(Element input, Element editor) {
        setAdditionalNamespaces(editor);
        findPredicatesInMappings(editor);
        rootName = input.getName();
        setVariablesFromXML("", input, new Hashtable());
        setRepeatsFromVariables();
    }

    MCREditorSubmission(MCRRequestParameters parms, Element editor, boolean validate) {
        this.parms = parms;
        rootName = parms.getParameter("_root");
        setVariablesFromSubmission(parms, editor);
        Collections.sort(variables);
        setRepeatsFromSubmission();
        setAdditionalNamespaces(editor);
        if (validate) {
            validate(parms, editor);
        }
    }

    MCREditorSubmission(Element saved, List submitted, String root, MCRRequestParameters parms) {
        Element input = saved.getChild("input");
        List children = input.getChildren();

        String varpath = parms.getParameter("subselect.varpath");
        boolean merge = "true".equals(parms.getParameter("subselect.merge"));

        LinkedHashMap<String, String> table = new LinkedHashMap<String, String>();

        for (Object aChildren : children) {
            Element var = (Element) aChildren;
            String path = var.getAttributeValue("name");
            String value = var.getAttributeValue("value");

            if (merge || !(path.equals(varpath) || path.startsWith(varpath + "/"))) {
                table.put(path, value);
            }
        }

        for (Object aSubmitted : submitted) {
            MCREditorVariable var = (MCREditorVariable) aSubmitted;
            String path = var.getPath();
            String value = var.getValue();
            path = varpath + path.substring(root.length());
            table.put(path, value);
        }

        for (String path : table.keySet()) {
            String value = table.get(path);
            addVariable(path, value);
        }

        Collections.sort(variables);
        setRepeatsFromVariables();
    }

    MCREditorSubmission(Element editor) {
        Element input = editor.getChild("input");
        List children = input.getChildren();

        for (Object aChildren : children) {
            Element var = (Element) aChildren;
            String path = var.getAttributeValue("name");
            String value = var.getAttributeValue("value");
            addVariable(path, value);
        }

        Collections.sort(variables);
        setAdditionalNamespaces(editor);
        setRepeatsFromVariables();
    }

    private String getNamespacePrefix(Namespace ns) {
        if (ns == null || ns.equals(Namespace.NO_NAMESPACE)) {
            return "";
        }
        for (String key : nsMap.keySet()) {
            if (ns.equals(nsMap.get(key))) {
                return key + ":";
            }
        }
        String msg = "Namespace " + ns.getURI() + " used in editor source input, but not declared in editor definition. Using: " + ns.getPrefix();
        LOGGER.warn(msg);
        return ns.getPrefix() + ":";
    }

    private void setVariablesFromXML(String prefix, Element element, Hashtable predecessors) {
        String key = getNamespacePrefix(element.getNamespace()) + element.getName();

        setVariablesFromXML(prefix, key, element, predecessors);

        List attributes = element.getAttributes();
        for (Object attribute1 : attributes) {
            Attribute attribute = (Attribute) attribute1;
            String name = getNamespacePrefix(attribute.getNamespace()) + attribute.getName();
            String value = attribute.getValue().replace(BLANK, BLANK_ESCAPED).replace(SLASH, SLASH_ESCAPED);
            if (value == null || value.length() == 0) {
                continue;
            }
            key = getNamespacePrefix(element.getNamespace()) + element.getName() + ATTR_SEP + name + ATTR_SEP + value;
            if (predicates.containsKey(key)) {
                setVariablesFromXML(prefix, key, element, predecessors);
            }
        }
    }

    private void setVariablesFromXML(String prefix, String key, Element element, Hashtable predecessors) {
        int pos = 1;
        if (predecessors.containsKey(key)) {
            pos = (Integer) predecessors.get(key) + 1;
        }
        predecessors.put(key, pos);

        String path = prefix + "/" + key;
        if (pos > 1) {
            path = path + "[" + pos + "]";
        }

        // Add element text
        addVariable(path, element.getText());

        // Add value of all attributes
        List attributes = element.getAttributes();
        for (Object attribute1 : attributes) {
            Attribute attribute = (Attribute) attribute1;
            String value = attribute.getValue();
            if (value != null && value.length() > 0) {
                addVariable(path + "/@" + getNamespacePrefix(attribute.getNamespace()) + attribute.getName(), value);
            }
        }

        // Add values of all children
        predecessors = new Hashtable();
        List children = element.getChildren();
        for (Object aChildren : children) {
            Element child = (Element) aChildren;
            setVariablesFromXML(path, child, predecessors);
        }
    }

    /** predicates on attributes, e.g. title[@type='main'] */
    private Hashtable predicates = new Hashtable();

    /** Fills a table of predicates on attributes, e.g. title[@type='main'] */
    private void findPredicatesInMappings(Element elem) {
        String var = elem.getAttributeValue("var");
        if (var != null)
            fillPredicatesTable(var);
        for (Element child : (List<Element>) (elem.getChildren()))
            findPredicatesInMappings(child);
    }

    private void fillPredicatesTable(String var) {
        int beginOfPredicate = var.indexOf("[@");
        while (beginOfPredicate != -1) {
            String name = var.substring(0, beginOfPredicate).trim();
            if (name.contains("/")) {
                name = name.substring(name.lastIndexOf("/") + 1).trim();
            }
            if (name.contains("["))
                name = name.substring(0, name.indexOf("["));

            String predicate = escapePredicate(var, beginOfPredicate);
            String key = name + predicate;
            String value = predicate.substring(predicate.lastIndexOf(ATTR_SEP) + ATTR_SEP.length());
            predicates.put(key, value);

            beginOfPredicate = var.indexOf("[@", beginOfPredicate + 4);
        }
    }

    static String escapePredicate(String var, int beginOfPredicate) {
        int pos2 = var.indexOf("=", beginOfPredicate);
        int pos3 = var.indexOf("]", beginOfPredicate);
        String attr = var.substring(beginOfPredicate + 2, pos2).trim();
        String value = var.substring(pos2 + 2, pos3 - 1).trim().replace(BLANK, BLANK_ESCAPED).replace(SLASH, SLASH_ESCAPED);
        return ATTR_SEP + attr + ATTR_SEP + value;
    }

    private void setVariablesFromSubmission(MCRRequestParameters parms, Element editor) {
        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();

            if (name.startsWith("_")) {
                continue; // Skip internal request params
            }

            String[] values = parms.getParameterValues(name);
            String sortNr = parms.getParameter("_sortnr-" + name);
            String ID = parms.getParameter("_id@" + name);

            // Skip files that should be deleted
            String delete = parms.getParameter("_delete-" + name);

            if ("true".equals(delete) && parms.getFileItem(name) == null) {
                continue;
            }

            // Skip request params that are not input but target params
            if (sortNr == null) {
                continue;
            }

            // For each value
            for (int k = 0; values != null && k < values.length; k++) {
                String value = values[k];

                if (value == null || value.trim().length() == 0) {
                    continue;
                }

                // Handle multiple variables with same name: checkboxes & select
                // multiple
                String nname = k == 0 ? name : name + "[" + (k + 1) + "]";

                MCREditorVariable var = new MCREditorVariable(nname, value);
                var.setSortNr(sortNr);

                // Add associated component from editor definition
                if (ID != null && ID.trim().length() > 0) {
                    Element component = MCREditorDefReader.findElementByID(ID, editor);

                    if (component != null) {
                        // Skip variables with values equal to autofill text
                        String attrib = component.getAttributeValue("autofill");
                        String elem = component.getChildTextTrim("autofill");
                        String autofill = null;

                        if (attrib != null && attrib.trim().length() > 0) {
                            autofill = attrib.trim();
                        } else if (attrib != null && attrib.trim().length() > 0) {
                            autofill = elem.trim();
                        }

                        if (value.trim().equals(autofill)) {
                            continue;
                        }
                    }
                }

                variables.add(var);

                FileItem file = parms.getFileItem(name);

                if (file != null) // Add associated uploaded file if it exists
                {
                    var.setFile(file);
                    files.add(file);
                }
            }
        }
    }

    private void validate(MCRRequestParameters parms, Element editor) {
        LOGGER.info("Validating editor input... ");

        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();

            if (!name.startsWith("_sortnr-")) {
                continue;
            }

            name = name.substring(8);

            String ID = parms.getParameter("_id@" + name);

            if (ID == null || ID.trim().length() == 0) {
                continue;
            }

            String[] values = { "" };

            if (parms.getParameterValues(name) != null) {
                values = parms.getParameterValues(name);
            }

            Element component = MCREditorDefReader.findElementByID(ID, editor);

            if (component == null) {
                continue;
            }

            List conditions = component.getChildren("condition");

            if (conditions == null) {
                continue;
            }

            // Skip variables with values equal to autofill text
            String attrib = component.getAttributeValue("autofill");
            String elem = component.getChildTextTrim("autofill");
            String autofill = null;

            if (attrib != null && attrib.trim().length() > 0) {
                autofill = attrib.trim();
            } else if (attrib != null && attrib.trim().length() > 0) {
                autofill = elem.trim();
            }

            if (values[0].trim().equals(autofill)) {
                values[0] = "";
            }

            for (Object condition1 : conditions) {
                Element condition = (Element) condition1;

                boolean ok = true;
                for (int j = 0; j < values.length && ok; j++) {
                    String nname = j == 0 ? name : name + "[" + (j + 1) + "]";
                    ok = checkCondition(condition, nname, values[j]);

                    if (!ok) {
                        String sortNr = parms.getParameter("_sortnr-" + name);
                        failed.put(sortNr, condition);

                        if (LOGGER.isDebugEnabled()) {
                            String cond = new XMLOutputter(Format.getCompactFormat()).outputString(condition);
                            LOGGER.debug("Validation condition failed:");
                            LOGGER.debug(nname + " = \"" + values[j] + "\"");
                            LOGGER.debug(cond);
                        }
                    }
                }
            }
        }

        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();

            if (!name.startsWith("_cond-")) {
                continue;
            }

            String path = name.substring(6);
            String[] ids = parms.getParameterValues(name);

            if (ids != null) {
                for (String id : ids) {
                    Element condition = MCREditorDefReader.findElementByID(id, editor);

                    if (condition == null) {
                        continue;
                    }

                    String field1 = condition.getAttributeValue("field1", "");
                    String field2 = condition.getAttributeValue("field2", "");

                    if (!field1.isEmpty() || !field2.isEmpty()) {
                        String pathA = path + ((!field1.isEmpty() && !field1.equals(".")) ? "/" + field1 : "");
                        String pathB = path + ((!field2.isEmpty() && !field2.equals(".")) ? "/" + field2 : "");

                        String valueA = parms.getParameter(pathA);
                        String valueB = parms.getParameter(pathB);

                        String sortNrA = parms.getParameter("_sortnr-" + pathA);
                        String sortNrB = parms.getParameter("_sortnr-" + pathB);

                        boolean pairValuesAlreadyInvalid = (failed.containsKey(sortNrA) || failed.containsKey(sortNrB));

                        if (!pairValuesAlreadyInvalid) {
                            MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedPairValidator();
                            setValidatorProperties(validator, condition);
                            if (!validator.isValid(valueA, valueB)) {
                                failed.put(sortNrA, condition);
                                failed.put(sortNrB, condition);
                            }
                        }
                    } else {

                        Element current = null;
                        try {
                            XPath xpath = XPath.newInstance(path);
                            for (Namespace namespace : getNamespaceMap().values())
                                xpath.addNamespace(namespace);
                            current = (Element) xpath.selectSingleNode(getXML());
                        } catch (JDOMException ex) {
                            LOGGER.debug("Could not validate, because no element found at xpath " + path);
                            continue;
                        }

                        MCRValidator validator = MCRValidatorBuilder.buildPredefinedCombinedElementValidator();
                        setValidatorProperties(validator, condition);
                        if (!validator.isValid(current)) {
                            String sortNr = parms.getParameter("_sortnr-" + path);
                            failed.put(sortNr, condition);
                        }
                    }
                }
            }
        }
    }

    private boolean checkCondition(Element condition, String name, String value) {
        value = (value == null ? "" : value.trim());

        MCRValidator validator;
        if (value.isEmpty()) {
            validator = new MCRRequiredValidator();
        } else {
            validator = MCRValidatorBuilder.buildPredefinedCombinedValidator();
        }

        setValidatorProperties(validator, condition);
        setRequiredProperty(validator, condition, name);
        return validator.isValid(value);
    }

    private void setRequiredProperty(MCRValidator validator, Element condition, String name) {
        boolean required = "true".equals(condition.getAttributeValue("required"));
        boolean repeated = name.endsWith("]");
        required = required && !repeated;
        validator.setProperty("required", Boolean.toString(required));
    }

    private void setValidatorProperties(MCRValidator validator, Element condition) {
        for (Attribute attribute : (List<Attribute>) (condition.getAttributes())) {
            if (!attribute.getValue().isEmpty())
                validator.setProperty(attribute.getName(), attribute.getValue());
        }
    }

    private void addVariable(String path, String text) {
        if (text == null || text.trim().length() == 0) {
            return;
        }

        LOGGER.debug("Editor variable " + path + "=" + text);
        variables.add(new MCREditorVariable(path, text));
    }

    public List getVariables() {
        return variables;
    }

    public List getFiles() {
        return files;
    }

    public FileItem getFile(Object xmlNode) {
        if (xml == null) {
            buildTargetXML();
        }

        return (FileItem) node2file.get(xmlNode);
    }

    public Object getXMLNode(FileItem file) {
        if (xml == null) {
            buildTargetXML();
        }

        return file2node.get(file);
    }

    public Document getXML() {
        if (xml == null) {
            buildTargetXML();
        }

        return xml;
    }

    void setXML(Document xml) {
        this.xml = xml;
    }

    Element buildInputElements() {
        Element input = new Element("input");

        for (Object variable : variables) {
            MCREditorVariable var = (MCREditorVariable) variable;
            input.addContent(var.asInputElement());
        }

        return input;
    }

    Element buildRepeatElements() {
        Element eRepeats = new Element("repeats");

        for (Object repeat : repeats) {
            MCREditorVariable var = (MCREditorVariable) repeat;
            eRepeats.addContent(var.asRepeatElement());
        }

        return eRepeats;
    }

    Element buildFailedConditions() {
        if (failed.isEmpty()) {
            return null;
        }

        Element failedConds = new Element("failed");

        for (Enumeration e = failed.keys(); e.hasMoreElements();) {
            String sortNr = (String) e.nextElement();
            Element condition = (Element) failed.get(sortNr);
            Element field = new Element("field");
            field.setAttribute("sortnr", sortNr);
            field.setAttribute("condition", condition.getAttributeValue("id"));
            failedConds.addContent(field);
        }

        failed = new Hashtable();

        return failedConds;
    }

    boolean errors() {
        return !failed.isEmpty();
    }

    public MCRRequestParameters getParameters() {
        return parms;
    }

    private void buildTargetXML() {
        Element root;
        if (variables.size() > 0) {
            root = buildElement(((MCREditorVariable) variables.get(0)).getPathElements()[0]);
        } else {
            root = buildElement(rootName.replace("/", ""));
        }

        for (Object variable : variables) {
            MCREditorVariable var = (MCREditorVariable) variable;

            Element parent = root;
            String[] elements = var.getPathElements();

            for (int j = 1; j < elements.length; j++) {
                String name = elements[j];

                if (name.endsWith("]")) {
                    int pos = name.lastIndexOf("[");
                    name = name.substring(0, pos) + "_XXX_" + name.substring(pos + 1, name.length() - 1);
                }

                Namespace ns = getNamespace(name);
                if (!ns.equals(Namespace.NO_NAMESPACE)) {
                    name = name.substring(name.indexOf(":") + 1);
                }
                Element child = parent.getChild(name, ns);

                if (child == null) {
                    child = new Element(name, ns);
                    parent.addContent(child);
                }

                parent = child;
            }

            Object node;

            if (!var.isAttribute()) {
                parent.addContent(var.getValue());
                node = parent;
            } else {
                LOGGER.debug("Setting attribute " + var.getPath() + " = " + var.getValue());
                setAttribute(parent, var.getAttributeName(), var.getValue());
                node = parent.getAttribute(var.getAttributeName());
            }

            FileItem file = parms == null ? null : parms.getFileItem(var.getPath());

            if (file != null) {
                file2node.put(file, node);
                node2file.put(node, file);
            }
        }

        renameRepeatedElements(root);
        xml = new Document(root);
    }

    /**
     * A map from namespace prefix to namespace for the namespaces registered in
     * the editor definition.
     */
    private HashMap<String, Namespace> nsMap = new HashMap<String, Namespace>();

    /**
     * A map from namespace prefix to namespace for the namespaces registered in
     * the editor definition.
     */
    public HashMap<String, Namespace> getNamespaceMap() {
        return nsMap;
    }

    /**
     * Stores the list of additional namespaces declared in the components
     * element of the editor definition. These namespaces and its prefixes can
     * be used in editor variable paths (var attributes of cells).
     */
    @SuppressWarnings("unchecked")
    private void setAdditionalNamespaces(Element editor) {
        Element components = editor.getChild("components");
        List<Namespace> namespaces = components.getAdditionalNamespaces();
        for (Namespace ns : namespaces) {
            nsMap.put(ns.getPrefix(), ns);
        }
        nsMap.put("xml", Namespace.XML_NAMESPACE);
        for (Namespace ns : MCRConstants.getStandardNamespaces()) {
            setNamespaceIfUndefined(ns);
        }
    }

    private void setNamespaceIfUndefined(Namespace namespace) {
        if (!nsMap.containsKey(namespace.getPrefix())) {
            nsMap.put(namespace.getPrefix(), namespace);
        }
    }

    /**
     * Extracts namespace prefix from the given name (the part before the ":")
     * and resolves it to the namespace registered in the editor definition.
     */
    private Namespace getNamespace(String name) {
        int pos1 = name.indexOf(":");
        int pos2 = name.indexOf(ATTR_SEP);
        if (pos1 == -1 || pos1 > pos2 && pos2 >= 0) {
            return Namespace.NO_NAMESPACE;
        }
        String prefix = name.substring(0, pos1);
        if (!nsMap.containsKey(prefix)) {
            String msg = "Namespace prefix " + prefix + " is used in editor variable, but not defined";
            throw new MCRConfigurationException(msg);
        }
        return nsMap.get(prefix);
    }

    /**
     * Builds a new XML element for data output. The name may contain a
     * namespace prefix, which is resolved to a namespace then.
     */
    private Element buildElement(String name) {
        Namespace ns = getNamespace(name);
        if (!ns.equals(Namespace.NO_NAMESPACE)) {
            name = name.substring(name.indexOf(":") + 1);
        }
        return new Element(name, ns);
    }

    /**
     * Sets attribute value of the given parent element. The name may contain a
     * namespace prefix, which is resolved to a namespace then.
     */
    private void setAttribute(Element parent, String name, String value) {
        Namespace ns = getNamespace(name);
        if (!ns.equals(Namespace.NO_NAMESPACE)) {
            name = name.substring(name.indexOf(":") + 1);
        }
        parent.setAttribute(name, value, ns);
    }

    private void renameRepeatedElements(Element element) {
        String name = element.getName();
        int pos = name.lastIndexOf("_XXX_");

        if (pos >= 0) {
            name = name.substring(0, pos);
            element.setName(name);
        }

        pos = name.indexOf(ATTR_SEP);
        if (pos > 0) {
            element.setName(name.substring(0, pos));
            int pos2 = name.indexOf(ATTR_SEP, pos + 2);
            String attr = name.substring(pos + 2, pos2);
            String val = name.substring(pos2 + 2).replace(BLANK_ESCAPED, BLANK).replace(SLASH_ESCAPED, SLASH);
            setAttribute(element, attr, val);
        }

        List children = element.getChildren();

        for (Object aChildren : children) {
            renameRepeatedElements((Element) aChildren);
        }
    }

    private void setRepeatsFromVariables() {
        Hashtable maxtable = new Hashtable();

        for (Object variable : variables) {
            MCREditorVariable var = (MCREditorVariable) variable;
            String[] path = var.getPathElements();
            String prefix = "/" + path[0];

            for (int j = 1; j < path.length; j++) {
                String name = path[j];
                int pos1 = name.lastIndexOf("[");
                int pos2 = name.lastIndexOf("]");

                if (pos1 != -1) {
                    String elem = name.substring(0, pos1);
                    String num = name.substring(pos1 + 1, pos2);
                    String key = prefix + "/" + elem;

                    int numNew = Integer.parseInt(num);

                    if (maxtable.containsKey(key)) {
                        int numOld = Integer.parseInt((String) maxtable.get(key));
                        maxtable.remove(key);
                        numNew = Math.max(numOld, numNew);
                    }

                    maxtable.put(key, String.valueOf(numNew));
                }

                prefix = prefix + "/" + name;
            }
        }

        for (Enumeration e = maxtable.keys(); e.hasMoreElements();) {
            String path = (String) e.nextElement();
            String value = (String) maxtable.get(path);

            repeats.add(new MCREditorVariable(path, value));
            LOGGER.debug("Editor repeats " + path + " = " + value);
        }
    }

    private void setRepeatsFromSubmission() {
        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String parameter = (String) e.nextElement();

            if (parameter.startsWith("_n-")) {
                String value = parms.getParameter(parameter);
                repeats.add(new MCREditorVariable(parameter.substring(3), value));
                LOGGER.debug("Editor repeats " + parameter.substring(3) + " = " + value);
            }
        }
    }

    void doPlus(String prefix, int nr) {
        changeRepeatNumber(prefix, +1);
        changeVariablesAndRepeats(prefix, nr, +1);
    }

    void doMinus(String prefix, int nr) {
        changeRepeatNumber(prefix, -1);

        String prefix2;

        if (nr > 1) {
            prefix2 = prefix + "[" + nr + "]";
        } else {
            prefix2 = prefix;
        }

        for (int i = 0; i < variables.size(); i++) {
            String path = ((MCREditorVariable) variables.get(i)).getPath();

            if (path.startsWith(prefix2 + "/") || path.equals(prefix2)) {
                variables.remove(i--);
            }
        }

        for (int i = 0; i < repeats.size(); i++) {
            String path = ((MCREditorVariable) repeats.get(i)).getPath();

            if (path.startsWith(prefix2 + "/")) {
                repeats.remove(i--);
            }
        }

        changeVariablesAndRepeats(prefix, nr, -1);
    }

    void doUp(String prefix, int nr) {
        String prefix1 = prefix + (nr > 2 ? "[" + String.valueOf(nr - 1) + "]" : "");
        String prefix2 = prefix + "[" + String.valueOf(nr) + "]";

        for (Object variable : variables) {
            MCREditorVariable var = (MCREditorVariable) variable;
            String path = var.getPath();

            if (path.startsWith(prefix1 + "/") || path.equals(prefix1)) {
                String rest = path.substring(prefix1.length());
                var.setPath(prefix2 + rest);
            } else if (path.startsWith(prefix2) || path.equals(prefix2)) {
                String rest = path.substring(prefix2.length());
                var.setPath(prefix1 + rest);
            }
        }

        for (Object repeat : repeats) {
            MCREditorVariable var = (MCREditorVariable) repeat;
            String path = var.getPath();

            if (path.startsWith(prefix1 + "/")) {
                String rest = path.substring(prefix1.length());
                var.setPath(prefix2 + rest);
            } else if (path.startsWith(prefix2 + "/")) {
                String rest = path.substring(prefix2.length());
                var.setPath(prefix1 + rest);
            }
        }
    }

    void changeRepeatNumber(String prefix, int change) {
        for (Object repeat : repeats) {
            MCREditorVariable var = (MCREditorVariable) repeat;

            if (var.getPath().equals(prefix)) {
                int value = Integer.parseInt(var.getValue()) + change;

                if (value == 0) {
                    value = 1;
                }

                var.setValue(String.valueOf(value));

                return;
            }
        }
    }

    void changeVariablesAndRepeats(String prefix, int nr, int change) {
        ArrayList list = new ArrayList();
        list.addAll(variables);
        list.addAll(repeats);

        for (Object aList : list) {
            MCREditorVariable var = (MCREditorVariable) aList;
            String path = var.getPath();

            if (!path.startsWith(prefix + "[")) {
                continue;
            }

            String rest = path.substring(prefix.length() + 1);

            int pos = rest.indexOf("]");
            int num = Integer.parseInt(rest.substring(0, pos));

            if (num > nr) {
                num += change;

                StringBuilder newpath = new StringBuilder(prefix);

                if (num > 1) {
                    newpath.append("[").append(num).append("]");
                }

                newpath.append(rest.substring(pos + 1));

                var.setPath(newpath.toString());
            }
        }
    }
}
