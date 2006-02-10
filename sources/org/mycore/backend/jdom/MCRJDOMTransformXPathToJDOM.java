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

package org.mycore.backend.jdom;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRNormalizer;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.query.MCRMetaSearchInterface;

/**
 * This is the implementation of the MCRMetaSearchInterface for the JDOM tree
 * 
 * @author Jens Kupferschmidt
 * @author Frank L?tzenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRJDOMTransformXPathToJDOM implements MCRMetaSearchInterface {
    /** The transformer factory */
    private javax.xml.transform.TransformerFactory transfakt = null;

    /** The default query * */
    public static final String DEFAULT_QUERY = "mycoreobject";

    /** the logger * */
    protected static Logger logger = Logger.getLogger(MCRJDOMTransformXPathToJDOM.class.getName());

    /**
     * The constructor.
     */
    public MCRJDOMTransformXPathToJDOM() {
        transfakt = javax.xml.transform.TransformerFactory.newInstance();
    }

    /**
     * This method start the Query over the memory search store for one object
     * type and returns the query result as HashSet of MCRObjectIDs.
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
        logger.debug("Incoming condition : " + query);

        // prepare the query over the rest of the metadata
        long t1 = System.currentTimeMillis();

        String xsltquery = DEFAULT_QUERY;

        if ((root != null) || (query.length() > 0)) {
            xsltquery = getXSLTArgument(query);
        }

        logger.debug("Transformed query : " + xsltquery);

        // prepare the stylesheet for query
        MCRJDOMMemoryStore store = MCRJDOMMemoryStore.instance();
        Hashtable objects = store.getObjects(type);
        logger.debug("" + objects.size() + " objects of type " + type + " available");

        org.jdom.Document stylesheet = store.getStylesheet(xsltquery);

        javax.xml.transform.Transformer trans = null;

        try {
            trans = transfakt.newTransformer(new org.jdom.transform.JDOMSource(stylesheet));
        } catch (Exception ex) {
            String msg = "Error building XSL transformer for JDOM search store";
            throw new MCRException(msg, ex);
        }

        long t2 = System.currentTimeMillis();
        double diff = (t2 - t1) / 1000.0;
        logger.debug("Prepare stylesheet in " + diff + " sec");

        // execute the query
        HashSet idmeta = new HashSet();
        Enumeration ids = objects.keys();

        while (ids.hasMoreElements()) {
            MCRObjectID id = (MCRObjectID) (ids.nextElement());
            Element obj = (Element) (objects.get(id));
            org.jdom.transform.JDOMResult jdomres = new org.jdom.transform.JDOMResult();

            try {
                trans.transform(new org.jdom.transform.JDOMSource(obj), jdomres);
            } catch (TransformerException ex) {
                String msg = "Error executing XSL query";
                throw new MCRException(msg, ex);
            }

            String result = jdomres.getDocument().getRootElement().getTextTrim();

            if ("true".equals(result)) {
                idmeta.add(id);
            }
        }

        long t3 = System.currentTimeMillis();
        diff = (t3 - t2) / 1000.0;
        logger.debug("Executed the XALAN transformation in " + diff + " s");

        return idmeta;
    }

    /**
     * The method transform the XPath query to a XSLT argument.
     * 
     * @param query
     *            the XPath query
     * @return the XSLT argument
     */
    private final String getXSLTArgument(String query) {
        if (query.equals("*")) {
            return DEFAULT_QUERY;
        }

        // replace the duck feets
        String newquery = MCRUtils.replaceString(query, "\"", "'");

        // separate the single queries
        StringBuffer cond = new StringBuffer(1024);
        String ss = "";
        int i = 0;
        int j = 0;
        int l = query.length();

        while (i < l) {
            j = newquery.indexOf("#####", i);

            if (j == -1) {
                try {
                    cond.append(' ').append(traceOneCondition(newquery.substring(i, l)));
                } catch (MCRException me) {
                    logger.error(me.getMessage());
                }

                break;
            }

            ss = newquery.substring(i, j);

            if (ss.trim().length() == 0) {
                i = j + 5;

                continue;
            }

            if (ss.equals(" and ")) {
                cond.append(ss);
            } else {
                if (ss.equals(" or ")) {
                    cond.append(ss);
                } else {
                    try {
                        cond.append(' ').append(traceOneCondition(ss));
                    } catch (MCRException me) {
                        cond.append(' ');
                    }
                }
            }

            i = j + 5;
        }

        // build the XSLT argument
        if (cond.toString().trim().length() == 0) {
            return DEFAULT_QUERY;
        }

        return (new StringBuffer(DEFAULT_QUERY)).append('[').append(cond.toString()).append(']').toString();
    }

    /**
     * This is a private routine they trace one condition.
     * 
     * @param condstr
     * one single condition @ return the transfromed query for XSLT.
     */
    private final String traceOneCondition(String condstr) {
        // search operations
        int maxcount = 10;
        String[] pathin = new String[maxcount];
        String[] pathout = new String[maxcount];
        String[] tag = new String[maxcount];
        String[] op = new String[maxcount];
        String[] value = new String[maxcount];
        String[] bool = new String[maxcount];
        int counter = 0;
        boolean klammer = false;

        // search for []
        String cond = "";
        int i = condstr.indexOf("[");

        if (i != -1) {
            int j = condstr.indexOf("]");

            if (j == -1) {
                throw new MCRPersistenceException("Error while analyze the query string.");
            }

            klammer = true;
            cond = condstr.substring(i + 1, j);

            String p = condstr.substring(0, i);

            for (int k = 0; k < maxcount; k++) {
                pathout[k] = p;
                pathin[k] = "";
            }
        } else {
            for (int k = 0; k < maxcount; k++) {
                pathin[k] = "";
                pathout[k] = "";
            }

            cond = condstr;
        }

        // logger.debug("Condition in traceOneCondition : " + cond);
        // analyze cond
        int tippelauf = 0;
        int tippelzu = 0;
        int tippelauf1 = 0;
        int tippelauf2 = 0;
        int tagstart = 0;
        int opstart = 0;

        while ((tippelauf != -1) && (tippelzu != -1)) {
            tippelauf1 = cond.indexOf("\"", tippelzu + 1);
            tippelauf2 = cond.indexOf("'", tippelzu + 1);

            if (tippelauf1 != -1) {
                tippelauf = tippelauf1;
                tippelzu = cond.indexOf("\"", tippelauf + 1);

                if (tippelzu == -1) {
                    break;
                }
            } else {
                if (tippelauf2 != -1) {
                    tippelauf = tippelauf2;
                    tippelzu = cond.indexOf("'", tippelauf + 1);

                    if (tippelzu == -1) {
                        break;
                    }
                } else {
                    break;
                }
            }

            value[counter] = new String(cond.substring(tippelauf + 1, tippelzu).trim());

            boolean opset = false;

            if (!opset) {
                opstart = cond.toUpperCase().indexOf("CONTAINS(", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "contains";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.toUpperCase().indexOf("LIKE", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "like";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf("!=", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "!=";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf(">=", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = ">=";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf("<=", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "<=";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf("=", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "=";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf("<", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = "<";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                opstart = cond.indexOf(">", tagstart);

                if ((opstart != -1) && (opstart < tippelauf)) {
                    op[counter] = ">";
                    tag[counter] = cond.substring(tagstart, opstart).trim();
                    opset = true;
                }
            }

            if (!opset) {
                return "";
            }

            bool[counter] = "";

            if ((tippelzu + 5) < cond.length()) {
                tagstart = cond.toLowerCase().indexOf(" and ", tippelzu + 1);

                if (tagstart == -1) {
                    tagstart = cond.toLowerCase().indexOf(" or ", tippelzu + 1);

                    if (tagstart == -1) {
                        return "";
                    }

                    tagstart += 4;
                    bool[counter] = " or ";
                } else {
                    tagstart += 5;
                    bool[counter] = " and ";
                }
            }

            // has the tag a path (if true split them)
            StringBuffer sbpath = new StringBuffer("");
            int j = 0;
            int l;
            int lastl = 0;
            int k = tag[counter].length();

            while (j < k) {
                l = tag[counter].indexOf("/", j);

                if (l == -1) {
                    String nt = "";

                    if (tag[counter].charAt(j) == '@') {
                        nt = tag[counter].substring(j, tag[counter].length());
                    }

                    if (tag[counter].charAt(j) == '*') {
                        nt = "text()";
                    }

                    if (tag[counter].indexOf("ts()", j) != -1) {
                        nt = "text()";
                    }

                    if (tag[counter].indexOf("text()", j) != -1) {
                        nt = "text()";
                    }

                    if (nt.length() == 0) {
                        nt = "text()";

                        if (lastl != 0) {
                            sbpath.append('/');
                        }

                        sbpath.append(tag[counter].substring(j, tag[counter].length()));
                    }

                    if (sbpath.length() != 0) {
                        pathin[counter] = sbpath.toString();
                    } else {
                        pathin[counter] = "";
                    }

                    tag[counter] = nt;

                    break;
                }

                if (lastl != 0) {
                    sbpath.append('/');
                }

                sbpath.append(tag[counter].substring(j, l));
                lastl = l;
                j = l + 1;
            }

            // increment the counter
            counter++;
        }

        // debug

        /*
         * for (i=0;i <counter;i++) { logger.debug("PATHOUT="+pathout[i]);
         * logger.debug("PATHIN="+pathin[i]); logger.debug("TAG="+tag[i]);
         * logger.debug("OPER="+op[i]); logger.debug("VALUE="+value[i]);
         * logger.debug("BOOLEAN="+bool[i]); logger.debug(""); }
         */
        StringBuffer sbout = new StringBuffer("");

        // if we have a common path
        if (klammer) {
            sbout.append(pathout[0]).append('[');
        }

        // Loop over counter
        for (i = 0; i < counter; i++) {
            // replace text() with .
            String newtag = MCRUtils.replaceString(tag[i], "text()", ".");

            // contains
            if (op[i].equals("contains") || op[i].equals("like")) {
                sbout.append("contains(");

                if (pathin[i].length() != 0) {
                    sbout.append(pathin[i]).append('/');
                }

                if (op[i].equals("contains")) {
                    sbout.append(newtag).append(",\'").append(MCRNormalizer.normalizeString(MCRUtils.replaceString(value[i], "*", ""))).append("\')");
                } else {
                    sbout.append(newtag).append(",\'").append(MCRUtils.replaceString(value[i], "*", "")).append("\')");
                }

                sbout.append(bool[i]);

                continue;
            }

            // date
            String test = MCRUtils.covertDateToISO(value[i]);

            if (test != null) {
                value[i] = test;
                sbout.append("java:org.mycore.backend.jdom.MCRXalanExtensions.compareDates(");

                if (pathin[i].length() != 0) {
                    sbout.append(pathin[i]).append('/');
                }

                sbout.append(newtag).append(",\'").append(value[i]).append("\',\'").append(op[i]).append("\') ").append(bool[i]);

                continue;
            }

            // numerical
            if (op[i].equals("<") || op[i].equals(">") || op[i].equals("<=") || op[i].equals(">=")) {
                if (pathin[i].length() != 0) {
                    sbout.append(pathin[i]).append('/');
                }

                sbout.append(newtag).append(' ').append(op[i]).append(' ').append(value[i]).append(' ').append(bool[i]);

                continue;
            }

            // general
            if (pathin[i].length() != 0) {
                sbout.append(pathin[i]).append('/');
            }

            sbout.append(newtag).append(' ').append(op[i]).append(" \'").append(value[i]).append("\'").append(bool[i]);
        }

        // if we have a common path
        if (klammer) {
            sbout.append(']');
        }

        return sbout.toString();
    }
}
