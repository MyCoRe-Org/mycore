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

import java.io.*;

import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.apache.log4j.Logger;
import org.hibernate.type.*;
import org.mycore.common.MCRUtils;
import org.mycore.common.MCRException;

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
    private Element rootOut;

    private DocType dType;
    
    private static Logger logger = Logger.getLogger("org.mycore.backend.hibernate");

    private Document docOut;
    private Element elclass;
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
            this.dType = createDoctype();
            this.rootOut =  new Element("hibernate-mapping");
            this.docOut = new Document(rootOut, dType);
            this.elclass = new Element("class");

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

    private static String doctype_url;

    private DocType createDoctype()
    {
        if(doctype_url==null) {
            try {
                File docFile = new File(new File(System.getProperties().getProperty("java.io.tmpdir")), "hibernate-mapping.dtd");
                InputStream input = this.getClass().getResourceAsStream("/hibernate-mapping.dtd");
                FileOutputStream output = new FileOutputStream(docFile);
                MCRUtils.copyStream(input, output);
                output.close();
                input.close();
                doctype_url = ""+docFile;
                System.out.println("--------------------> "+ doctype_url);
            } catch(IOException e) {
                throw new MCRException("couldn't create temporary hibernate docType file", e);
            }
        }
        return new DocType("hibernate-mapping", "-//Hibernate/Hibernate Mapping DTD//EN", doctype_url);
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
    public boolean addColumn(String Name, String Column, Type Type, int Length, boolean NotNull, boolean Unique, boolean Index){
    	Element prop = new Element("property");
    	Element elColumn = new Element("column");
    	try{
            prop.setAttribute("name", Name);
            prop.setAttribute("type", Type.getName());
            if (Index){
                elColumn.setAttribute("index",getTableName() + "_INDEX");
                elColumn.setAttribute("name",Name);
                if (Length>0){
                    elColumn.setAttribute("length", ""+Length);
                }
                elColumn.setAttribute("not-null",Boolean.toString(NotNull));
                elColumn.setAttribute("unique", Boolean.toString(Unique));
                prop.addContent(elColumn); 
            }else{
                prop.setAttribute("column", Column);
                if (Length>0){
                    prop.setAttribute("length", ""+Length);
                }
                prop.setAttribute("not-null",Boolean.toString(NotNull));
                prop.setAttribute("unique", Boolean.toString(Unique));
            }
            elclass.addContent(prop);
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
    public boolean addIDColumn(String Name, String Column, Type Type, int Length,  String Generator, boolean Index){
        Element elid = new Element("id");
        Element elgenerator = new Element("generator");
        Element elColumn = new Element("column");
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
                elKeyProp.setAttribute("type", Type.getName());
                if (Index){
                    elColumn.setAttribute("name",Name);
                    elColumn.setAttribute("index", getTableName() + "_INDEX");
                    if (Length > 0){
                        elColumn.setAttribute("length", ""+Length);
                    }
                    elKeyProp.addContent(elColumn);
                }else{
                    elKeyProp.setAttribute("column", Column);
                    if (Length > 0){
                        elKeyProp.setAttribute("length", ""+Length);
                    }
                }
                elComposite.addContent(elKeyProp);
                
            }else{
                // only one PK column
                elid.setAttribute("column", Column);
                elid.setAttribute("name", Name);
                elid.setAttribute("type", Type.getName());
                if (Length > 0){
                    elColumn.setAttribute("length", ""+Length);
                }
                elgenerator.setAttribute("class",Generator);
                elid.addContent(elgenerator);
                elclass.addContent(elid);
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
           ret = outputter.outputString(docOut).toString();
           logger.debug(ret);
       }
       catch (Exception e) {
         e.printStackTrace();
       }      
       return ret;
   }
}
