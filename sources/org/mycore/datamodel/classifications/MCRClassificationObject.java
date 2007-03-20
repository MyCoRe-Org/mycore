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

package org.mycore.datamodel.classifications;

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRLinkTableManager;

/**
 * This class is an abstract class for the implementation of the classes
 * classification an category.
 * 
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public abstract class MCRClassificationObject {
    /** The number of the languages * */
    public static final int MAX_CLASSIFICATION_LANG = 8;

    /** The length of the text * */
    public static final int MAX_CLASSIFICATION_TEXT = 254;

    /** The length of the description * */
    public static final int MAX_CLASSIFICATION_DESCRIPTION = 254;

    /** The length of the URL * */
    public static final int MAX_CATEGORY_URL = 254;

    protected String ID;

    protected ArrayList<String> lang;

    protected ArrayList<String> text;

    protected ArrayList<String> description;

    protected String[] childrenIDs;

    protected boolean deleted = false;

    protected static String default_lang = "en";

    /**
     * Load static data for all MCRClassificationObject
     */
    static {
        default_lang = MCRConfiguration.instance().getString("MCR.metadata_default_lang", default_lang);
    }

    /**
     * The method get the MCRClassificationManager instance.
     * 
     * @return the MCRClassificationManager instance.
     */
    protected static MCRClassificationManager manager() {
        return MCRClassificationManager.instance();
    }

    
    /**
     * The abstract constructor of a classififcation or a category.
     * 
     * @param ID
     *            an identifier String
     */
    public MCRClassificationObject(String ID) {
        assert(ID != null);
        this.ID = ID;
        this.text = new ArrayList<String>();
        this.lang = new ArrayList<String>();
        this.description = new ArrayList<String>();
        this.childrenIDs = null;
        this.deleted = false;
    }

    /**
     * The method ensure that the classification is not deleted.
     * 
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    protected void ensureNotDeleted() throws MCRUsageException {
        if (this.deleted) {
            throw new MCRUsageException("This classification object is invalid because it has been deleted");
        }
    }

    /**
     * The method check that this instance realy exist in the store
     *
     * @return true if it is, else false
     **/
    public boolean existInStore(){
  	  return (manager().retrieveCategoryItem( getClassificationID(), ID )!= null);	  
    }
    
    /**
     * This method get the ID.
     * 
     * @return the ID
     * @exception MCRUsageException
     *                if the object is deleted.
     */
    public String getID() throws MCRUsageException {
        ensureNotDeleted();

        return ID;
    }

    /**
     * This method get the classification ID.
     * 
     * @return the classification ID
     */
    protected abstract String getClassificationID();

    /**
     * The method return the text ArrayList.
     * 
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public ArrayList getTextArray() throws MCRUsageException {
        ensureNotDeleted();

        return text;
    }

    /**
     * The method return the text String for a given index.
     * 
     * @param index
     *            a index in the ArrayList
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public String getText(int index) throws MCRUsageException {
        ensureNotDeleted();

        if ((index < 0) || (index > text.size())) {
            return "";
        }

        return ((String) text.get(index));
    }

    /**
     * The method returns the text String for a given language.
     * 
     * @param lang
     *            a language
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public String getText(String lang) throws MCRUsageException {
        ensureNotDeleted();

        if (this.text.size() == 0) {
            return "";
        }

        if (!MCRUtils.isSupportedLang(lang)) {
            return (String) this.text.get(0);
        }

        for (int i = 0; i < this.lang.size(); i++) {
            if (((String) this.lang.get(i)).equals(lang)) {
                return (String) this.text.get(i);
            }
        }

        return (String) this.text.get(0);
    }

    /**
     * The method return the lang ArrayList.
     * 
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public ArrayList getLangArray() throws MCRUsageException {
        ensureNotDeleted();

        return lang;
    }

    /**
     * The method return the lang String for a given index.
     * 
     * @param index
     *            a index in the ArrayList
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public String getLang(int index) throws MCRUsageException {
        ensureNotDeleted();

        if ((index < 0) || (index > text.size())) {
            return default_lang;
        }

        return ((String) lang.get(index));
    }

    /**
     * The method return the description ArrayList.
     * 
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public ArrayList getDescriptionArray() throws MCRUsageException {
        ensureNotDeleted();

        return description;
    }

    /**
     * The method return the description String for a given index.
     * 
     * @param index
     *            a index in the ArrayList
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public String getDescription(int index) throws MCRUsageException {
        ensureNotDeleted();

        if ((index < 0) || (index > text.size())) {
            return "";
        }

        return ((String) description.get(index));
    }

    /**
     * The method returns the description String for a given language.
     * 
     * @param lang
     *            a language
     * @exception MCRUsageException
     *                if the classification is deleted.
     */
    public String getDescription(String lang) throws MCRUsageException {
        ensureNotDeleted();

        if (this.description.size() == 0) {
            return "";
        }

        if (!MCRUtils.isSupportedLang(lang)) {
            return (String) this.description.get(0);
        }

        for (int i = 0; i < this.lang.size(); i++) {
            if (((String) this.lang.get(i)).equals(lang)) {
                return (String) this.description.get(i);
            }
        }

        return (String) this.description.get(0);
    }

    /**
     * The method add a triple of a lang with a lable and a description to the
     * object. The text and the description can be an empty string.
     * 
     * @param lang
     *            a language in form of a 'xml:lang' attribute
     * @param text
     *            a text String
     * @param description
     *            a description String
     */
    public void addData(String lang, String text, String description) {
        ensureNotDeleted();

        if (lang == null) {
            lang = default_lang;
        }

        if (text == null) {
            text = "";
        }

        if (text.length() > MAX_CLASSIFICATION_TEXT) {
            text = text.substring(0, MAX_CLASSIFICATION_TEXT);
        }

        if (description == null) {
            description = "";
        }

        if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
            description = description.substring(0, MAX_CLASSIFICATION_DESCRIPTION);
        }

        this.text.add(text);
        this.lang.add(lang);
        this.description.add(description);
    }

    /**
     * The method set a triple of a lang with a lable and a description to the
     * object. The text and the description can be an empty string.
     * 
     * @param lang
     *            a language in form of a 'xml:lang' attribute
     * @param text
     *            a text String
     * @param description
     *            a description String
     */
    public void setData(String lang, String text, String description) {
        ensureNotDeleted();

        if (lang == null) {
            lang = default_lang;
        }

        if (text == null) {
            text = "";
        }

        if (text.length() > MAX_CLASSIFICATION_TEXT) {
            text = text.substring(0, MAX_CLASSIFICATION_TEXT);
        }

        if (description == null) {
            description = "";
        }

        if (description.length() > MAX_CLASSIFICATION_DESCRIPTION) {
            description = description.substring(0, MAX_CLASSIFICATION_DESCRIPTION);
        }

        int i = -1;

        for (int j = 0; j < this.lang.size(); j++) {
            if (((String) this.lang.get(i)).equals(lang)) {
                i = j;

                break;
            }
        }

        if (i != -1) {
            this.text.add(i, text);
            this.lang.add(i, lang);
            this.description.add(i, description);

            return;
        }

        this.text.add(text);
        this.lang.add(lang);
        this.description.add(description);
    }

    /**
     * The method return the size of the lang/text/description triple.
     * 
     * @return the size
     */
    public int getSize() {
        return text.size();
    }

    /**
     * The method return the text tag as an JDOM element
     * 
     * @param index
     *            the index of the text
     * @return the text as JDOM element
     */
    public final org.jdom.Element getJDOMElement(int index) {
        if ((index < 0) || (index > getSize())) {
            return null;
        }

        org.jdom.Element elm = new org.jdom.Element("label");
        elm.setAttribute("lang", getLang(index), XML_NAMESPACE);
        elm.setAttribute("text", getText(index));
        elm.setAttribute("description", getDescription(index));

        return elm;
    }

    /**
     * The method check that this instance has children.
     * 
     * @return true if it is, else false
     */
    public boolean hasChildren() {
        ensureNotDeleted();

        return (getNumChildren() > 0);
    }

    /**
     * The method return the number of chlidren.
     * 
     * @return the number of chlidren
     */
    public int getNumChildren() {
        ensureNotDeleted();

        if (childrenIDs != null) {
            return childrenIDs.length;
        }
        return manager().retrieveNumberOfChildren(getClassificationID(), ID);
    }

    /**
     * The method return the number of Documents from a Classification or
     * Category for the Classification ID with the Category ID
     * 
     * @return int
     */
    public int countDocLinks(String[] doctypes, String restriction) {
        ensureNotDeleted();
        MCRLinkTableManager mcl = MCRLinkTableManager.instance();
        if (this.getClassificationID().equals(this.getID())) {
            return mcl.countReferenceCategory(this.getClassificationID(), "", doctypes, restriction);
        }
        return mcl.countReferenceCategory(this.getClassificationID(), this.getID(), doctypes, restriction);
    }

    /**
     * The method return a list of MCRCategoryItems they are children of this.
     * 
     * @return a list of children
     */
    public MCRCategoryItem[] getChildren() {
        ensureNotDeleted();

        MCRCategoryItem[] children;

        if (childrenIDs == null) {
            String parentID = ((this instanceof MCRCategoryItem) ? ID : null);
            children = manager().retrieveChildren(getClassificationID(), parentID);
            childrenIDs = new String[children.length];

            for (int i = 0; i < children.length; i++)
                childrenIDs[i] = children[i].getID();
        } else {
            children = new MCRCategoryItem[childrenIDs.length];

            for (int i = 0; i < children.length; i++)
                children[i] = manager().retrieveCategoryItem(getClassificationID(), childrenIDs[i]);
        }

        return children;
    }
    
    public List getChildrenFromJDomAsList(){
  	   ensureNotDeleted();
       String categID = ( this instanceof MCRCategoryItem ? ID : null );
 	   Element EFound;	   
 	   if (categID != null){
 	      String cachingID = getClassificationID() + "@@" + categID;	      
 		  EFound =  (Element) (	manager().jDomCache.get(cachingID));
          if (EFound == null) {
 			 EFound = findCategInJDom(categID, receiveClassificationAsJDOM().getRootElement().getChild("categories"));
            	 manager().jDomCache.put(cachingID, EFound);
          }		
 	   } else {
 		  EFound = receiveClassificationAsJDOM().getRootElement().getChild("categories");
 	   }
 	   return EFound.getChildren("category");    	
    }
    
    public MCRCategoryItem[] getChildrenFromJDom(){
 	   ensureNotDeleted();
       MCRCategoryItem[] children;
       String categID = ( this instanceof MCRCategoryItem ? ID : null );
       Document cljdom = receiveClassificationAsJDOM();
 	   Element EFound;	   
 	   if (categID != null){
 	      String cachingID = getClassificationID() + "@@" + categID;	      
 		  EFound =  (Element) (	manager().jDomCache.get(cachingID));
           if (EFound == null) {
 			 EFound = findCategInJDom(categID, cljdom.getRootElement().getChild("categories"));
            	 manager().jDomCache.put(cachingID, EFound);
           }		
 	   } else {
 		  EFound = cljdom.getRootElement().getChild("categories");
 	   }
 	   List childfromJDom = EFound.getChildren("category");
 	   children = new MCRCategoryItem[ childfromJDom.size() ];
 	   for( int k = 0; k < childfromJDom.size(); k++ )	{
 		   children[k] = setNewCategFromJDomElement( ((Element)childfromJDom.get(k)) );
 	   }   
 	   childrenIDs = new String[ children.length ];	   
 	   for( int k = 0; k < children.length   ; k++ )	{
 		   childrenIDs[ k] = children[ k ].getID();
 	   }
 	   return children;
    }
    
    /**
     * The method remove this.
     */
    public void delete() {
        ensureNotDeleted();

        MCRCategoryItem[] children = getChildren();

        for (int i = 0; i < children.length; i++)
            children[i].delete();

        deleted = true;
    }

    /**
     * Put all data to a string
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName()).append("\n");
        sb.append("ID:             ").append(ID).append("\n");

        for (int i = 0; i < text.size(); i++) {
            sb.append("Lang:           ").append(((String) lang.get(i))).append("\n").append("Text:          ").append(((String) text.get(i))).append("\n").append("Description:    ").append(((String) description.get(i))).append("\n");
        }

        sb.append("\n");

        return sb.toString();
    }
    
    private Element findCategInJDom( String categID, Element categories){
        List jchildren = categories.getChildren("category");
   	 Element EFound = new Element("category");
   	 for( int j = 0; j < jchildren.size(); j++ )	{
   		 if( ((Element)jchildren.get(j)).getAttributeValue("ID").equalsIgnoreCase(categID)){
   			 //System.out.println("Treffer:" + categID);
   			 return (Element)jchildren.get(j);
   		 }
   		 if (!((Element)jchildren.get(j)).getChildren("category").isEmpty())  {
   			 EFound = findCategInJDom(categID, (Element)jchildren.get(j));
   			 if (EFound.getAttribute("ID") != null ){
   				 return EFound;
   			 }
   		 }
      	 }
   	 return EFound;
      }
      
      private MCRCategoryItem 	setNewCategFromJDomElement( Element newCateg) {	   
    	   MCRCategoryItem cat = new MCRCategoryItem(newCateg.getAttributeValue("ID"),this);
    	   if ( newCateg.getAttributeValue("counter")!= null )
    		   cat.counter = Integer.parseInt( newCateg.getAttributeValue("counter"));
    	   List tagList = newCateg.getChildren("label");
    	   Element element;
   	   for (int i = 0; i < tagList.size(); i++) {
   		   element = (Element) tagList.get(i);
   		   cat.addData(element.getAttributeValue("lang",XML_NAMESPACE),
   		   			   element.getAttributeValue("text"),
   		   			   element.getAttributeValue("description"));		   
   	   }
   	   //process url, if given
   	   element = newCateg.getChild("url");
   	   if (element != null) {    	   
   		   cat.URL= element.getAttributeValue("href", XLINK_NAMESPACE);
   	   }
   	   return cat;
      }
      
   
    public Document receiveClassificationAsJDOM() {
 	   //	 Klassification laden!
 	   //MCRClassification cl = new MCRClassification();
 	   String cachingID = getClassificationID();
        Document cljdom =  (Document)(	manager().jDomCache.get(cachingID));
        if (cljdom == null) {
     	   cljdom = MCRClassification.receiveClassificationAsJDOM(getClassificationID());
    	  	   manager().jDomCache.put(cachingID,cljdom); 
        }
        return cljdom;
    }
}
