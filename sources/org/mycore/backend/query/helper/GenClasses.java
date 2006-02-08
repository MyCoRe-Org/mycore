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

package org.mycore.backend.query.helper;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.services.fieldquery.MCRFieldDef;

public class GenClasses {
    private static String pack = "org.mycore.backend.query";
    private static MCRConfiguration config = MCRConfiguration.instance();

    private HashMap searchfields = new HashMap();

    private HashMap typeMapping = new HashMap();

    public static void main(String[] args) {
        
        try {
            if (args.length < 1) {
                System.out.println("needs arg:\n" + "1: pathname for output as String\n" + "2: (optional) package name as String");
            } else {
                if (args.length == 2) {
                    pack = args[1];
                }            	
            	int i = 0;
            	for (Iterator it = getHibernateIndices().iterator(); it.hasNext();) {
            		GenClasses gen = new GenClasses();
            		gen.buildFieldMapping();
            		i++;
            		String indexID = (String)it.next();
            		String indexName = config.getString("MCR.Searcher." + indexID + ".Index");
            		String queryClassName;
            		String queryTable = config.getString("MCR.Searcher." + indexID + ".TableName");
           			queryClassName = queryTable + "Bean";
            		gen.readDefinition(indexName);
            		gen.execute(args[0], queryClassName);
				}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List getHibernateIndices(){
    	List ret = new ArrayList();
    	Properties props = config.getProperties("MCR.Searcher.");
    	for(Enumeration en = props.keys(); en.hasMoreElements();) {
    		String prop = (String)en.nextElement();
    		if(prop.endsWith("Class") && props.getProperty(prop).endsWith("MCRHIBSearcher")) {
    			Matcher m = Pattern.compile("MCR\\.Searcher\\.(.*)\\.Class").matcher(prop);
    			if (m.find()) {
    				ret.add(m.group(1));
    			}
    		}
    	}
    	return ret;
    }
    
    private void execute(String dest, String queryClassName) {
        try {
            JavaClass jc = new JavaClass(pack, queryClassName);
            Iterator it = searchfields.keySet().iterator();

            jc.addField("String", "mcrid");

            while (it.hasNext()) {
                Element el = (Element) searchfields.get(it.next());
                jc.addField((String) typeMapping.get(el.getAttributeValue("type")), el.getAttributeValue("name"));
            }

            jc.write(dest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readDefinition(String indexName) {
        try {
            searchfields = loadFields(MCRFieldDef.getConfigFile(), indexName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashMap loadFields(Element def, String indexName) {
        HashMap map = new HashMap();
        List fields = new LinkedList();

        for (int i = 0; i < def.getChildren().size(); i++) {
            if (((Element) def.getChildren().get(i)).getAttributeValue("id").equals(indexName)) {
                fields = ((Element) def.getChildren().get(i)).getChildren();

                break;
            }
        }

        for (int i = 0; i < fields.size(); i++) {
            Element fieldelement = (Element) fields.get(i);
            map.put(fieldelement.getAttributeValue("name"), fieldelement);

            List children = new LinkedList();
            children = fieldelement.getChildren();

            for (int j = 0; j < children.size(); j++) {
                Element addElement = new Element("field");
                Element childelement = (Element) children.get(j);

                addElement.setAttribute("name", fieldelement.getAttributeValue("name") + "_" + childelement.getAttributeValue("name"));
                addElement.setAttribute("xpath", fieldelement.getAttributeValue("xpath"));
                addElement.setAttribute("type", "text");
                map.put(addElement.getAttributeValue("name"), addElement);
            }
        }

        return map;
    }

    private void buildFieldMapping() {
        typeMapping.put("text", "String");
        typeMapping.put("name", "String");
        typeMapping.put("identifier", "String");
        typeMapping.put("date", "java.sql.Date");
        typeMapping.put("time", "java.sql.Time");
        typeMapping.put("timestamp", "java.sql.Timestamp");
        typeMapping.put("integer", "Integer");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("decimal", "Double");
    }
}
