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

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class GenClasses {
    private static String pack = "org.mycore.backend.query";

    private HashMap searchfields = new HashMap();

    private HashMap typeMapping = new HashMap();

    public static void main(String[] args) {
        GenClasses gen = new GenClasses();

        try {
            if (args.length < 2) {
                System.out.println("needs arg:\n" + "1: filename for queryconfiguration as String\n" + "2: pathname for output as String\n" + "3: (optional) package name as String");
            } else {
                gen.buildFieldMapping();
                gen.readDefinition(args[0], args[1]);
                gen.execute(args[1]);

                if (args.length == 3) {
                    pack = args[3];
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute(String dest) {
        try {
            JavaClass jc = new JavaClass(pack, "MCRQuery");
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

    private void readDefinition(String path, String dest) {
        try {
            SAXBuilder builder = new SAXBuilder();
            File d = new File(path);
            builder.setValidation(false);
            searchfields = loadFields(builder.build(d).getRootElement());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashMap loadFields(Element def) {
        HashMap map = new HashMap();
        List fields = new LinkedList();

        for (int i = 0; i < def.getChildren().size(); i++) {
            if (((Element) def.getChildren().get(i)).getAttributeValue("id").equals("metadata")) {
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
