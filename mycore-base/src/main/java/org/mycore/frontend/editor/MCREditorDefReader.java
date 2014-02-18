/*
 * $Revision$ 
 * $Date$
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserFactory;

/*
 * Reads in definition of editor forms like search mask and data input forms.
 * Resolves includes and prepares editor form for output. 
 */
public class MCREditorDefReader {
    private final static Logger LOGGER = Logger.getLogger(MCREditorDefReader.class);

    private Element editor;

    HashMap<String, Element> id2component = new LinkedHashMap<String, Element>();

    HashMap<Element, String> referencing2ref = new LinkedHashMap<Element, String>();

    private MCRTokenSubstitutor tokenSubstitutor;

    /**
     * Reads the editor definition from the given URI
     * 
     * @param validate
     *            if true, validate editor definition against schema
     * @param parameters
     *            http request parameters           
     */
    MCREditorDefReader(String uri, String ref, boolean validate, MCRParameters parameters) {
        long time = System.nanoTime();
        this.tokenSubstitutor = new MCRTokenSubstitutor(parameters);

        Element include = new Element("include").setAttribute("uri", uri);
        if (ref != null && ref.length() > 0) {
            include.setAttribute("ref", ref);
        }

        editor = new Element("editor");
        editor.setAttribute("id", ref);
        editor.addContent(include);
        resolveIncludes(editor);
        checkDuplicateIDs(editor);
        resolveReferences();
        if (validate) {
            validate(uri, ref);
        }

        time = (System.nanoTime() - time) / 1000000;
        LOGGER.info("Finished reading editor definition in " + time + " ms");
    }

    private void checkDuplicateIDs(Element editor) {
        Set<String> ids = new HashSet<String>();
        Iterator<Element> elements = editor.getDescendants(Filters.element());
        while (elements.hasNext()) {
            String id = elements.next().getAttributeValue("id");
            if (id == null || id.trim().length() == 0) {
                continue;
            }
            if (ids.contains(id)) {
                String msg = "Duplicate ID '" + id + "', already used in editor definition";
                throw new MCRConfigurationException(msg);
            } else {
                ids.add(id);
            }
        }
    }

    private void validate(String uri, String ref) {
        if (ref != null && ref.length() > 0) {
            uri += "#" + ref;
        }
        LOGGER.info("Validating editor " + uri + "...");

        Document doc = new Document(editor);
        editor.setAttribute("noNamespaceSchemaLocation", "editor.xsd", MCRConstants.XSI_NAMESPACE);

        try {
            MCRXMLParserFactory.getValidatingParser().parseXML(new MCRJDOMContent(doc));
        } catch (Exception ex) {
            String msg = "Error validating editor " + uri;
            LOGGER.error(msg);
            throw new MCRConfigurationException(msg, ex);
        }

        editor.detach();
        editor.removeAttribute("noNamespaceSchemaLocation", MCRConstants.XSI_NAMESPACE);
        LOGGER.info("Validation succeeded.");
    }

    /**
     * Returns the complete editor with all references resolved 
     */
    Element getEditor() {
        return editor;
    }

    /**
     * Recursively removes include elements that are direct or indirect children
     * of the given container element and replaces them with the included
     * resource. Includes that may be contained in included resources are
     * recursively resolved, too.
     * 
     * @param element
     *            The element where to start resolving includes
     */
    private boolean resolveIncludes(Element element) {
        boolean replaced = false;

        String ref = element.getAttributeValue("ref", "");
        ref = tokenSubstitutor.substituteTokens(ref);
        
        if (element.getName().equals("include")) {
            String uri = element.getAttributeValue("uri");
            if (uri != null) {
                uri = tokenSubstitutor.substituteTokens(uri);
                LOGGER.info("Including " + uri + (ref.length() > 0 ? "#" + ref : ""));
                Element parent = element.getParentElement();
                int pos = parent.indexOf(element);

                Element container = MCRURIResolver.instance().resolve(uri);
                List<Content> found;

                if (ref.length() == 0) {
                    found = container.cloneContent();
                } else {
                    found = findContent(container, ref);
                    ref = "";
                }
                replaced = true;
                parent.addContent(pos, found);
                element.detach();
            }
        } else {
            String id = element.getAttributeValue("id", "");
            if (id.length() > 0) {
                id2component.put(id, element);
            }

            setDefaultAttributes(element);
            resolveChildren(element);
        }

        if (ref.length() > 0) {
            referencing2ref.put(element, ref);
        }
        return replaced;
    }

    private void resolveChildren(Element parent) {
        for (int i = 0; i < parent.getContentSize(); i++) {
            Content child = parent.getContent(i);
            if (child instanceof Element && resolveIncludes((Element) child)) {
                i--;
            }
        }
    }

    private List<Content> findContent(Element candidate, String id) {
        if (id.equals(candidate.getAttributeValue("id"))) {
            return candidate.cloneContent();
        } else {
            for (Element child : (List<Element>) candidate.getChildren()) {
                List<Content> found = findContent(child, id);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
    }

    /**
     * Returns that direct or indirect child element of the given element, thats
     * ID attribute has the given value.
     * 
     * @param id
     *            the value the ID attribute must have
     * @param candidate
     *            the element to start searching with
     * @return the element below that has the given ID, or null if no such
     *         element exists.
     */
    public static Element findElementByID(String id, Element candidate) {
        if (id.equals(candidate.getAttributeValue("id"))) {
            return candidate;
        } else {
            for (Element child : (List<Element>) candidate.getChildren()) {
                Element found = findElementByID(id, child);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
    }

    /**
     * Recursively resolves references by the @ref attribute and
     * replaces them with the referenced component.
     */
    private void resolveReferences() {
        for (Iterator<Element> it = referencing2ref.keySet().iterator(); it.hasNext();) {
            Element referencing = it.next();
            String id = referencing2ref.get(referencing);
            LOGGER.debug("Resolving reference to " + id);

            Element found = id2component.get(id);
            if (found == null) {
                String msg = "Reference to component " + id + " could not be resolved";
                throw new MCRConfigurationException(msg);
            }

            String name = referencing.getName();
            referencing.removeAttribute("ref");
            it.remove();

            if (name.equals("cell") || name.equals("repeater")) {
                if (found.getParentElement().getName().equals("components")) {
                    referencing.addContent(0, found.detach());
                } else {
                    referencing.addContent(0, (Element) found.clone());
                }
            } else if (name.equals("panel")) {
                if (referencing2ref.containsValue(id)) {
                    referencing.addContent(0, found.cloneContent());
                } else {
                    found.detach();
                    List<Content> content = found.getContent();
                    for (int i = 0; !content.isEmpty(); i++) {
                        Content child = content.remove(0);
                        referencing.addContent(i, child);
                    }
                }
            } else if (name.equals("include")) {
                Element parent = referencing.getParentElement();
                int pos = parent.indexOf(referencing);
                referencing.detach();

                if (referencing2ref.containsValue(id)) {
                    parent.addContent(pos, found.cloneContent());
                } else {
                    found.detach();
                    List<Content> content = found.getContent();
                    for (int i = pos; !content.isEmpty(); i++) {
                        Content child = content.remove(0);
                        parent.addContent(i, child);
                    }
                }
            }
        }

        Element components = editor.getChild("components");
        String root = components.getAttributeValue("root");

        for (int i = 0; i < components.getContentSize(); i++) {
            Content child = components.getContent(i);
            if (!(child instanceof Element)) {
                continue;
            }
            if (((Element) child).getName().equals("headline")) {
                continue;
            }
            if (!root.equals(((Element) child).getAttributeValue("id"))) {
                components.removeContent(i--);
            }
        }
    }

    /**
     * This map contains default attribute values to set for a given element name
     */
    private static HashMap<String, Properties> defaultAttributes = new HashMap<String, Properties>();

    static {
        defaultAttributes.put("cell", new Properties());
        defaultAttributes.get("cell").setProperty("row", "1");
        defaultAttributes.get("cell").setProperty("col", "1");
        defaultAttributes.get("cell").setProperty("class", "editorCell");
        defaultAttributes.put("headline", new Properties());
        defaultAttributes.get("headline").setProperty("class", "editorHeadline");
        defaultAttributes.put("repeater", new Properties());
        defaultAttributes.get("repeater").setProperty("class", "editorRepeater");
        defaultAttributes.get("repeater").setProperty("min", "1");
        defaultAttributes.get("repeater").setProperty("max", "10");
        defaultAttributes.put("panel", new Properties());
        defaultAttributes.get("panel").setProperty("class", "editorPanel");
        defaultAttributes.put("editor", new Properties());
        defaultAttributes.get("editor").setProperty("class", "editor");
        defaultAttributes.put("helpPopup", new Properties());
        defaultAttributes.get("helpPopup").setProperty("class", "editorButton");
        defaultAttributes.put("text", new Properties());
        defaultAttributes.get("text").setProperty("class", "editorText");
        defaultAttributes.put("textfield", new Properties());
        defaultAttributes.get("textfield").setProperty("class", "editorTextfield");
        defaultAttributes.put("textarea", new Properties());
        defaultAttributes.get("textarea").setProperty("class", "editorTextarea");
        defaultAttributes.put("file", new Properties());
        defaultAttributes.get("file").setProperty("class", "editorFile");
        defaultAttributes.put("password", new Properties());
        defaultAttributes.get("password").setProperty("class", "editorPassword");
        defaultAttributes.put("subselect", new Properties());
        defaultAttributes.get("subselect").setProperty("class", "editorButton");
        defaultAttributes.put("submitButton", new Properties());
        defaultAttributes.get("submitButton").setProperty("class", "editorButton");
        defaultAttributes.put("cancelButton", new Properties());
        defaultAttributes.get("cancelButton").setProperty("class", "editorButton");
        defaultAttributes.put("button", new Properties());
        defaultAttributes.get("button").setProperty("class", "editorButton");
        defaultAttributes.put("list", new Properties());
        defaultAttributes.get("list").setProperty("class", "editorList");
        defaultAttributes.put("checkbox", new Properties());
        defaultAttributes.get("checkbox").setProperty("class", "editorCheckbox");
    }

    /**
     * Sets default attribute values for the given element, if any
     */
    private void setDefaultAttributes(Element element) {
        Properties defaults = defaultAttributes.get(element.getName());
        if (defaults == null) {
            return;
        }
        for (Object element2 : defaults.keySet()) {
            String key = (String) element2;
            if (element.getAttribute(key) == null) {
                element.setAttribute(key, defaults.getProperty(key));
            }
        }
    }

    /** 
     * Transforms @var attribute values that have a condition like
     * title[@type='main'] into escaped internal syntax
     * title__type__main 
     */
    static void fixConditionedVariables(Element element) {
        String var = element.getAttributeValue("var", "");
        int beginOfPredicate = var.indexOf("[@");
        while (beginOfPredicate != -1) {
            String predicate = MCREditorSubmission.escapePredicate(var, beginOfPredicate);
            int endOfPredicate = var.indexOf("]", beginOfPredicate);
            var = var.substring(0, beginOfPredicate) + predicate + var.substring(endOfPredicate + 1);
            element.setAttribute("var", var);
            beginOfPredicate = var.indexOf("[@", endOfPredicate);
        }

        for (Element child : (List<Element>) (element.getChildren()))
            fixConditionedVariables(child);
    }
}
