/**
 * $RCSfile: MCROAIQueryService.java,v $
 * $Revision: 1.10 $ $Date: 2003/01/31 11:56:25 $
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

package org.mycore.services.oai;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.Element;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.query.MCRQueryCollector;

/**
 * @author Werner Gresshoff
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision: 1.10 $ $Date: 2003/01/31 11:56:25 $
 *
 * This is the MyCoRe-Implementation of the <i>MCROAIQuery</i>-Interface.
 */
public class MCROAIQueryService implements MCROAIQuery {
    static Logger logger = Logger.getLogger(MCROAIQueryService.class);
    static MCRQueryCollector collector;

    private static final String STR_OAI_RESTRICTION_CLASSIFICATION = "MCR.oai.restriction.classification"; //Classification and...
    private static final String STR_OAI_RESTRICTION_CATEGORY = "MCR.oai.restriction.category"; //...Category to restrict the access to
	private static final String STR_OAI_SETSCHEME = "MCR.oai.setscheme"; // the classification id which serves as scheme for the OAI set structure
    private static final String STR_OAI_REPOSITORY_IDENTIFIER = "MCR.oai.repositoryidentifier"; // Identifier of the repository

	private MCRConfiguration config;
	
	/**
	 * Method MCROAIQueryService.
	 */
	public MCROAIQueryService() {
		config = MCRConfiguration.instance();
    	config.reload(true);
    	PropertyConfigurator.configure(config.getLoggingProperties());
    	if (collector==null){
			int cThreads=config.getInt("MCR.Collector_Thread_num",2);
			int aThreads=config.getInt("MCR.Agent_Thread_num",6);
			collector=new MCRQueryCollector(cThreads,aThreads);
    	}
	}
	
	/**
	 * Method exists. Checks if the given ID exists in the data repository
	 * @param id The ID to be checked
	 * @return boolean
	 */
	public boolean exists(String id) {
		return MCRObject.existInDatastore(id);
	}
	
	/**
	 * Method listSets. Gets a list of classificationId's and Labels for a given ID
	 * @param classificationId
	 * @param instance the Servletinstance
	 * @return List A list that contains an array of three Strings: the category id, 
	 * 				the label and a description
	 */
	public List listSets(String classificationId, String instance) {
		List list = new ArrayList();
        MCRClassificationItem repository = MCRClassificationItem.
            getClassificationItem(classificationId);
        if ((repository != null) && (repository.hasChildren())) {
        	MCRCategoryItem[] children = repository.getChildren();

			list = getSets(list, repository.getChildren(), "", instance);
        }

		return list;
	}
	
	/**
	 * Method getSets. Creates a <i>list</i> from an Array of Sets
	 * @param list The list to add the new elements to.
	 * @param categories The categories to extract the information from.
	 * @param parentSpec The setSpec of the parent set.
	 * @param instance the Servletinstance
	 * @return List A list that contains an array of three Strings: the category id, 
	 * 				the label and a description
	 */
    private List getSets(List list, MCRCategoryItem[] categories, String parentSpec, String instance) {
        List newList = new ArrayList(list);
        
        for (int i = 0; i < categories.length; i++) { 
           	String[] set = new String[3];
   	        set[0] = new String(parentSpec + categories[i].getID());
       	    set[1] = new String(categories[i].getText("en"));
          	set[2] = new String(categories[i].getDescription("en"));
          	
	    	logger.debug("Suche nach Kategorie: " + categories[i].getID());    
    		
    		//We should better have a look if the set is empty...        
	        StringBuffer query = new StringBuffer("");
            query.append("/mycoreobject[@classid=\"").append(categories[i].getClassificationID()).
                append("\" and @categid=\"").append(categories[i].getID()).append("\"]");

			try {
				String restrictionClassification = config.getString(STR_OAI_RESTRICTION_CLASSIFICATION + "." + instance);
				String restrictionCategory = config.getString(STR_OAI_RESTRICTION_CATEGORY + "." + instance);
				
				query.append(" and ").append("/mycoreobject[@classid=\"").append(restrictionClassification).
	                append("\" and @categid=\"").append(restrictionCategory).append("\"]");
		    } catch (MCRConfigurationException mcrx) {
		    }
			    
   	    	logger.debug("Die erzeugte Query ist: " + query.toString());
   	    	
			MCRXMLContainer qra = new MCRXMLContainer();
    	    try {
    	    	synchronized(qra){
    	    		collector.collectQueryResults("local", "document", query.toString(),qra);
    	    		qra.wait();
    	    	}
    	    } catch (MCRException mcrx) {
    	    	logger.error("Die Query ist fehlgeschlagen.");
    	    	return newList;
    	    } catch (InterruptedException ignored){}

			if (qra.size() > 0) {
		    	newList.add(set);
		    	logger.debug("Der Gruppenliste wurde ein neuer Datensatz hinzugef�gt.");
		    	
    	        if (categories[i].hasChildren()) {
        	        newList = getSets(newList, categories[i].getChildren(), set[0] + ":", instance);
	            }
			}
		}
        
        return newList;
    }
    
	/**
	 * Method listIdentifiers.Gets a list of identifiers
	 * @param set the category (if known) is in the first element
	 * @param from the date (if known) is in the first element
	 * @param until the date (if known) is in the first element
	 * @param instance the Servletinstance
	 * @return List A list that contains an array of three Strings: the identifier,
	 * 				a datestamp (modification date) and a string with a blank
	 * 				separated list of categories the element is classified in
	 */
	public List listIdentifiers(String[] set, String[] from, String[] until, String instance) {
		List list = new ArrayList();
        StringBuffer query = new StringBuffer("");
        String classificationId = null;
        String repositoryId = null;

		try {
	        classificationId = config.getString(STR_OAI_SETSCHEME + "." + instance);
        	repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);

        	if (set == null) {
            	query.append("/mycoreobject[metadata/*/*/@classid=\"").append(classificationId).append("\"]");
    	    } else {
	            String categoryId = set[0].substring(set[0].lastIndexOf(':') + 1);
            	query.append("/mycoreobject[@classid=\"").append(classificationId).
        	        append("\" and @categid=\"").append(categoryId).append("\"]");
    	    }
	        if (from != null) {
/*        	    String date = from[0].substring(8) + "." + from[0].substring(5, 7) +
    	            "." + from[0].substring(0, 4); */
    	        String date = from[0];
	            query.append(" and ").append("/mycoreobject[service.dates.date>=\"").append(date).
            	    append("\" and service.dates.date/@type=\"modifydate\"]");
        	}
    	    if (until != null) {
/*	            String date = until[0].substring(8) + "." + until[0].substring(5, 7) +
            	    "." + until[0].substring(0, 4); */
            	String date = until[0];
        	    query.append(" and ").append("/mycoreobject[service.dates.date<=\"").append(date).
    	            append("\" and service.dates.date/@type=\"modifydate\"]");
	        }
	    } catch (MCRConfigurationException mcrx) {
	    }
        
		try {
			String restrictionClassification = config.getString(STR_OAI_RESTRICTION_CLASSIFICATION + "." + instance);
			String restrictionCategory = config.getString(STR_OAI_RESTRICTION_CATEGORY + "." + instance);
				
			query.append(" and ").append("/mycoreobject[@classid=\"").append(restrictionClassification).
                append("\" and @categid=\"").append(restrictionCategory).append("\"]");
	    } catch (MCRConfigurationException mcrx) {
	    }
			    
    	logger.debug("Die erzeugte Query ist: " + query.toString());
    	
   	    try {
   	    	MCRXMLContainer qra=new MCRXMLContainer();
   	    	synchronized(qra){
				collector.collectQueryResults("local", "document", query.toString(),qra);
				qra.wait();
   	    	}
   	    
   	    	if (qra.size() == 0) {
   	    		return list;
   	    	}
   	    	
	   	    for (int i = 0; i < qra.size(); i++) {
   		    	String objectId = qra.getId(i);
   	    	
	    	    MCRObject object = new MCRObject();
    	        object.receiveFromDatastore(objectId);
	 	
        		String[] identifier = getHeader(object, objectId, repositoryId);
        		list.add(identifier);
	   	    }
   	    } catch (Exception mcrx) {
   	    	logger.error("Die Query ist fehlgeschlagen.");
   	    } finally {
   	    	return list;
   	    }
	}
	
	/**
	 * Method getHeader. Gets the header information from the MCRObject <i>object</i>.
	 * @param object The MCRObject
	 * @param objectId The objectId as String representation
	 * @param repositoryId The repository id
	 * @return String[] Array of three Strings:  the identifier,
	 * 				a datestamp (modification date) and a string with a blank
	 * 				separated list of categories the element is classified in
	 */
	private String[] getHeader(MCRObject object, String objectId, String repositoryId) {
	    Calendar calendar = object.getService().getDate("modifydate");
	    // Format the date.
        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
   		formatter.setCalendar(calendar);
        String datestamp = formatter.format(calendar.getTime());
            
        StringBuffer setSpec = new StringBuffer("");
       	String[] identifier = new String[3];
       	identifier[0] = "oai:" + repositoryId + ":" + objectId;
       	identifier[1] = datestamp;
   		identifier[2] = new String("");
        
        for (int j = 0; j < object.getMetadata().size(); j++) {
   		    if (object.getMetadata().getMetadataElement(j).getClassName().equals("MCRMetaClassification")) {
           		MCRMetaElement element = object.getMetadata().getMetadataElement(j);
                		
                if (element.getTag().equals("subjects")) {
   		            for (int k = 0; k < element.size(); k++) {
           		        MCRMetaClassification classification = (MCRMetaClassification) element.getElement(k);
           		        String classificationId = classification.getClassId();
                        String categoryId = classification.getCategId();
                        MCRCategoryItem category = MCRCategoryItem.getCategoryItem(classificationId, categoryId);
                        MCRCategoryItem parent;
   		                while ((parent = category.getParent()) != null) {
                            categoryId = parent.getID() + ":" + categoryId;
                   		    category = parent;
           		        }
                		        
           		        setSpec.append(" ").append(categoryId);
               		}
                    		
               		identifier[2] = setSpec.toString().trim();
           		}
       		}
   		}
   		
   		return identifier;
	}
	
	/**
	 * Method getRecord. Gets a metadata record with the given <i>id</id>.
	 * @param id The id of the object.
	 * @param instance the Servletinstance
	 * @return List A list that contains an array of three Strings: the identifier,
	 * 				a datestamp (modification date) and a string with a blank
	 * 				separated list of categories the element is classified in
	 * 				and a JDOM element with the metadata of the record
	 */
	public List getRecord(String id, String instance) {
		List list = new ArrayList();

        MCRObject object = new MCRObject();
        String repositoryId = null;
        try {
        	repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);
            object.receiveFromDatastore(id);
	    } catch (MCRConfigurationException mcrx) {
	    	return null;
        } catch (MCRException e) {
            return null;
        }
	 	
   		String[] identifier = getHeader(object, id, repositoryId);
   		list.add(identifier);

        Element eMetadata = object.getMetadata().createXML();
        list.add(eMetadata);
        
        return list;
	}
		
	/**
	 * Method listRecords.Gets a list of metadata records
	 * @param set the category (if known) is in the first element
	 * @param from the date (if known) is in the first element
	 * @param until the date (if known) is in the first element
	 * @param instance the Servletinstance
	 * @return List A list that contains an array of three Strings: the identifier,
	 * 				a datestamp (modification date) and a string with a blank
	 * 				separated list of categories the element is classified in
	 */
	public List listRecords(String[] set, String[] from, String[] until, String instance) {
		List list = new ArrayList();
        StringBuffer query = new StringBuffer("");
        String classificationId = null;
        String repositoryId = null;

		try {
	        classificationId = config.getString(STR_OAI_SETSCHEME + "." + instance);
        	repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);

        	if (set == null) {
            	query.append("/mycoreobject[metadata/*/*/@classid=\"").append(classificationId).append("\"]");
    	    } else {
	            String categoryId = set[0].substring(set[0].lastIndexOf(':') + 1);
            	query.append("/mycoreobject[@classid=\"").append(classificationId).
        	        append("\" and @categid=\"").append(categoryId).append("\"]");
    	    }
	        if (from != null) {
/*        	    String date = from[0].substring(8) + "." + from[0].substring(5, 7) +
    	            "." + from[0].substring(0, 4); */
    	        String date = from[0];
	            query.append(" and ").append("/mycoreobject[service.dates.date>=\"").append(date).
            	    append("\" and service.dates.date/@type=\"modifydate\"]");
        	}
    	    if (until != null) {
/*	            String date = until[0].substring(8) + "." + until[0].substring(5, 7) +
            	    "." + until[0].substring(0, 4); */
    	        String date = until[0];
        	    query.append(" and ").append("/mycoreobject[service.dates.date<=\"").append(date).
    	            append("\" and service.dates.date/@type=\"modifydate\"]");
	        }
	    } catch (MCRConfigurationException mcrx) {
	    }
        
		try {
			String restrictionClassification = config.getString(STR_OAI_RESTRICTION_CLASSIFICATION + "." + instance);
			String restrictionCategory = config.getString(STR_OAI_RESTRICTION_CATEGORY + "." + instance);
				
			query.append(" and ").append("/mycoreobject[@classid=\"").append(restrictionClassification).
                append("\" and @categid=\"").append(restrictionCategory).append("\"]");
	    } catch (MCRConfigurationException mcrx) {
	    }
			    
    	logger.debug("Die erzeugte Query ist: " + query.toString());
    	
   	    try {
	        MCRXMLContainer qra = new MCRXMLContainer();
	        synchronized(qra){
				collector.collectQueryResults("local", "document", query.toString(),qra);
				qra.wait();
	        }
   	    
   	    	if (qra.size() == 0) {
   	    		return list;
   	    	}
   	    	
	   	    for (int i = 0; i < qra.size(); i++) {
   		    	String objectId = qra.getId(i);
   	    	
	    	    MCRObject object = new MCRObject();
    	        object.receiveFromDatastore(objectId);
	 	
        		String[] identifier = getHeader(object, objectId, repositoryId);
        		list.add(identifier);

    		    Element eMetadata = object.getMetadata().createXML();
	        	list.add(eMetadata);
	   	    }
   	    } catch (Exception mcrx) {
   	    	logger.error("Die Query ist fehlgeschlagen.");
   	    } finally {
   	    	return list;
   	    }
	}
	
}
