/*
 * $RCSfile: MCRConfiguration.java,v $
 * $Revision: 1.25 $ $Date: 2005/09/02 14:26:23 $
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

package org.mycore.services.oai;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.query.MCRQueryCache;
import org.mycore.services.query.MCRQueryCollector;

/**
 * @author Werner Gresshoff
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision: 1.10 $ $Date: 2003/01/31 11:56:25 $
 * @deprecated
 * 
 * This is the MyCoRe-Implementation of the <i>MCROAIQuery </i>-Interface.
 */
public class MCROAIQueryService implements MCROAIQuery {
    static Logger logger = Logger.getLogger(MCROAIQueryService.class.getName());

    static MCRQueryCollector collector;

    private static final String STR_OAI_MAXRETURNS = "MCR.oai.maxreturns";

    // maximum
    // number
    // of
    // returned
    // list
    // sets
    private static final String STR_OAI_RESTRICTION_CLASSIFICATION = "MCR.oai.restriction.classification";

    // Classification
    // and...
    private static final String STR_OAI_RESTRICTION_CATEGORY = "MCR.oai.restriction.category";

    // ...Category
    // to
    // restrict
    // the
    // access
    // to
    private static final String STR_OAI_SETSCHEME = "MCR.oai.setscheme";

    // the
    // classification
    // id
    // which
    // serves
    // as
    // scheme
    // for
    // the
    // OAI
    // set
    // structure
    private static final String STR_OAI_REPOSITORY_IDENTIFIER = "MCR.oai.repositoryidentifier";

    // Identifier
    // of
    // the
    // repository
    private static final String STR_OAI_QUERYTYPE = "MCR.oai.querytype";

    // search
    // query
    // type
    private static MCRConfiguration config;

    private static int maxReturns;

    private int deliveredResults = 0;

    private int numResults = 0;

    private String lastQuery = "";

    private Object[] resultArray;

    static {
        config = MCRConfiguration.instance();
        maxReturns = config.getInt(STR_OAI_MAXRETURNS);
    }

    /**
     * Method MCROAIQueryService.
     */
    public MCROAIQueryService() {
        if (collector == null) {
            int cThreads = config.getInt("MCR.Collector_Thread_num", 2);
            int aThreads = config.getInt("MCR.Agent_Thread_num", 6);
            collector = new MCRQueryCollector(cThreads, aThreads);
        }
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
     * @param instance
     *            the Servletinstance
     * @return List A list that contains an array of three Strings: the category
     *         id, the label and a description
     */
    public List listSets(String instance) {
        String classificationId = config.getString(STR_OAI_SETSCHEME + "." + instance, "");
        String[] classification;

        if (classificationId.length() == 0) {
            logger.debug("Suche in allen Klassifikationen");
            classification = MCRClassification.getAllClassificationID();
        } else {
            logger.debug("Suche in Klassifikationen: " + classificationId);

            StringTokenizer tokenizer = new StringTokenizer(classificationId, " ");
            classification = new String[tokenizer.countTokens()];

            int i = 0;

            while (tokenizer.hasMoreTokens()) {
                classification[i++] = tokenizer.nextToken();
            }
        }

        List list = new ArrayList();

        for (int i = 0; i < classification.length; i++) {
            MCRClassificationItem repository = MCRClassificationItem.getClassificationItem(classification[i]);
            MCRCategoryItem[] children = repository.getChildren();
            logger.debug("ClassificationItem " + repository.getClassificationID() + " hat " + children.length + " Kinder.");

            if ((repository != null) && (repository.hasChildren())) {
                logger.debug("ClassificationItem hat " + repository.getNumChildren() + " Kinder.");
                list.addAll(getSets(repository.getChildren(), "", instance));
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
            set[0] = new String(parentSpec + categories[i].getID());
            set[1] = new String(categories[i].getText("en"));
            set[2] = new String(categories[i].getDescription("en"));

            logger.debug("Suche nach Kategorie: " + categories[i].getID());

            if (categories[i].hasChildren()) {
                logger.debug("Kategorie " + categories[i].getID() + " hat " + categories[i].getNumChildren() + " Kinder.");
                newList.addAll(getSets(categories[i].getChildren(), set[0] + ":", instance));
            }

            // We should better have a look if the set is empty...
            MCRLinkTableManager ltm = MCRLinkTableManager.instance();
            int numberOfLinks = ltm.countReferenceCategory(categories[i].getClassificationID(), categories[i].getID());

            if (numberOfLinks > 0) {
                newList.add(set);
                logger.debug("Der Gruppenliste wurde der Datensatz " + set[0] + " hinzugefügt.");
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
        return listRecordsOrIdentifiers(set, from, until, instance, false);
    }

    /**
     * Method getHeader. Gets the header information from the MCRObject
     * <i>object </i>.
     * 
     * @param object
     *            The MCRObject
     * @param objectId
     *            The objectId as String representation
     * @param repositoryId
     *            The repository id
     * @return String[] Array of three Strings: the identifier, a datestamp
     *         (modification date) and a string with a blank separated list of
     *         categories the element is classified in
     */
    private String[] getHeader(MCRObject object, String objectId, String repositoryId, String instance) {
        Date date = object.getService().getDate("modifydate");

        // Format the date.
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        String datestamp = formatter.format(date);

        StringBuffer setSpec = new StringBuffer("");
        String[] identifier = new String[3];
        identifier[0] = "oai:" + repositoryId + ":" + objectId;
        identifier[1] = datestamp;
        identifier[2] = new String("");

        List classifications = Arrays.asList(getClassifications(instance));

        for (int j = 0; j < object.getMetadata().size(); j++) {
            if (object.getMetadata().getMetadataElement(j).getClassName().equals("MCRMetaClassification")) {
                MCRMetaElement element = object.getMetadata().getMetadataElement(j);

                for (int k = 0; k < element.size(); k++) {
                    MCRMetaClassification classification = (MCRMetaClassification) element.getElement(k);
                    String classificationId = classification.getClassId();

                    if (classifications.contains(classificationId)) {
                        String categoryId = classification.getCategId();
                        MCRCategoryItem category = MCRCategoryItem.getCategoryItem(classificationId, categoryId);
                        MCRCategoryItem parent;

                        while ((parent = category.getParent()) != null) {
                            categoryId = parent.getID() + ":" + categoryId;
                            category = parent;
                        }

                        setSpec.append(" ").append(categoryId);
                    }
                }

                identifier[2] = setSpec.toString().trim();
            }
        }

        return identifier;
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
            repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);
            object.receiveFromDatastore(id);
        } catch (MCRConfigurationException mcrx) {
        	logger.error("catched error", mcrx);
            return null;
        } catch (MCRException e) {
        	logger.error("catched error", e);
            return null;
        }

        String[] identifier = getHeader(object, id, repositoryId, instance);
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
        return listRecordsOrIdentifiers(set, from, until, instance, true);
    }

    /**
     * @param instance
     * @return all classification id's from configuration file
     * @throws MCRConfigurationException
     */
    private String[] getClassifications(String instance) throws MCRConfigurationException {
        String[] classification;
        String id = config.getString(STR_OAI_SETSCHEME + "." + instance);

        if (id.length() == 0) {
            logger.debug("Suche in allen Klassifikationen");
            classification = MCRClassification.getAllClassificationID();
        } else {
            logger.debug("Suche in Klassifikationen: " + id);

            StringTokenizer tokenizer = new StringTokenizer(id, " ");
            classification = new String[tokenizer.countTokens()];

            int i = 0;

            while (tokenizer.hasMoreTokens()) {
                classification[i++] = tokenizer.nextToken();
            }
        }

        return classification;
    }

    private Collection doQuery(String query, String querytype) {
        List results = new ArrayList();

        try {
            MCRXMLContainer qra = new MCRXMLContainer();
            qra.importElements(MCRQueryCache.getResultList("local", query, querytype, MCRConfiguration.instance().getInt("MCR.query_max_results", 10)));

            for (int j = 0; j < qra.size(); j++) {
                results.add(qra.getId(j));
            }
        } catch (Exception mcrx) {
            logger.error("Die Query ist fehlgeschlagen.");
        }

        return results;
    }

    private List listRecordsOrIdentifiers(String[] set, String[] from, String[] until, String instance, boolean listRecords) {
        List list = new ArrayList();
        List queries = new ArrayList();
        List restrictionQueries = new ArrayList();
        String repositoryId = null;
        String querytype = null;

        if (hasMore() && ((listRecords == lastQuery.equals("listRecords")) || (!listRecords == lastQuery.equals("listIdentifiers")))) {
            for (int i = deliveredResults; i < Math.min(maxReturns + deliveredResults, numResults); i++) {
                String objectId = (String) resultArray[i];
                MCRObject object = new MCRObject();
                try {
                    repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);
                    object.receiveFromDatastore(objectId);
                } catch (Exception e) {
                    logger.error("error stacktrace", e);
                }                

                String[] identifier = getHeader(object, objectId, repositoryId, instance);
                list.add(identifier);

                if (listRecords) {
                    Element eMetadata = (Element) object.createXML().getRootElement().clone();
                    list.add(eMetadata);
                }
            }

            deliveredResults = Math.min(maxReturns + deliveredResults, numResults);

            return list;
        }

        resetResults(listRecords ? "listRecords" : "listIdentifiers");

        try {
            String restrictionClassification = config.getString(STR_OAI_RESTRICTION_CLASSIFICATION + "." + instance);
            String restrictionCategory = config.getString(STR_OAI_RESTRICTION_CATEGORY + "." + instance);
            StringBuffer query = new StringBuffer("");

            query.append("/mycoreobject[metadata/*/*[@classid=\"").append(restrictionClassification).append("\" and @categid=\"").append(restrictionCategory).append("\"] ]");

            restrictionQueries.add(query.toString());
        } catch (MCRConfigurationException mcrx) {
        }

        String[] classification;

        try {
            classification = getClassifications(instance);
            repositoryId = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + instance);
            querytype = config.getString(STR_OAI_QUERYTYPE + "." + instance);
        } catch (MCRConfigurationException mcrx) {
            return list;
        }

        for (int i = 0; i < classification.length; i++) {
            StringBuffer query = new StringBuffer("");

            if (set == null) {
                query.append("/mycoreobject[metadata/*/*[@classid=\"").append(classification[i]).append("\"] ]");
            } else {
                String categoryId = set[0].substring(set[0].lastIndexOf(':') + 1);
                query.append("/mycoreobject[metadata/*/*[@classid=\"").append(classification[i]).append("\" and @categid=\"").append(categoryId).append("\"] ]");
            }

            queries.add(query.toString());
        }

        if (from != null) {
            StringBuffer query = new StringBuffer("");

            String date = from[0];
            query.append("/mycoreobject[service/servdates/servdate[text()>=\"").append(date).append("\" and @type=\"modifydate\"] ]");

            restrictionQueries.add(query.toString());
        }

        if (until != null) {
            StringBuffer query = new StringBuffer("");

            String date = until[0];
            query.append("/mycoreobject[service/servdates/servdate[text()<=\"").append(date).append("\" and @type=\"modifydate\"] ]");

            restrictionQueries.add(query.toString());
        }

        Set results = new HashSet();

        for (int i = 0; i < queries.size(); i++) {
            String query = (String) queries.get(i);
            results.addAll(doQuery(query, querytype));
        }

        for (int i = 0; i < restrictionQueries.size(); i++) {
            String query = (String) restrictionQueries.get(i);
            results.retainAll(doQuery(query, querytype));
        }

        numResults = results.size();
        deliveredResults = Math.min(maxReturns, numResults);
        resultArray = results.toArray();

        for (int i = 0; i < deliveredResults; i++) {
            String objectId = (String) resultArray[i];
            MCRObject object = new MCRObject();
            object.receiveFromDatastore(objectId);

            String[] identifier = getHeader(object, objectId, repositoryId, instance);
            list.add(identifier);

            if (listRecords) {
                Element eMetadata = (Element) object.createXML().getRootElement().clone();
                list.add(eMetadata);
            }
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

    private void resetResults(String query) {
        deliveredResults = 0;
        numResults = 0;
        resultArray = null;
        lastQuery = query;
    }
}
