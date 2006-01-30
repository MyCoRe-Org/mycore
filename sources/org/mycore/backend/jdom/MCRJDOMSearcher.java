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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRQueryCondition;

/**
 * Implements a searcher and indexer for MCRObject metadata using only data in
 * memory without any persistent structures. When data is indexed, the values
 * are stored as XML document in memory. When data is searched, the query is
 * transformed to a XSL condition and run against the XML in memory. This class
 * may also be useful for learning how to implement MCRSearchers and indexers.
 * 
 * TODO: read metadata of all stored MCRObjects at startup
 * 
 * @author Frank Lützenkirchen
 */
public class MCRJDOMSearcher extends MCRSearcher {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRJDOMSearcher.class);

    /**
     * Map where key is entryID and value is XML document containing indexed
     * data
     */
    private HashMap map = new HashMap();

    /** XSL transformer factory */
    private TransformerFactory factory = TransformerFactory.newInstance();

    protected void addToIndex(String entryID, List fields) {
        LOGGER.info("MCRJDOMSearcher indexing data of " + entryID);

        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        Element root = new Element("data");

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (fields.get(i));
            Element e = new Element(fv.getField().getName());
            e.addContent(fv.getValue());
            root.addContent(e);
        }

        if (LOGGER.isDebugEnabled()) {
            String s = new XMLOutputter(Format.getPrettyFormat()).outputString(root);
            LOGGER.debug("----------" + entryID + "----------");
            LOGGER.debug(s);
            LOGGER.debug("-----------------------------------");
        }

        map.put(entryID, new Document(root));
    }

    protected void removeFromIndex(String entryID) {
        LOGGER.info("MCRJDOMSearcher removing indexed data of " + entryID);
        map.remove(entryID);
    }

    public MCRResults search(MCRCondition cond, List order, int maxResults) {
        String xslCondition = buildXSLCondition(cond);
        LOGGER.debug("MCRJDOMSearcher searching for " + xslCondition);

        if (xslTemplate == null) {
            xslTemplate = prepareStylesheet();
        }

        Document xsl = buildStylesheet(xslCondition);

        MCRResults results = new MCRResults();
        java.util.Iterator keys = map.keySet().iterator();

        while (keys.hasNext()) {
            String entryID = (String) (keys.next());
            Document xml = (Document) (map.get(entryID));

            if (matches(xml, xsl)) {
                results.addHit(new MCRHit(entryID));
            }

            if ((maxResults > 0) && (results.getNumHits() >= maxResults)) {
                break;
            }
        }

        LOGGER.debug("MCRJDOMSearcher results completed");

        return results;
    }

    /**
     * Returns true if the xml input document matches the xsl when condition in
     * the xsl stylesheet.
     */
    private boolean matches(Document xml, Document xsl) {
        Source xmlsrc = new JDOMSource(xml);
        Source xslsrc = new JDOMSource(xsl);

        try {
            Transformer transformer = factory.newTransformer(xslsrc);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(xmlsrc, new StreamResult(out));
            out.close();

            return "t".equals(out.toString("UTF-8"));
        } catch (Exception ex) {
            LOGGER.warn("Exception while testing indexed data with XSL condition", ex);

            return false;
        }
    }

    /**
     * XSL stylesheet template where only the when test attribute has to be
     * added
     */
    private Document xslTemplate = null;

    /** Prepares an XSL stylesheet in memory used as template */
    private Document prepareStylesheet() {
        Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
        Namespace xalanns = Namespace.getNamespace("xalan", "http://xml.apache.org/xalan");
        Namespace extns = Namespace.getNamespace("ext", "xalan://org.mycore.backend.jdom.MCRJDOMSearcher");

        Element stylesheet = new Element("stylesheet");
        stylesheet.setAttribute("version", "1.0");
        stylesheet.setNamespace(xslns);
        stylesheet.addNamespaceDeclaration(xalanns);
        stylesheet.addNamespaceDeclaration(extns);
        stylesheet.setAttribute("extension-element-prefixes", "ext");

        Element output = new Element("output", xslns);
        output.setAttribute("method", "text");
        stylesheet.addContent(output);

        Element template = new Element("template", xslns);
        template.setAttribute("match", "/data");
        stylesheet.addContent(template);

        Element choose = new Element("choose", xslns);
        template.addContent(choose);

        Element when = new Element("when", xslns);
        when.addContent("t");

        Element otherwise = new Element("otherwise", xslns);
        otherwise.addContent("f");
        choose.addContent(when).addContent(otherwise);

        return new Document(stylesheet);
    }

    /** Adds the condition as xsl when test attribute to the stylesheet template */
    private Document buildStylesheet(String condition) {
        Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
        Document xsl = (Document) (xslTemplate.clone());
        xsl.getRootElement().getChild("template", xslns).getChild("choose", xslns).getChild("when", xslns).setAttribute("test", condition);

        return xsl;
    }

    /** Converter from MCRCondition to XSL test condition */
    private String buildXSLCondition(MCRCondition cond) {
        if (cond instanceof MCRQueryCondition) {
            MCRQueryCondition sc = (MCRQueryCondition) cond;
            StringBuffer sb = new StringBuffer(sc.getField().getName());
            sb.append("[");

            if ("= < > <= >=".indexOf(sc.getOperator()) >= 0) {
                String type = sc.getField().getDataType();

                if ("integer".equals(type) || "decimal".equals(type)) {
                    sb.append("number(text()) ");
                    sb.append(sc.getOperator());
                    sb.append(" ");
                    sb.append(sc.getValue());
                } else {
                    sb.append("text() ");
                    sb.append(sc.getOperator());
                    sb.append(" '");
                    sb.append(sc.getValue());
                    sb.append("'");
                }
            } else if ("phrase".equals(sc.getOperator())) {
                sb.append("contains(text(),'");
                sb.append(sc.getValue()).append("')");
            } else if ("contains".equals(sc.getOperator())) {
                sb.append("ext:contains(text(),'");
                sb.append(sc.getValue()).append("')");
            } else if ("like".equals(sc.getOperator())) {
                sb.append("ext:like(text(),'");
                sb.append(sc.getValue()).append("')");
            }

            sb.append("]");

            return sb.toString();
        } else if (cond instanceof MCRNotCondition) {
            MCRNotCondition nc = (MCRNotCondition) cond;

            return "not " + buildXSLCondition(nc.getChild());
        } else if (cond instanceof MCRAndCondition) {
            MCRAndCondition ac = (MCRAndCondition) cond;
            List children = ac.getChildren();
            StringBuffer sb = new StringBuffer();
            sb.append("(");

            for (int i = 0; i < children.size(); i++) {
                MCRCondition sc = (MCRCondition) (children.get(i));
                sb.append(buildXSLCondition(sc));

                if (i < (children.size() - 1)) {
                    sb.append(" and ");
                }
            }

            sb.append(")");

            return sb.toString();
        } else if (cond instanceof MCROrCondition) {
            MCROrCondition oc = (MCROrCondition) cond;
            List children = oc.getChildren();
            StringBuffer sb = new StringBuffer();
            sb.append("( ");

            for (int i = 0; i < children.size(); i++) {
                MCRCondition sc = (MCRCondition) (children.get(i));
                sb.append(buildXSLCondition(sc));

                if (i < (children.size() - 1)) {
                    sb.append(" or ");
                }
            }

            sb.append(" )");

            return sb.toString();
        } else {
            return "";
        }
    }

    /** Implements the contains operator as Xalan function extension */
    public static boolean contains(String value, String words) {
        if ((value == null) || (value.trim().length() == 0)) {
            return false;
        }

        if ((words == null) || (words.trim().length() == 0)) {
            return true;
        }

        StringTokenizer st = new StringTokenizer(words);

        String valuelow = value.toLowerCase();
        while (st.hasMoreTokens())

            if (valuelow.indexOf(st.nextToken().toLowerCase()) == -1) {
                return false;
            }

        return true;
    }

    /** Implements the like operator as Xalan function extension */
    public static boolean like(String value, String pattern) {
        if ((value == null) || (value.trim().length() == 0)) {
            return false;
        }

        if ((pattern == null) || (pattern.trim().length() == 0)) {
            return true;
        }

        pattern = pattern.replaceAll("\\?", ".");
        pattern = pattern.replaceAll("\\*", "(.*)");

        Pattern p = Pattern.compile(pattern);

        return p.matcher(value).matches();
    }
}
