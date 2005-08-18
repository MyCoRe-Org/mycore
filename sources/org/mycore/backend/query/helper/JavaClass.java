package org.mycore.backend.query.helper;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;

public class JavaClass
{
    private String name;
    private String pack;
    private List fields;

    public JavaClass(String pack, String name)
    {
    this.pack = pack;
    this.name = name;
    this.fields = new LinkedList();
    }

    public void addField(String type, String name)
    {
        this.fields.add(new Field(type, name));
    }
    
    public String up(String s)
    {
        if(s.length()<=0)
            return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
    
    public void write(String directory) throws IOException
    {
        FileOutputStream fo = new FileOutputStream(new File(new File(directory), name+".java"));
        BufferedOutputStream bo = new BufferedOutputStream(fo);
        PrintWriter po = new PrintWriter(fo);
        
        po.println("/*");
        po.println(" * This file is part of ** M y C o R e **");
        po.println(" * Visit our homepage at http://www.mycore.de/ for details.");
        po.println(" *");
        po.println(" * This program is free software; you can use it, redistribute it");
        po.println(" * and / or modify it under the terms of the GNU General Public License");
        po.println(" * (GPL) as published by the Free Software Foundation; either version 2");
        po.println(" * of the License or (at your option) any later version.");
        po.println(" *");
        po.println(" * This program is distributed in the hope that it will be useful, but");
        po.println(" * WITHOUT ANY WARRANTY; without even the implied warranty of");
        po.println(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
        po.println(" * GNU General Public License for more details.");
        po.println(" *");
        po.println(" * You should have received a copy of the GNU General Public License");
        po.println(" * along with this program, normally in the file license.txt.");
        po.println(" * If not, write to the Free Software Foundation Inc.,");
        po.println(" * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA");
        po.println(" */");
        po.println("");
        
        po.println("package "+pack+";");
        po.println("");
        po.println("import java.sql.Types.*;");
        po.println("");
        po.println("class "+name);
        po.println("{");
        Iterator it = fields.iterator();
        while(it.hasNext()) {
            Field field = (Field)it.next();
            po.println("    private "+field.type+" "+field.name+";");
        }
        
        it = fields.iterator();
        
        po.println("");
        while(it.hasNext()) {
            Field field = (Field)it.next();
            po.println("    /**");
            po.println("    * @hibernate.property");
            po.println("    * column=\""+field.name.toUpperCase()+"\"");
            po.println("    * not-null=\"true\"");
            po.println("    * update=\"true\"");
            po.println("    */");
            po.println("    public "+field.type+" get"+up(field.name)+"() {");
            po.println("        return "+field.name+";");
            po.println("    }");
            po.println("    public void set"+up(field.name)+"("+field.type+" "+field.name+") {");
            po.println("        this."+field.name+" = "+field.name+";");
            po.println("    }");
            if(it.hasNext()) {
                po.println("");
            }
        }
        
        po.println("}");
        po.close();
        bo.close();
        fo.close();
    }
    
    private class Field
    {
        String type;
        String name;
        Field(String type, String name) {this.type = type;this.name = name;}
    };
}

