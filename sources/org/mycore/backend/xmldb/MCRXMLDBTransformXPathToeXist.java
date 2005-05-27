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
package org.mycore.backend.xmldb;

import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.MCRMetaSearchInterface;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.modules.XPathQueryService;

/**
 * This is the implementation of the MCRMetaSearchInterface for the XML:DB API
 * 
 * @author Marc Schluepmann
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRXMLDBTransformXPathToeXist implements MCRMetaSearchInterface {

    /** The default query * */
    public static final String DEFAULT_QUERY = "/*";

    // the logger
    protected static Logger logger = Logger
            .getLogger(MCRXMLDBTransformXPathToeXist.class.getName());

    private MCRConfiguration config = null;

    private String database;

    /**
     * The constructor.
     */
    public MCRXMLDBTransformXPathToeXist() {
        config = MCRConfiguration.instance();
        MCRXMLDBConnectionPool.instance();
        database = config.getString("MCR.persistence_xmldb_database", "");
        logger.debug("MCR.persistence_xmldb_database    : " + database);
    }

    /**
     * This method start the Query over the XML:DB persitence layer for one
     * object type and and return the query result as HashSet of MCRObjectIDs.
     * 
     * @param root
     *            the query root
     * @param query
     *            the metadata queries
     * @param type
     *            the MCRObject type
     * @return a result list as MCRXMLContainer
     */
    public final HashSet getResultIDs(String root, String query, String type) {
        // prepare the query over the rest of the metadata
        HashSet idmeta = new HashSet();
        logger.debug("Incomming condition : " + query);
        String newquery = "";
        if ((root == null) && (query.length() == 0)) {
            newquery = DEFAULT_QUERY;
        }
        if (database.equals("exist") && (query.length() != 0)) {
            newquery = handleQueryStringExist(root, query, type);
        }
        if (database.equals("tamino") && (query.length() != 0)) {
            newquery = handleQueryStringTamino(root, query, type);
        }
        logger.debug("Transformed query : " + newquery);

        // do it over the metadata
        if (newquery.length() != 0) {
            try {
                Collection collection = MCRXMLDBConnectionPool.instance()
                        .getConnection(type);
                XPathQueryService xps = (XPathQueryService) collection
                        .getService("XPathQueryService", "1.0");

                ResourceSet resultset = xps.query(newquery);
                logger.debug("Results: "
                        + Integer.toString((int) resultset.getSize()));
                ResourceIterator ri = resultset.getIterator();
                while (ri.hasMoreResources()) {
                    //doc = MCRXMLDBPersistence.convertResToDoc(xmldoc);
                    //OK we simply asume that all docs are well in exist
                    //and their ID is our ObjectID :o)
                    //ID=ObjectID+"_1" we remove the "_1" now
                    StringTokenizer tok = new StringTokenizer(ri.nextResource()
                            .getId(), "_");
                    idmeta.add(new MCRObjectID(
                            new StringBuffer(tok.nextToken()).append('_')
                                    .append(tok.nextToken()).append('_')
                                    .append(tok.nextToken()).toString()));
                }
            } catch (Exception e) {
                throw new MCRPersistenceException(e.getMessage(), e);
            }
        }
        return idmeta;
    }

    /**
     * Handle query string for exist
     */
    private String handleQueryStringExist(String root, String query, String type) {
        query = MCRUtils.replaceString(query, "#####", "");
        query = MCRUtils.replaceString(query, "like", "&=");
        query = MCRUtils.replaceString(query, "text()", ".");
        query = MCRUtils.replaceString(query, "ts()", ".");
        query = MCRUtils.replaceString(query, "contains(", "&=");
        query = MCRUtils.replaceString(query, "contains (", "&=");
        query = MCRUtils.replaceString(query, ")", "");

        // Workaround for interpreting categids as String
        // insert a dummy "X" before the following strange operation
        // and delete it at the end again
        query = query.replaceAll("@categid[ ]*([&]{0,1}=)[ ]*\"(.+)\"",
                "@categid$1\"X$2\"");

        // select numbers and remove ""
        int i = 0;
        int l = query.length();
        double k = 0;
        while (i < l) {
            i = query.indexOf("\"", i);
            if (i != -1) {
                int j = query.indexOf("\"", i + 1);
                if (j != -1) {
                    try {
                        k = Double.parseDouble(query.substring(i + 1, j)
                                .replace(',', '.'));
                        StringBuffer sb = new StringBuffer(1024);
                        sb.append(query.substring(0, i));
                        if (k == Math.floor(k)) {
                            int m = (int) k;
                            sb.append(m);
                        } else {
                            String s = Double.toString(k).replace(',', '.');
                            sb.append(s);
                        }
                        sb.append(query.substring(j + 1, l));
                        query = sb.toString();
                        l = sb.length();
                    } catch (Exception e) {
                        String s = MCRUtils.covertDateToISO(query.substring(
                                i + 1, j));
                        StringBuffer sb = new StringBuffer(1024);
                        if (s != null) {
                            sb.append(query.substring(0, i + 1));
                            sb.append(s);
                            sb.append(query.substring(j, l));
                            query = sb.toString();
                            l = sb.length();
                        }
                    }
                    i = j + 1;
                }
                i = i + 1;
            } else {
                break;
            }
        }
        // Workaround for interpreting categids as String
        // remove the above inserted dummy "X"
        // from the category ID
        query = query.replaceAll("@categid[ ]*([&]{0,1}=)[ ]*\"X(.+)\"",
                "@categid $1\"$2\"");
        // fix a eXist-Bug
        // for handling the correct
        // xpath-query
        //  "mycoreobject[metadata/*/*[@classid="DocPortal_class_000000000000006"
        //		and @categid="510"] ]
        if (root.equals("/mycoreobject")
                && query.matches("\\A[ ]*metadata/[*]{1}/[*]{1}.*")) {
            query = root + "/" + query.trim();
        } else {
            // combine the separated queries
            query = root + "[" + query + "]";
        }
        return query;
    }

    /**
     * Handle query string for Tamino
     */
    private String handleQueryStringTamino(String root, String query,
            String type) {
        query = MCRUtils.replaceString(query, "#####", "");
        query = MCRUtils.replaceString(query, "like", "~="); // 030919
        query = MCRUtils.replaceString(query, ")", "");
        query = MCRUtils.replaceString(query, "\"", "'");
        query = MCRUtils.replaceString(query, "contains(", "~=");
        query = MCRUtils.replaceString(query, "metadata/*/*/@href=",
                "metadata//@xlink:href=");

        if (-1 != query.indexOf("] and")) {
            query = MCRUtils.replaceString(query, "] and /mycoreobject[",
                    " and /mycoreobject/"); // 031002
        }

        query = root + "[" + query + "]";
        return query;
    }
}