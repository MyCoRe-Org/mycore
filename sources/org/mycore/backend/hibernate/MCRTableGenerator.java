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

package org.mycore.backend.hibernate;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;

import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.hibernate.type.*;

/**
 * Creator class for hibernte mapping file
 * This class generates a jDOM mapping file for hibernate
 * to map a sql-table with a java class
 * 
 * @author Arne Seifert
 */
public class MCRTableGenerator {
    
    /**
     * Object declaration
     */
    private Element rootOut = new Element("hibernate-mapping");
    private DocType dType= new DocType("hibernate-mapping",
    		"-//Hibernate/Hibernate Mapping DTD//EN", 
			"http://hibernate.sourceforge.net/hibernate-mapping.dtd");
    /*private DocType dType= new DocType("hibernate-mapping",
    		"-//Hibernate/Hibernate Mapping DTD//EN", 
			"file:///home/seifert/Dokumente/java/jDomParse/hibernate-mapping.dtd");*/

    private Document docOut = new Document(rootOut, dType);
    private Element elclass = new Element("class");
    private int intPKColumns =1;
    private int intIdSet = 0;
    private String classname = ""; 
    
    /**
     * Constructor for table
     * @param tableName name of the table/class
     * @param sqlName name of the sql-table
     * @param Package name of the talbe package in the database;
     * @param intPKColumns number of primary key columns
     */
    public MCRTableGenerator(String tableName, String sqlName, String Package, int intPKColumns){
        try{
            net.sf.ehcache.CacheManager.create(new ByteArrayInputStream(
            	    "<?xml version=\"1.0\"?><ehcache><defaultCache maxElementsInMemory=\"10000\" eternal=\"false\" timeToIdleSeconds=\"120\" timeToLiveSeconds=\"120\" overflowToDisk=\"true\" diskPersistent=\"false\" diskExpiryThreadIntervalSeconds=\"120\"/></ehcache>"
            	    .getBytes()));

            
            this.intPKColumns = intPKColumns;
            this.classname = sqlName;
            elclass.setAttribute("name", sqlName);
            elclass.setAttribute("table", tableName);
            rootOut.addContent(elclass);
            if (! Package.equals("")){
            	docOut.getRootElement().setAttribute("package",Package);
            }
        }catch(Exception e){
        	System.out.println(e.toString());
        }
    }
    
    /**
     * This method adds a new column in the mapping table
     * 
     * @param Name name of the class-attribute
     * @param Column name of the table-column
     * @param Type datatype of sql-column
     * @param Notnull NotNull-value of column
     * @param Unique identifier for unique columns
     * @return boolean as indicator for errors
     */
    public boolean addColumn(String Name, String Column, Type Type, int Length, boolean NotNull, boolean Unique){
    	Element prop = new Element("property");
    	try{
            prop.setAttribute("name", Name);
            prop.setAttribute("column", Column);
            prop.setAttribute("type", Type.getName());
            if (Length>0){
                prop.setAttribute("length", ""+Length);
            }
            prop.setAttribute("not-null",Boolean.toString(NotNull));
            prop.setAttribute("unique", Boolean.toString(Unique));
            elclass.addContent(prop);
            prop = null;
            return true;
    	}catch(Exception e){
    		System.out.println(e.toString());
    		return false;
    	}
    }
    
    /**
     * This method adds a new ID column in the mapping table
     * 
     * @param Name name of the class-attribute
     * @param Column name of the table-column
     * @param Type datatype of sql-column
     * @param Generator attribute for the table id
     * @return boolean as indicator for errors
     */
    public boolean addIDColumn(String Name, String Column, Type Type, int Length,  String Generator){
        Element elid = new Element("id");
        Element elgenerator = new Element("generator");
        try{
            if (this.intPKColumns > 1){
                //more then one PK column
                Element elComposite;
                Element elKeyProp = new Element("property"); 
                if (intIdSet == 0){
                    // 1st id column
                    elComposite = new Element("composite-id");
                    intIdSet = 1;
                    elComposite.setAttribute("class", this.classname + "PK");
                    elComposite.setAttribute("name", "key");
                    elclass.addContent(elComposite);
                }else{
                    // 2nd... PK Column
                    elComposite = elclass.getChild("composite-id");
                }
                elKeyProp.setAttribute("name",Name);
                elKeyProp.setAttribute("column", Column);
                elKeyProp.setAttribute("type", Type.getName());
                if (Length > 0){
                    elKeyProp.setAttribute("length", ""+Length);
                }
                elComposite.addContent(elKeyProp);
                
            }else{
               // only one PK column
                elid.setAttribute("column", Column);
                elid.setAttribute("name", Name);
                elid.setAttribute("type", Type.getName());
                elgenerator.setAttribute("class",Generator);
                elid.addContent(elgenerator);
                elclass.addContent(elid);
                elid = null;
                elgenerator = null;
            }
            return true;
        }catch(Exception e){
            System.out.println(e.toString());
            return false;
        }
    	
    }
    
    /**
     * This method returns the name of the table
     * @return tablename as string
     */
    public String getTableName(){
    	try{
    		return docOut.getRootElement().getChild("class").getAttributeValue("table");
    	}catch(Exception e){
    		System.out.println(e.toString());
    		return "";
    	}
    }
    
    /**
     * This methos returns the java class table name
     * @return classname for table as string
     */
    public String getClassName(){
    	try{
    		return docOut.getRootElement().getChild("class").getAttributeValue("name");
    	}catch(Exception e){
    		System.out.println(e.toString());
    		return "";
    	}
    }
   
   /**
    * This method converts the mapping jdom to a string
    * @return complete mapping defifnition as xml-string
    */   
   public String getTableXML(){
   		String ret="";
       try {
           Format format = Format.getPrettyFormat();             
           XMLOutputter outputter = new XMLOutputter(format);
           FileOutputStream output;
           output = new FileOutputStream("./file.xml");
           outputter.output(docOut , output); 
           ret = outputter.outputString(docOut).toString();
           System.out.println(ret);
       }
       catch (Exception e) {
         System.err.println(e);
       }      
       //System.out.println(docOut.toString());
       return ret;
   }
    
}
