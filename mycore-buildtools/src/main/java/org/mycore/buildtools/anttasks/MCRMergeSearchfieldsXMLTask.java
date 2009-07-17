/*
 * $RCSfile: MergeProperties.java,v $
 * $Revision: 1.2 $ $Date: 2006/11/22 10:50:21 $
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
package org.mycore.buildtools.anttasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Ant task, that can be used to merges one searchfield.xml file into another.
 * New indexes will be added. It depends on the boolean flag overwrite, if the
 * paths and object types for existing indexes will be overwritten or
 * concatenated .
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Mi, 06 Feb
 *          2008) $
 * 
 */
public class MCRMergeSearchfieldsXMLTask extends Task {
	private String base, delta, out;
	private boolean overwrite = false;

	/**
	 * set the base searchfield.xml file
	 * 
	 * @param f
	 *            the base searchfield.xml file
	 */
	public void setBasefile(String f) {
		base = f;
		out = f;
	}

	/**
	 * set the delta searchfield.xml file
	 * 
	 * @param f
	 *            the delta searchfield.xml file
	 */
	public void setDeltafile(String f) {
		delta = f;
	}

	/**
	 * sets the overwrite flag when set exisiting object type and path values
	 * will be overwritten
	 * 
	 * @param b
	 */
	public void setOverwrite(boolean b) {
		overwrite = b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document docBase = builder.parse(new InputSource(
					new InputStreamReader(new FileInputStream(new File(base)),
							"UTF-8")));
			docBase.normalize();

			Document docDelta = builder.parse(new InputSource(
					new InputStreamReader(new FileInputStream(new File(delta)),
							"UTF-8")));
			docDelta.normalize();

			merge(docBase, docDelta);
			docBase.normalize();

			// output
			// TransformerFactory tFactory = TransformerFactory.newInstance();
			// Transformer transformer = tFactory.newTransformer();
			// transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			// transformer.setOutputProperty(OutputKeys.METHOD,"xml");
			// transformer.setOutputProperty(OutputKeys.INDENT, "true");
			// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
			// "4");
			// DOMSource source = new DOMSource(docBase);
			// StreamResult result = new StreamResult(new FileOutputStream(new
			// File(out)));
			// transformer.transform(source, result);

			// alternative code for output (with "pretty print")
			OutputFormat format = new OutputFormat(docBase);
			format.setLineWidth(360);
			format.setIndenting(true);
			format.setIndent(4);
			XMLSerializer serializer = new XMLSerializer(
					new OutputStreamWriter(new FileOutputStream(new File(out)),
							"UTF-8"), format);
			serializer.serialize(docBase);

		} catch (Exception e) {
			throw new BuildException(
					"Something went wrong while merging searchfields!", e);
		}
	}

	/**
	 * simple test case for the class
	 * @param args the default arguments for main method (unused)
	 */
	public static void main(String[] args) {
		String basefile = "C:\\mycore\\modules\\buildtools\\test\\searchfields\\searchfields.org.xml";
		String deltafile = "C:\\mycore\\modules\\buildtools\\test\\searchfields\\othersearchfields.xml";
		String outfile = "C:\\mycore\\modules\\buildtools\\test\\searchfields\\searchfields.xml";

		// copy the file;
		try {
			FileChannel ic = new FileInputStream(basefile).getChannel();
			FileChannel oc = new FileOutputStream(outfile).getChannel();
			ic.transferTo(0, ic.size(), oc);
			ic.close();
			oc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		MCRMergeSearchfieldsXMLTask mx = new MCRMergeSearchfieldsXMLTask();
		mx.setBasefile(outfile);
		mx.setDeltafile(deltafile);

		mx.execute();
		System.out.println("OK");

	}

	private void merge(Document docBase, Document docDelta) {
		NodeList nlDeltaElemIndex = docDelta.getDocumentElement()
				.getElementsByTagName("index");
		for (int i = 0; i < nlDeltaElemIndex.getLength(); i++) {
			Element eDeltaIndex = (Element) nlDeltaElemIndex.item(i);
			// System.out.println(eDeltaIndex.getAttribute("id"));
			Element eBaseIndex = findIndexElement(eDeltaIndex
					.getAttribute("id"), docBase);
			mergeIndexElements(eBaseIndex, eDeltaIndex);
		}
	}

	private Element findIndexElement(String id, Document doc) {
		NodeList nlElemIndex = doc.getDocumentElement().getElementsByTagName(
				"index");
		for (int i = 0; i < nlElemIndex.getLength(); i++) {
			Element elem = (Element) nlElemIndex.item(i);
			if (elem.getAttribute("id").equals(id)) {
				return elem;
			}
		}
		// Element has not been found -> create index element
		Element index = doc.createElement("index");
		index.setAttribute("id", id);
		doc.getDocumentElement().appendChild(index);
		return index;
	}

	private void mergeIndexElements(Element eBaseIndex, Element eDeltaIndex) {
		NodeList nlDeltaElemField = eDeltaIndex.getElementsByTagName("field");
		for (int i = 0; i < nlDeltaElemField.getLength(); i++) {
			Element eDeltaField = (Element) nlDeltaElemField.item(i);
			// System.out.println("+--- "+ eDeltaField.getAttribute("name"));
			Element eBaseField = findFieldElement(eDeltaField
					.getAttribute("name"), eBaseIndex);
			if (overwrite) {
				overwriteField(eBaseField, eDeltaField);
			} else {
				mergeField(eBaseField, eDeltaField);
			}
		}
	}

	private Element findFieldElement(String name, Element eIndex) {
		NodeList nlElemField = eIndex.getElementsByTagName("field");
		for (int i = 0; i < nlElemField.getLength(); i++) {
			Element elem = (Element) nlElemField.item(i);
			if (elem.getAttribute("name").equals(name)) {
				return elem;
			}
		}
		// Element has not been found -> create index element
		Element field = eIndex.getOwnerDocument().createElement("field");
		field.setAttribute("name", name);
		eIndex.appendChild(field);
		return field;
	}

	private void mergeField(Element eBaseField, Element eDeltaField) {
		// attributes source and type must be equal
		if (!eBaseField.getAttribute("source").equals("")
				&& !eDeltaField.getAttribute("source").equals("")
				&& !eBaseField.getAttribute("source").equals(
						eDeltaField.getAttribute("source"))) {
			throw new BuildException("Attributes source of field "
					+ eBaseField.getAttribute("name")
					+ " must be equal to be merged!" + "\n   base file : "
					+ base + "\n   delta file: " + delta);
		}
		if (!eBaseField.getAttribute("type").equals("")
				&& !eDeltaField.getAttribute("type").equals("")
				&& !eBaseField.getAttribute("type").equals(
						eDeltaField.getAttribute("type"))) {
			throw new BuildException("Attributes type of field "
					+ eBaseField.getAttribute("name")
					+ " must be equal to be merged!" + "\n   base file : "
					+ base + "\n   delta file: " + delta);
		}
		// attribute objects (separated by space)
		if (!eDeltaField.getAttribute("objects").equals("")) {
			if (eBaseField.getAttribute("objects").trim().length() == 0) {
				eBaseField.setAttribute("objects", eDeltaField
						.getAttribute("objects"));
			} else {
				String data = (eBaseField.getAttribute("objects") + " " + eDeltaField
						.getAttribute("objects"));
				Set<String> set = new HashSet<String>(Arrays.asList(data
						.split("\\s+")));
				StringBuffer result = new StringBuffer();
				for (String s : set) {
					result.append(" ").append(s.trim());
				}
				eBaseField.setAttribute("objects", result.substring(1));
			}
		}
		// attribute: value (separated by "|")
		if (!eDeltaField.getAttribute("value").equals("")) {
			if (eBaseField.getAttribute("value").trim().length() == 0) {
				eBaseField.setAttribute("value", eDeltaField
						.getAttribute("value"));
			} else {
				String data = (eBaseField.getAttribute("value") + " | " + eDeltaField
						.getAttribute("value"));
				Set<String> set = new HashSet<String>(Arrays.asList(data
						.split("\\s*\\|\\s*")));
				StringBuffer result = new StringBuffer();
				for (String s : set) {
					result.append(" | ").append(s.trim());
				}
				eBaseField.setAttribute("value", result.substring(3));
			}
		}
		// attribute: xpath (separated by "|")
		if (!eDeltaField.getAttribute("xpath").equals("")) {
			if (eBaseField.getAttribute("xpath").trim().length() == 0) {
				eBaseField.setAttribute("xpath", eDeltaField
						.getAttribute("xpath"));
			} else {
				String data = (eBaseField.getAttribute("xpath") + " | " + eDeltaField
						.getAttribute("xpath"));
				Set<String> set = new HashSet<String>(Arrays.asList(data
						.split("\\s*\\|\\s*")));
				StringBuffer result = new StringBuffer();
				for (String s : set) {
					result.append(" | ").append(s.trim());
				}
				eBaseField.setAttribute("xpath", result.substring(3));
			}
		}

		// attributes: sortable, classification, i18n (will be overwritten)
		if (!eDeltaField.getAttribute("sortable").equals("")) {
			eBaseField.setAttribute("sortable", eDeltaField
					.getAttribute("sortable"));
		}
		if (!eDeltaField.getAttribute("i18n").equals("")) {
			eBaseField.setAttribute("i18n", eDeltaField.getAttribute("i18n"));
		}
		if (!eDeltaField.getAttribute("classification").equals("")) {
			eBaseField.setAttribute("classification", eDeltaField
					.getAttribute("classification"));
		}
	}

	private void overwriteField(Element eBaseField, Element eDeltaField) {
		Node newField = eBaseField.getOwnerDocument().importNode(eDeltaField,
				true);
		eBaseField.getParentNode().replaceChild(newField, eBaseField);
	}
}
