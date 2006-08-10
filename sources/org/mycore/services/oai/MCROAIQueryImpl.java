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

package org.mycore.services.oai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * @author Heiko Helmbrecht
 * 
 * @version $Revision$ $Date$
 * 
 * This is the MyCoRe-Implementation of the <i>MCROAIQuery </i>-Interface.
 */
public class MCROAIQueryImpl implements MCROAIQuery {

    private static Logger logger;

    // maximum number of returned list sets
    private static int maxReturns;

    static {
        logger = Logger.getLogger(MCROAIQueryImpl.class.getName());
        maxReturns = MCROAIProvider.getMaximalHitsize();
    }

    private int deliveredResults = 0;

    private int numResults = 0;

    private String lastQuery = "";

    private Object[] resultArray;

    /**
     * Method MCROAIQueryService.
     */
    public MCROAIQueryImpl() {
    }

    /**
     * Method exists. Checks if the given ID exists in the data repository
     * 
     * @param id
     *            The ID to be checked
     * @return boolean
     */
    public boolean exists(String id) {
        return MCRObject.existInDatastore(id);
    }

    /**
     * Method listSets. Gets a list of classificationId's and Labels for a given
     * ID
     * 
     * @param classificationId
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the category
     *         id, the label and a description
     */
    public List listSets(String instance) {
        String[] classification = MCROAIProvider.getConfigBean(instance).getClassificationIDs();
        List list = new ArrayList();
        for (int i = 0; i < classification.length; i++) {
            MCRClassificationItem repository = MCRClassificationItem.getClassificationItem(classification[i]);
            MCRCategoryItem[] children = repository.getChildren();
            logger.debug("ClassificationItem " + repository.getClassificationID() + " hat " + children.length + " Kinder.");
            if ((repository != null) && (repository.hasChildren())) {
                logger.debug("ClassificationItem hat " + repository.getNumChildren() + " Kinder.");
                List newSets =getSets(repository.getChildren(), "", instance);
                if (newSets.size()>0){
                	list.addAll(newSets);
                	if(repository.getLangArray().contains("x-dini")){
                    	String[] set = new String[3];
                    	set[0]= repository.getText("x-dini");
                    	set[1]=repository.getText("x-dini");
                    	set[2]=repository.getDescription("x-dini");
                    	list.add(set);
                    }
                }	
            }
        }

        return list;
    }

    /**
     * Method getSets. Creates a <i>list </i> from an Array of Sets
     * 
     * @param categories
     *            The categories to extract the information from.
     * @param parentSpec
     *            The setSpec of the parent set.
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the category
     *         id, the label and a description
     */
    private List getSets(MCRCategoryItem[] categories, String parentSpec, String instance) {
        
    	List newList = new ArrayList();

        for (int i = 0; i < categories.length; i++) {
            String[] set = new String[3];
//          added DINI (OAI) Support
            if(categories[i].getLangArray().contains("x-dini")){
               	//ignore parentSpec, since it is specified in the label
            	set[0] = new String(categories[i].getText("x-dini"));
            	
            }
            else{
            	set[0] = new String(parentSpec + categories[i].getID());	
            }            
            set[1] = new String(categories[i].getText("en"));
            set[2] = new String(categories[i].getDescription("en"));

            if (categories[i].hasChildren()) {
            	logger.debug("Kategorie " + categories[i].getID() + " hat " + categories[i].getNumChildren() + " Kinder.");
                newList.addAll(getSets(categories[i].getChildren(), set[0] + ":", instance));
            }

            // We should better have a look if the set is empty...
            MCRLinkTableManager ltm = MCRLinkTableManager.instance();
            int numberOfLinks = ltm.countReferenceCategory(categories[i].getClassificationID(), categories[i].getID());

            if (numberOfLinks > 0) {
            	if(!set[0].equals("")){ //emtpy - dini - attributes shall be ignored
            		newList.add(set);
            		logger.debug("Der Gruppenliste wurde der Datensatz " + set[0] + " hinzugefügt.");
            	}
            }
        }

        return newList;
    }

    /**
     * Method listIdentifiers.Gets a list of identifiers
     * 
     * @param set
     *            the category (if known) is in the first element
     * @param from
     *            the date (if known) is in the first element
     * @param until
     *            the date (if known) is in the first element
     * @param metadataPrefix
     *            the requested metadata prefix
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the
     *         identifier, a datestamp (modification date) and a string with a
     *         blank separated list of categories the element is classified in
     */
    public List listIdentifiers(String[] set, String[] from, String[] until, String metadataPrefix, String instance) {
        return listRecordsOrIdentifiers(set, from, until, metadataPrefix, instance, false);
    }

    /**
     * Method getRecord. Gets a metadata record with the given <i>id </id>.
     * 
     * @param id
     *            The id of the object.
     * @param metadataPrefix
     *            the requested metadata prefix
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the
     *         identifier, a datestamp (modification date) and a string with a
     *         blank separated list of categories the element is classified in
     *         and a JDOM element with the metadata of the record
     */
    public List getRecord(String id, String metadataPrefix, String instance) {
        List list = new ArrayList();

        MCRObject object = new MCRObject();
        String repositoryId = null;
        try {
            repositoryId = MCROAIProvider.getConfigBean(instance).getRepositoryIdentifier();
            object.receiveFromDatastore(id);
        } catch (MCRConfigurationException mcrx) {
            return null;
        } catch (MCRException e) {
            return null;
        }

        String[] identifier = MCROAIProvider.getHeader(object, id, repositoryId, instance);
        list.add(identifier);
        logger.debug("Identifier hinzugefügt");

        Element eMetadata = (Element) object.createXML().getRootElement().clone();

        list.add(eMetadata);
        logger.debug("Metadaten hinzugefügt");

        return list;
    }

    /**
     * Method listRecords.Gets a list of metadata records
     * 
     * @param set
     *            the category (if known) is in the first element
     * @param from
     *            the date (if known) is in the first element
     * @param until
     *            the date (if known) is in the first element
     * @param metadataPrefix
     *            the requested metadata prefix
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the
     *         identifier, a datestamp (modification date) and a string with a
     *         blank separated list of categories the element is classified in
     */
    public List listRecords(String[] set, String[] from, String[] until, String metadataPrefix, String instance) {
        return listRecordsOrIdentifiers(set, from, until, metadataPrefix, instance, true);
    }

    private List listRecordsOrIdentifiers(String[] set, String[] from, String[] until, String metadataPrefix, String instance, boolean listRecords) {
        List list = new ArrayList();

        if (hasMore() && ((listRecords == lastQuery.equals("listRecords")) || (!listRecords == lastQuery.equals("listIdentifiers")))) {
            for (int i = deliveredResults; i < Math.min(maxReturns + deliveredResults, numResults); i++) {
                list.add(resultArray[i]);
            }
            deliveredResults = Math.min(maxReturns + deliveredResults, numResults);
            return list;
        }

        resetResults(listRecords ? "listRecords" : "listIdentifiers");

        // create query condition
        MCRAndCondition cAnd = new MCRAndCondition();

        String restriction = MCROAIProvider.getConfigBean(instance).getQueryRestriction();
        if (restriction != null)
            try {
                cAnd.addChild(new MCRQueryParser().parse(restriction));
            } catch (MCRException mcrx) {
                logger.warn("Error in adding OAI restriction: " + restriction, mcrx);
            }

        List searchFields = MCROAIProvider.getConfigBean(instance).getSearchFields();

        MCROrCondition cOr = new MCROrCondition();
        for (Iterator it = searchFields.iterator(); it.hasNext();) {
            String searchField = (String) it.next();
            MCRFieldDef field = MCRFieldDef.getDef(searchField);
            if (set == null) {
                cOr.addChild(new MCRQueryCondition(field, "like", ""));
            } else {
                String categoryId = set[0].substring(set[0].lastIndexOf(':') + 1);
                cOr.addChild(new MCRQueryCondition(field, "like", categoryId));
                generateQueryForDiniLabels(cOr, searchField, set[0], instance);
            }
        }
        if ((cOr.getChildren() != null) && (cOr.getChildren().size() > 0)) {
            cAnd.addChild(cOr);
        }

        MCRFieldDef field = MCRFieldDef.getDef("modified");
        if (from != null) {
            String date = getTimeStamp(from[0]);
            cAnd.addChild(new MCRQueryCondition(field, ">=", date));
        }

        if (until != null) {
            String date = getTimeStamp(until[0]);
            cAnd.addChild(new MCRQueryCondition(field, "<=", date));
        }

        MCRQuery query = new MCRQuery(cAnd);
        query.setSortBy(new MCRSortBy(MCRFieldDef.getDef("id"), MCRSortBy.ASCENDING));

        logger.debug("OAI-QUERY:" + cAnd);
        MCRResults results = MCRQueryManager.search(query);

        numResults = results.getNumHits();
        resultArray = new Object[numResults];
        logger.debug("OAIQuery found:" + numResults + " hits");
        deliveredResults = Math.min(maxReturns, numResults);
        logger.debug("deliveredResults:" + deliveredResults);
        for (int i = 0; i < numResults; i++) {
            resultArray[i] = results.getHit(i).getID();
        }
        for (int i = 0; i < deliveredResults; i++) {
            list.add(resultArray[i]);
        }

        return list;
    }

    /**
     * Method hasMore.
     * 
     * @return true, if more results for the last query exists, else false
     */
    public boolean hasMore() {
        return deliveredResults < numResults;
    }

    private String getTimeStamp(String isoDate) {
        int len = isoDate.length();
        if (len == 10) {
            return isoDate + " 00:00:00";
        } else if (len == 20) {
            return isoDate.substring(0, 10) + " " + isoDate.substring(11, 19);
        }
        logger.warn("unallowed iso date format:" + isoDate);
        return null;
    }

    private void resetResults(String query) {
        deliveredResults = 0;
        numResults = 0;
        resultArray = null;
        lastQuery = query;
    }
    
    private void generateQueryForDiniLabels(MCROrCondition cOr,
			String searchField, String set, String instance) {
		//expected searchfields:  "format",  "type",      "subject"
		//mapping to DINI sets:   "doc-type", "pub-type", "ddc"
		MCRFieldDef field = MCRFieldDef.getDef(searchField);

		String[] classification = MCROAIProvider.getConfigBean(instance)
				.getClassificationIDsForSearchField(searchField);

		for (int i = 0; i < classification.length; i++) {
			MCRClassificationItem repository = MCRClassificationItem.getClassificationItem(classification[i]);
			org.jdom.Document jDomDoc = repository.receiveClassificationAsJDOM();
			try {
				//could be improved: return only <label> under a <categegory> but //category/label[..] does not work here
				XPath xpathExpr = XPath.newInstance("//label[@xml:lang='x-dini' and @text='"+ set + "']/..//@ID");
				List resultList = xpathExpr.selectNodes(jDomDoc);
				for (int j = 0; j < resultList.size(); j++) {
					if (resultList.get(j) instanceof Attribute) {
						cOr.addChild(new MCRQueryCondition(field, "like", ((Attribute) resultList.get(j)).getValue()));
					}
				}
			} catch (JDOMException e) {
				logger.error(e);
			}

		}
	}
}
