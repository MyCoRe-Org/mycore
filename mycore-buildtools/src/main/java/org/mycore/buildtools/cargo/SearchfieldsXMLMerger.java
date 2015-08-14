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
package org.mycore.buildtools.cargo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.codehaus.cargo.module.merge.MergeException;
import org.codehaus.cargo.module.merge.MergeProcessor;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * This class implements a merger for MyCoRe's searchfields.xml.
 * It will be used for the goal cargo:uberwar of the Cargo Maven2 Plugin
 * (http://cargo.codehaus.org/Maven2+plugin)
 * where different WAR files are merged together.
 * 
 * <br>
 * Use the following configuration in merge.xml:
 * <pre>
 * &lt;merge&gt;
 *   &lt;document&gt;WEB-INF/classes/searchfields.xml&lt;/document&gt;
 *   &lt;classname&gt;org.mycore.buildtools.cargo.SearchfieldsXMLMerger&lt;/classname&gt;
 * &lt;/merge&gt;
 * </pre>
 * 
 * @see MergeProcessor
 * 
 * @author Robert Stephan
 */
public class SearchfieldsXMLMerger implements MergeProcessor {
	public static final String MODE_MERGE = "merge";
	public static final String MODE_REPLACE = "replace";
	public static final String MODE_DELETE = "delete";
	private ArrayList<Document> searchfieldFileList;

	public SearchfieldsXMLMerger() {
		searchfieldFileList = new ArrayList<Document>();
	}

	/**
	 * adds another searchfield.xml as org.jdom2.Document
	 */
	public void addMergeItem(Object o) throws MergeException {
		if(o instanceof ByteArrayInputStream){
			ByteArrayInputStream bais = (ByteArrayInputStream) o;
			try{
				BufferedReader br = new BufferedReader(new InputStreamReader(bais, "UTF-8"));
				SAXBuilder sb = new SAXBuilder();
				//sb.setIgnoringElementContentWhitespace(false);
				Document doc = sb.build(br);
				searchfieldFileList.add(doc);
			}
			catch(Exception e){
				throw new MergeException("Error reading searchfields.xml", e);
			}		
		}
		else{
			throw new MergeException("Please specify a <file> subelement which contains the searchfields.xml");
		}
	}

	@Override
	/**
	 * does the merging
	 * Either indexes or fields can be merged.
	 * For each item the mode can be specified by adding an attribute "buildaction"
	 * which can have one of the values: "merge" (default), "delete", "replace"
	 * Items marked as "delete" will be removed
	 * Items marked as "replace" will be complete replaced with the new version
	 * Items marked as "merge" will be merged
	 * 
	 * How are the attributes of &lt;field&gt; elements merged?
	 * <ul><li>name: will be used as identifier for the field</li>
	 *     <li>objects: will be joined by space (" "), doubled entries will be removed</li>
	 *     <li>xpath, value: the XPath-Expressions are joined by OR ("|"), doubled entries will be removed</li>
	 *     <li>type source i18n classification: merging their values makes no sense, they will be replaced</li>
	 * </ul>
	 * 
	 * @return org.jdom2.Document the new (merged) searchfields
	 */
	
	public Object performMerge() throws MergeException {
		try{
			if (searchfieldFileList.size() == 0) {
				return null;
			}
			Document baseDoc =searchfieldFileList.get(0);
			Document returnDoc = new Document();
			returnDoc.addContent((Element)baseDoc.getRootElement().clone());
			for (int i = 1; i < this.searchfieldFileList.size(); i++) {
				Document deltaDoc = searchfieldFileList.get(i);
				mergeSearchfields(returnDoc, deltaDoc);
			}
			
			XMLOutputter xmlOut = new XMLOutputter(Format.getRawFormat());
			StringWriter sw = new StringWriter();
			xmlOut.output(returnDoc, sw);
			return new ByteArrayInputStream(sw.toString().getBytes("UTF-8"));
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * looks for &lt;index&gt; elements to be merged (which would have the same "id" attribute
	 * @param baseDoc the base searchfields.xml
	 * @param deltaDoc the searchfields.xml containing the information to be merged
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void mergeSearchfields(Document baseDoc, Document deltaDoc) throws IOException{
		Hashtable<String, Element> baseIndexe = new Hashtable<String, Element>();
		List<Element> l = (List<Element>)baseDoc.getRootElement().getChildren();
		for(Element e: l){
			baseIndexe.put(e.getAttributeValue("id"), e);
		}
		l = (List<Element>)deltaDoc.getRootElement().getChildren();
		for(Element eDeltaIndex: l){
			String mode = eDeltaIndex.getAttributeValue("buildaction");
			if(mode==null){mode=MODE_MERGE;}
			String id = eDeltaIndex.getAttributeValue("id");
			if(baseIndexe.containsKey(id)){
				mergeIndex(baseDoc, baseIndexe.get(id), eDeltaIndex, mode);
			}
			else{
				if(MODE_MERGE.equals(mode) || MODE_REPLACE.equals(mode)){
					baseDoc.getRootElement().addContent((Element)eDeltaIndex.clone());
				}
			}
		}
	}
	
	/**
	 * merges two &lt;index<&gt; elements
	 * @param baseDoc - the searchfields.xml
	 * @param eBaseIndex - the base index field
	 * @param eDeltaIndex - the index field containing the merge information
	 * @param mode - the merge mode ("delete", "replace" "merge")
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void mergeIndex(Document baseDoc, Element eBaseIndex, Element eDeltaIndex, String mode) throws IOException{
		if(MODE_DELETE.equals(mode)){
			baseDoc.getRootElement().removeContent(eBaseIndex);
			return;
		}
		if(MODE_REPLACE.equals(mode)){
			baseDoc.getRootElement().removeContent(eBaseIndex);
			Element e = (Element)eDeltaIndex.clone();
			e.removeAttribute("buildaction");
			baseDoc.getRootElement().addContent(e);
			return;
		}
		if(MODE_MERGE.equals(mode)){
			Hashtable<String, Element> baseFields = new Hashtable<String, Element>();
			List<Element> l = (List<Element>)eBaseIndex.getChildren();
			for(Element e: l){
				baseFields.put(e.getAttributeValue("name"), e);
			}
			l = (List<Element>)eDeltaIndex.getChildren();
			for(Element eDeltaField: l){
				String name = eDeltaField.getAttributeValue("name");
				System.out.println(name);
				String fieldMode = eDeltaField.getAttributeValue("buildaction");
				if(fieldMode==null){fieldMode=MODE_MERGE;}
				if(baseFields.containsKey(name)){
					mergeFields(eBaseIndex, baseFields.get(name), eDeltaField, fieldMode);
				}
				else{
					if(MODE_MERGE.equals(fieldMode) || MODE_REPLACE.equals(fieldMode)){
						Element e = (Element)eDeltaField.clone();
						e.removeAttribute("buildaction");
						eBaseIndex.addContent(e);
					}
				}
			}			
		}
	}
	/**
	 * merges two &lt;field&gt; elements
	 * @param eBaseIndex the base index
	 * @param eBaseField the base searchfield
	 * @param eDeltaField the searchfield containing the merge information
	 * @param mode - the merge mode ("delete", "replace" "merge")
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void mergeFields(Element eBaseIndex, Element eBaseField, Element eDeltaField, String mode) throws IOException{
		if(MODE_DELETE.equals(mode)){
			eBaseIndex.removeContent(eBaseField);
			return;
		}
		if(MODE_REPLACE.equals(mode)){
			eBaseIndex.removeContent(eBaseField);
			Element e = (Element)eDeltaField.clone();
			e.removeAttribute("buildaction");
			eBaseIndex.addContent(e);
			return;
		}
		if(MODE_MERGE.equals(mode)){
			eDeltaField.removeAttribute("buildaction");
			List<Attribute>attributes =  eDeltaField.getAttributes();
			for(Attribute a: attributes){
				String name = a.getName();
				if(name.equals("name")){
					//do nothing since it is a kind of key attribute
					continue;
				}
				Attribute baseAttr = eBaseField.getAttribute(name);
				if(baseAttr == null){
					eBaseField.setAttribute((Attribute)a.clone());
					continue;
				}
				if("type source i18n classification".contains(name)){
					//these attributes are unique and cannot be merged -> the will be replaced!
					eBaseField.setAttribute(name, a.getValue());
					continue;
				}
				if("xpath value".contains(name)){
					//these attributes contain XPath Expressions and will be combined by OR ("|")
					String value = a.getValue();
					
					boolean isPresent=false;
					String[] oldValues = baseAttr.getValue().split("\\|");
                    for (String oldValue : oldValues) {
                        if (oldValue.trim().equals(value)) {
                            isPresent = true;
                            break;
                        }
                    }
					if(!isPresent){
						eBaseField.setAttribute(name, baseAttr.getValue()+" | "+value);	
					}
					continue;
				}
					
				if("objects".contains(name)){
					//these attributes contain values separated by blank (" ")
					//the new value will be added with an additional blank (" ")
					String value = a.getValue();
					
					boolean isPresent=false;
					String[] oldValues = baseAttr.getValue().split("\\s");
                    for (String oldValue : oldValues) {
                        if (oldValue.trim().equals(value)) {
                            isPresent = true;
                            break;
                        }
                    }
					if(!isPresent){
						eBaseField.setAttribute(name, baseAttr.getValue()+" "+value);	
					}
                }
			}
		}	
	}
}
