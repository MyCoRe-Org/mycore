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

package org.mycore.frontend.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * Container class that holds all data and files edited and submitted from an
 * HTML page that contains a MyCoRe XML editor form.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
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

    /**
     * Set variables from source xml file that should be edited
     * 
     * @param input
     *            the root element of the XML input
     */
    MCREditorSubmission(Element input) {
        setVariablesFromElement(input, "/", "");
        setRepeatsFromVariables();
    }

    MCREditorSubmission(MCRRequestParameters parms, Element editor, boolean validate) {
        this.parms = parms;
        setVariablesFromSubmission(parms, editor, validate);
        Collections.sort(variables);
        setRepeatsFromSubmission();
    }

    MCREditorSubmission(Element saved, List submitted, String root, String varpath) {
        Element input = saved.getChild("input");
        List children = input.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Element var = (Element) (children.get(i));
            String path = var.getAttributeValue("name");
            String value = var.getAttributeValue("value");

            if (path.equals(varpath) || path.startsWith(varpath + "/")) {
                continue;
            }
            addVariable(path, value);
        }

        for (int i = 0; i < submitted.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (submitted.get(i));
            String path = var.getPath();
            String value = var.getValue();
            path = varpath + path.substring(root.length());
            addVariable(path, value);
        }

        Collections.sort(variables);
        setRepeatsFromVariables();
    }

    private void setVariablesFromElement(Element element, String prefix, String suffix) {
        String path = prefix + element.getName() + suffix;
        String text = element.getText();

        addVariable(path, text);

        List attributes = element.getAttributes();

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = (Attribute) (attributes.get(i));
            addVariable(path + "/@" + attribute.getName(), attribute.getValue());
        }

        List children = element.getChildren();

        for (int i = 0, nr = 1; i < children.size(); i++) {
            Element child = (Element) (children.get(i));
            suffix = "";

            if (i > 0) {
                Element before = (Element) (children.get(i - 1));

                if (child.getName().equals(before.getName())) {
                    suffix = "[" + String.valueOf(++nr) + "]";
                } else {
                    nr = 1;
                }
            }

            setVariablesFromElement(child, path + "/", suffix);
        }
    }

    private void setVariablesFromSubmission(MCRRequestParameters parms, Element editor, boolean validate) {
        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());

            if (name.startsWith("_")) {
                continue; // Skip internal request params
            }

            String[] values = parms.getParameterValues(name);
            String sortNr = parms.getParameter("_sortnr-" + name);
            String ID = parms.getParameter("_id@" + name);

            // Skip files that should be deleted
            String delete = parms.getParameter("_delete-" + name);

            if ("true".equals(delete) && (parms.getFileItem(name) == null)) {
                continue;
            }

            // Skip request params that are not input but target params
            if (sortNr == null) {
                continue;
            }

            // For each value
            for (int k = 0; (values != null) && (k < values.length); k++) {
                String value = values[k];

                if ((value == null) || (value.trim().length() == 0)) {
                    continue;
                }

                // Handle multiple variables with same name: checkboxes & select
                // multiple
                String nname = ((k == 0) ? name : (name + "[" + (k + 1) + "]"));

                MCREditorVariable var = new MCREditorVariable(nname, value);
                var.setSortNr(sortNr);

                // Add associated component from editor definition
                if ((ID != null) && (ID.trim().length() > 0)) {
                    Element component = MCREditorDefReader.findElementByID(ID, editor);

                    if (component != null) {
                        // Skip variables with values equal to autofill text
                        String attrib = component.getAttributeValue("autofill");
                        String elem = component.getChildTextTrim("autofill");
                        String autofill = null;

                        if ((attrib != null) && (attrib.trim().length() > 0)) {
                            autofill = attrib.trim();
                        } else if ((attrib != null) && (attrib.trim().length() > 0)) {
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

        if (validate) {
            validate(parms, editor);
        }
    }

    private void validate(MCRRequestParameters parms, Element editor) {
        LOGGER.info("Validating editor input... ");

        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());

            if (!name.startsWith("_sortnr-")) {
                continue;
            }

            name = name.substring(8);

            String ID = parms.getParameter("_id@" + name);

            if ((ID == null) || (ID.trim().length() == 0)) {
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

            if ((attrib != null) && (attrib.trim().length() > 0)) {
                autofill = attrib.trim();
            } else if ((attrib != null) && (attrib.trim().length() > 0)) {
                autofill = elem.trim();
            }

            if (values[0].trim().equals(autofill)) {
                values[0] = "";
            }

            for (int i = 0; i < conditions.size(); i++) {
                Element condition = (Element) (conditions.get(i));

                boolean ok = true;
                for (int j = 0; (j < values.length) && ok; j++) {
                    String nname = ((j == 0) ? name : (name + "[" + (j + 1) + "]"));
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
            String name = (String) (e.nextElement());

            if (!name.startsWith("_cond-")) {
                continue;
            }

            String path = name.substring(6);
            String[] ids = parms.getParameterValues(name);

            if (ids != null) {
                for (int i = 0; i < ids.length; i++) {
                    String id = ids[i];
                    Element condition = MCREditorDefReader.findElementByID(id, editor);

                    if (condition == null) {
                        continue;
                    }

                    String pathA = path + "/" + condition.getAttributeValue("field1");
                    String pathB = path + "/" + condition.getAttributeValue("field2");
                    String valueA = parms.getParameter(pathA);
                    String valueB = parms.getParameter(pathB);

                    String type = condition.getAttributeValue("type");
                    String oper = condition.getAttributeValue("operator");
                    String format = condition.getAttributeValue("format");

                    String clazz = condition.getAttributeValue("class");
                    String method = condition.getAttributeValue("method");

                    boolean ok = true;

                    if ((oper != null) && (oper.length() > 0))
                        ok = MCRInputValidator.instance().compare(valueA, valueB, oper, type, format);
                    if ((clazz != null) && (clazz.length() > 0) && (condition.getAttributeValue("field1") != null) && (condition.getAttributeValue("field2") != null))
                        ok = ok && MCRInputValidator.instance().validateExternally(clazz, method, valueA, valueB);

                    if (!ok) {
                        String sortNrA = parms.getParameter("_sortnr-" + pathA);
                        failed.put(sortNrA, condition);

                        String sortNrB = parms.getParameter("_sortnr-" + pathB);
                        failed.put(sortNrB, condition);

                        if (LOGGER.isDebugEnabled()) {
                            String cond = new XMLOutputter(Format.getCompactFormat()).outputString(condition);
                            LOGGER.debug("Validation condition failed:");
                            LOGGER.debug(pathA + " " + oper + " " + pathB);
                            LOGGER.debug(cond);
                        }
                    } else {
                        Element current = null;

                        try {
                            String xslcond = condition.getAttributeValue("xsl");
                            if ((xslcond != null) && (xslcond.length() > 0)) {
                                current = (Element) (XPath.selectSingleNode(this.getXML(), path));
                                ok = MCRInputValidator.instance().validateXSLCondition(current, xslcond);
                                if ((!ok) && LOGGER.isDebugEnabled()) {
                                    String xml = new XMLOutputter(Format.getPrettyFormat()).outputString(current);
                                    LOGGER.debug("Validation condition failed:");
                                    LOGGER.debug("Context xpath: " + path);
                                    LOGGER.debug("XSL condition: " + xslcond);
                                    LOGGER.debug(xml);
                                }
                            }
                            if ((clazz != null) && (clazz.length() > 0)) {
                                if (current == null)
                                    current = (Element) (XPath.selectSingleNode(this.getXML(), path));
                                ok = ok && MCRInputValidator.instance().validateExternally(clazz, method, current);
                            }
                        } catch (JDOMException ex) {
                            LOGGER.debug("Could not validate, because no element found at xpath " + path);
                            continue;
                        }
                        if (!ok) {
                            String sortNr = parms.getParameter("_sortnr-" + path);
                            failed.put(sortNr, condition);
                        }
                    }
                }
            }
        }
    }

    private boolean checkCondition(Element condition, String name, String value) {
        boolean required = "true".equals(condition.getAttributeValue("required"));

        if (required) {
            if (name.endsWith("]") && ((value == null) || (value.trim().length() == 0))) {
                return true; // repeated field is required but missing, this
                // is
            }
            // ok
            else if (!MCRInputValidator.instance().validateRequired(value)) {
                return false; // field is required but empty, this is an error
            }
        } else if ((!required) && (value.trim().length() == 0)) {
            return true; // field is not required and empty, this is OK
        }

        String type = condition.getAttributeValue("type");
        String min = condition.getAttributeValue("min");
        String max = condition.getAttributeValue("max");
        String format = condition.getAttributeValue("format");

        if ((type != null) && !MCRInputValidator.instance().validateMinMaxType(value, type, min, max, format)) {
            return false; // field type, data format and/or min max value is
            // illegal
        }

        String minLength = condition.getAttributeValue("minLength");
        String maxLength = condition.getAttributeValue("maxLength");

        if (((maxLength != null) || (minLength != null)) && !MCRInputValidator.instance().validateLength(value, minLength, maxLength)) {
            return false; // field min/max length is illegal
        }

        String regexp = condition.getAttributeValue("regexp");

        if ((regexp != null) && !MCRInputValidator.instance().validateRegularExpression(value, regexp)) {
            return false; // field does not match given regular expression
        }

        String xsl = condition.getAttributeValue("xsl");

        if ((xsl != null) && !MCRInputValidator.instance().validateXSLCondition(value, xsl)) {
            return false; // field does not match given xsl condition
        }

        String clazz = condition.getAttributeValue("class");
        String method = condition.getAttributeValue("method");

        if ((clazz != null) && (method != null) && !MCRInputValidator.instance().validateExternally(clazz, method, value)) {
            return false; // field does not validate using external method
        }

        return true;
    }

    private void addVariable(String path, String text) {
        if ((text == null) || (text.trim().length() == 0)) {
            return;
        }

        MCREditorServlet.logger.debug("Editor variable " + path + "=" + text);
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

        return (FileItem) (node2file.get(xmlNode));
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

    Element buildInputElements() {
        Element input = new Element("input");

        for (int i = 0; i < variables.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (variables.get(i));
            input.addContent(var.asInputElement());
        }

        return input;
    }

    Element buildRepeatElements() {
        Element eRepeats = new Element("repeats");

        for (int i = 0; i < repeats.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (repeats.get(i));
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
            String sortNr = (String) (e.nextElement());
            Element condition = (Element) (failed.get(sortNr));
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
        MCREditorVariable first = (MCREditorVariable) (variables.get(0));
        Element root = new Element(first.getPathElements()[0]);

        for (int i = 0; i < variables.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (variables.get(i));

            Element parent = root;
            String[] elements = var.getPathElements();

            for (int j = 1; j < elements.length; j++) {
                String name = elements[j];

                if (name.endsWith("]")) {
                    int pos = name.lastIndexOf("[");
                    name = name.substring(0, pos) + "_XXX_" + name.substring(pos + 1, name.length() - 1);
                }

                Element child = parent.getChild(name);

                if (child == null) {
                    child = new Element(name);
                    parent.addContent(child);
                }

                parent = child;
            }

            Object node;

            if (!var.isAttribute()) {
                parent.addContent(var.getValue());
                node = parent;
            } else {
                parent.setAttribute(var.getAttributeName(), var.getValue());
                node = parent.getAttribute(var.getAttributeName());
            }

            FileItem file = parms.getFileItem(var.getPath());

            if (file != null) {
                file2node.put(file, node);
                node2file.put(node, file);
            }
        }

        renameRepeatedElements(root);
        xml = new Document(root);
    }

    private void renameRepeatedElements(Element element) {
        String name = element.getName();
        int pos = name.lastIndexOf("_XXX_");

        if (pos >= 0) {
            element.setName(name.substring(0, pos));
        }

        pos = name.indexOf("__");
        if (pos > 0) {
            element.setName(name.substring(0, pos));
            int pos2 = name.indexOf("__", pos + 2);
            String attr = name.substring(pos + 2, pos2);
            String val = name.substring(pos2 + 2);
            element.setAttribute(attr, val);
        }

        List children = element.getChildren();

        for (int i = 0; i < children.size(); i++)
            renameRepeatedElements((Element) (children.get(i)));
    }

    private void setRepeatsFromVariables() {
        Hashtable maxtable = new Hashtable();

        for (int i = 0; i < variables.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (variables.get(i));
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
                        int numOld = Integer.parseInt((String) (maxtable.get(key)));
                        maxtable.remove(key);
                        numNew = Math.max(numOld, numNew);
                    }

                    maxtable.put(key, String.valueOf(numNew));
                }

                prefix = prefix + "/" + name;
            }
        }

        for (Enumeration e = maxtable.keys(); e.hasMoreElements();) {
            String path = (String) (e.nextElement());
            String value = (String) (maxtable.get(path));

            repeats.add(new MCREditorVariable(path, value));
            MCREditorServlet.logger.debug("Editor repeats " + path + " = " + value);
        }
    }

    private void setRepeatsFromSubmission() {
        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String parameter = (String) (e.nextElement());

            if (parameter.startsWith("_n-")) {
                String value = parms.getParameter(parameter);
                repeats.add(new MCREditorVariable(parameter.substring(3), value));
                MCREditorServlet.logger.debug("Editor repeats " + parameter.substring(3) + " = " + value);
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
            String path = ((MCREditorVariable) (variables.get(i))).getPath();

            if (path.startsWith(prefix2 + "/") || path.equals(prefix2)) {
                variables.remove(i--);
            }
        }

        for (int i = 0; i < repeats.size(); i++) {
            String path = ((MCREditorVariable) (repeats.get(i))).getPath();

            if (path.startsWith(prefix2 + "/")) {
                repeats.remove(i--);
            }
        }

        changeVariablesAndRepeats(prefix, nr, -1);
    }

    void doUp(String prefix, int nr) {
        String prefix1 = prefix + ((nr > 2) ? ("[" + String.valueOf(nr - 1) + "]") : "");
        String prefix2 = prefix + "[" + String.valueOf(nr) + "]";

        for (int i = 0; i < variables.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (variables.get(i));
            String path = var.getPath();

            if (path.startsWith(prefix1 + "/") || path.equals(prefix1)) {
                String rest = path.substring(prefix1.length());
                var.setPath(prefix2 + rest);
            } else if (path.startsWith(prefix2) || path.equals(prefix2)) {
                String rest = path.substring(prefix2.length());
                var.setPath(prefix1 + rest);
            }
        }

        for (int i = 0; i < repeats.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (repeats.get(i));
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
        for (int i = 0; i < repeats.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (repeats.get(i));

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

        for (int i = 0; i < list.size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (list.get(i));
            String path = var.getPath();

            if (!path.startsWith(prefix + "[")) {
                continue;
            }

            String rest = path.substring(prefix.length() + 1);

            int pos = rest.indexOf("]");
            int num = Integer.parseInt(rest.substring(0, pos));

            if (num > nr) {
                num += change;

                StringBuffer newpath = new StringBuffer(prefix);

                if (num > 1) {
                    newpath.append("[").append(num).append("]");
                }

                newpath.append(rest.substring(pos + 1));

                var.setPath(newpath.toString());
            }
        }
    }
}
