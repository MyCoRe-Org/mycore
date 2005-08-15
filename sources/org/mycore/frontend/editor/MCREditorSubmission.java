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

package org.mycore.frontend.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Container class that holds all data and files edited and submitted from an
 * HTML page that contains a MyCoRe XML editor form.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCREditorSubmission {
	private List variables = new ArrayList();

	private List repeats = new ArrayList();

	private List files = new ArrayList();

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

	MCREditorSubmission(MCRRequestParameters parms, Element editor) {
		this.parms = parms;
		setVariablesFromSubmission(parms, editor);
		Collections.sort(variables);
		setRepeatsFromSubmission();
	}

	MCREditorSubmission(Element saved, List submitted, String root,
			String varpath) {
		Element input = saved.getChild("input");
		List children = input.getChildren();
		for (int i = 0; i < children.size(); i++) {
			Element var = (Element) (children.get(i));
			String path = var.getAttributeValue("name");
			String value = var.getAttributeValue("value");

			if (path.equals(varpath) || path.startsWith(varpath + "/"))
				continue;
			else
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

	private void setVariablesFromElement(Element element, String prefix,
			String suffix) {
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
				if (child.getName().equals(before.getName()))
					suffix = "[" + String.valueOf(++nr) + "]";
				else
					nr = 1;
			}
			setVariablesFromElement(child, path + "/", suffix);
		}
	}

	private void setVariablesFromSubmission(MCRRequestParameters parms,
			Element editor) {
		for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
			String name = (String) (e.nextElement());
			if (name.startsWith("_"))
				continue; // Skip internal request params

			String[] values = parms.getParameterValues(name);
			String sortNr = parms.getParameter("_sortnr-" + name);
			String ID = parms.getParameter("_id@" + name);

			// Skip files that should be deleted
			String delete = parms.getParameter("_delete-" + name);
			if ("true".equals(delete) && (parms.getFileItem(name) == null))
				continue;

			// Skip request params that are not input but target params
			if (sortNr == null)
				continue;

			// For each value
			for (int k = 0; (values != null) && (k < values.length); k++) {
				String value = values[k];
				if ((value == null) || (value.trim().length() == 0))
					continue;

				// Handle multiple variables with same name: checkboxes & select
				// multiple
				String nname = (k == 0 ? name : name + "[" + (k + 1) + "]");

				MCREditorVariable var = new MCREditorVariable(nname, value);
				var.setSortNr(sortNr);

				// Add associated component from editor definition
				if ((ID != null) && (ID.trim().length() > 0)) {
					Element component = MCREditorDefReader.findElementByID(ID,
							editor);
					if (component != null) {
						// Skip variables with values equal to autofill text
						String attrib = component.getAttributeValue("autofill");
						String elem = component.getChildTextTrim("autofill");
						String autofill = null;

						if ((attrib != null) && (attrib.trim().length() > 0))
							autofill = attrib.trim();
						else if ((attrib != null)
								&& (attrib.trim().length() > 0))
							autofill = elem.trim();

						if (value.trim().equals(autofill))
							continue;
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

	private void addVariable(String path, String text) {
		if ((text == null) || (text.trim().length() == 0))
			return;

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
		if (xml == null)
			buildTargetXML();
		return (FileItem) (node2file.get(xmlNode));
	}

	public Object getXMLNode(FileItem file) {
		if (xml == null)
			buildTargetXML();
		return file2node.get(file);
	}

	public Document getXML() {
		if (xml == null)
			buildTargetXML();
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
					name = name.substring(0, pos) + "_XXX_"
							+ name.substring(pos + 1, name.length() - 1);
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

		if (pos >= 0)
			element.setName(name.substring(0, pos));

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
						int numOld = Integer.parseInt((String) (maxtable
								.get(key)));
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
			MCREditorServlet.logger.debug("Editor repeats " + path + " = "
					+ value);
		}
	}

	private void setRepeatsFromSubmission() {
		for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
			String parameter = (String) (e.nextElement());
			if (parameter.startsWith("_n-")) {
				String value = parms.getParameter(parameter);
				repeats
						.add(new MCREditorVariable(parameter.substring(3),
								value));
				MCREditorServlet.logger.debug("Editor repeats "
						+ parameter.substring(3) + " = " + value);
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
		if (nr > 1)
			prefix2 = prefix + "[" + nr + "]";
		else
			prefix2 = prefix;

		for (int i = 0; i < variables.size(); i++) {
			String path = ((MCREditorVariable) (variables.get(i))).getPath();

			if (path.startsWith(prefix2 + "/") || path.equals(prefix2))
				variables.remove(i--);
		}

		for (int i = 0; i < repeats.size(); i++) {
			String path = ((MCREditorVariable) (repeats.get(i))).getPath();

			if (path.startsWith(prefix2 + "/"))
				repeats.remove(i--);
		}

		changeVariablesAndRepeats(prefix, nr, -1);
	}

	void doUp(String prefix, int nr) {
		String prefix1 = prefix
				+ (nr > 2 ? "[" + String.valueOf(nr - 1) + "]" : "");
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
				if (value == 0)
					value = 1;
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

			if (!path.startsWith(prefix + "["))
				continue;

			String rest = path.substring(prefix.length() + 1);

			int pos = rest.indexOf("]");
			int num = Integer.parseInt(rest.substring(0, pos));

			if (num > nr) {
				num += change;

				StringBuffer newpath = new StringBuffer(prefix);
				if (num > 1)
					newpath.append("[").append(num).append("]");
				newpath.append(rest.substring(pos + 1));

				var.setPath(newpath.toString());
			}
		}
	}
}
