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

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.oai.MCROAIQuery;
import org.mycore.services.query.MCRQueryResult;

/**
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.10 $ $Date: 2003/01/31 11:56:25 $
 *
 * This is the MyCoRe-Implementation of the <i>MCROAIQuery</i>-Interface.
 */
public class MCROAIQueryService implements MCROAIQuery {

    private static final String STR_OAI_RESTRICTION_CLASSIFICATION = "MCR.oai.restriction.classification"; //Classification and...
    private static final String STR_OAI_RESTRICTION_CATEGORY = "MCR.oai.restriction.category"; //...Category to restrict the access to
	private static final String STR_OAI_SETSCHEME = "MCR.oai.setscheme"; // the classification id which serves as scheme for the OAI set structure
    private static final String STR_OAI_REPOSITORY_IDENTIFIER = "MCR.oai.repositoryidentifier"; // Identifier of the repository

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
			return list;
        }
        return null;
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
       	    set[1] = new String(categories[i].getLabel("en"));
          	set[2] = new String(categories[i].getDescription("en"));
          	
	    	// logger.debug("Suche nach Kategorie: " + set[0]);    
    		
    		//We should better have a look if the set is empty...        
	        StringBuffer query = new StringBuffer("");
            query.append("/mycoreobject[@classid=\"").append(categories[i].getClassificationID()).
                append("\" and @categid=\"").append(categories[i].getID()).append("\"]");

			MCRConfiguration config = MCRConfiguration.instance();
			try {
				String restrictionClassification = config.getString(STR_OAI_RESTRICTION_CLASSIFICATION + "." + instance);
				String restrictionCategory = config.getString(STR_OAI_RESTRICTION_CATEGORY + "." + instance);
				
				query.append(" and ").append("/mycoreobject[@classid=\"").append(restrictionClassification).
	                append("\" and @categid=\"").append(restrictionCategory).append("\"]");
		    } catch (MCRConfigurationException mcrx) {
		    }
			    
			MCRQueryResult qr = new MCRQueryResult();
			MCRXMLContainer qra = null;
    	    try {
		        qra = qr.setFromQuery("local", "document", query.toString());
    	    } catch (MCRException mcrx) {
    	    	// logger.error("Die Query " + query.toString() + "ist fehlgeschlagen.");
    	    	return newList;
    	    }

			if (qra.size() > 0) {
		    	newList.add(set);
		    
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
		MCRConfiguration config = MCRConfiguration.instance();
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
        	    String date = from[0].substring(8) + "." + from[0].substring(5, 7) +
    	            "." + from[0].substring(0, 4);
	            query.append(" and ").append("/mycoreobject[service.dates.date>=\"").append(date).
            	    append("\" and service.dates.date/@type=\"modifydate\"]");
        	}
    	    if (until != null) {
	            String date = until[0].substring(8) + "." + until[0].substring(5, 7) +
            	    "." + until[0].substring(0, 4);
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
			    
        MCRQueryResult qr = new MCRQueryResult();
   	    try {
	        MCRXMLContainer qra = qr.setFromQuery("local", "document", query.toString());
   	    
   	    	if (qra.size() == 0) {
   	    		return null;
   	    	}
   	    	
	   	    for (int i = 0; i < qra.size(); i++) {
   		    	String objectId = qra.getId(i);
   	    	
	    	    MCRObject object = new MCRObject();
    	        object.receiveFromDatastore(objectId);
    	    
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
        
        		list.add(identifier);
	   	    }
   	    } catch (MCRException mcrx) {
   	    	return null;
   	    }
   	    
		return list;
	}
	
}
