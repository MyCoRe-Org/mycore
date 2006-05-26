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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.frontend.editor.MCRInputValidator;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRData2Fields;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Implements a searcher and indexer for MCRObject metadata using only data in
 * memory without any persistent structures. When data is indexed, the values
 * are stored as XML document in memory. When data is searched, the query is
 * transformed to a XSL condition and run against the XML in memory. Before
 * first use of instances of this class, all MCRObject metadata is loaded from
 * persistent store and indexed in memory. This class may also be useful for
 * learning how to implement MCRSearchers and indexers.
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

    public void init(String ID) {
        super.init(ID);

        MCRXMLTableManager mcr_xml = MCRXMLTableManager.instance();

        // Find all types of MCRObject data:
        String cfgPrefix = "MCR.persistence_config_";
        Properties props = MCRConfiguration.instance().getProperties(cfgPrefix);
        for (Enumeration keys = props.keys(); keys.hasMoreElements();) {
            String key = (String) (keys.nextElement());
            String type = key.substring(cfgPrefix.length());
            if ("derivate".equals(type))
                continue;

            LOGGER.debug("Now indexing metadata of all stored MCRObjects from type " + type);

            try {
                List IDs = mcr_xml.retrieveAllIDs(type);
                int numObjects = IDs.size();
                for (int i = 0; i < numObjects; i++) {
                    String sid = (String) (IDs.get(i));
                    MCRObject obj = new MCRObject();
                    MCRObjectID oid = new MCRObjectID(sid);
                    obj.setId(oid);
                    obj.setFromXML(mcr_xml.retrieve(oid), false);
                    List fields = MCRData2Fields.buildFields(obj, index);
                    addToIndex(sid, sid, fields);
                }
            } catch (Exception ex) {
                LOGGER.error(ex);
            }
        }
    }

    protected void addToIndex(String entryID, String returnID, List fields) {
        if ((fields == null) || (fields.size() == 0)) {
            return;
        }

        LOGGER.info("MCRJDOMSearcher indexing data of " + entryID);
        Element data = new Element("data");
        data.setAttribute("returnID", returnID);

        for (int i = 0; i < fields.size(); i++) {
            MCRFieldValue fv = (MCRFieldValue) (fields.get(i));
            Element field = new Element(fv.getField().getName());
            field.addContent(fv.getValue());
            data.addContent(field);
        }

        if (LOGGER.isDebugEnabled()) {
            String s = new XMLOutputter(Format.getPrettyFormat()).outputString(data);
            LOGGER.debug("----------" + entryID + "----------");
            LOGGER.debug(s);
            LOGGER.debug("-----------------------------------");
        }

        map.put(entryID, new Document(data));
    }

    protected void removeFromIndex(String entryID) {
        LOGGER.info("MCRJDOMSearcher removing indexed data of " + entryID);
        map.remove(entryID);
    }

    public MCRResults search(MCRCondition condition, int maxResults, List sortBy, boolean addSortData) {
        String xslCondition = buildXSLCondition(condition);
        LOGGER.debug("MCRJDOMSearcher searching for " + xslCondition);

        Transformer transformer = buildStylesheet(xslCondition);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        MCRResults results = new MCRResults();

        for (Iterator keys = map.keySet().iterator(); keys.hasNext();) {
            String entryID = (String) (keys.next());
            Document xml = (Document) (map.get(entryID));

            if (matches(xml, transformer, out)) {
                String returnID = xml.getRootElement().getAttributeValue("returnID");
                MCRHit hit = new MCRHit(returnID);

                // Add values of all fields that may be sort criteria
                for (int i = 0; i < sortBy.size(); i++) {
                    MCRSortBy by = (MCRSortBy) (sortBy.get(i));

                    List values = xml.getRootElement().getChildren(by.getField().getName());
                    for (Iterator itv = values.iterator(); itv.hasNext();) {
                        Element value = (Element) (itv.next());
                        MCRFieldDef def = MCRFieldDef.getDef(value.getName());
                        hit.addSortData(new MCRFieldValue(def, value.getText()));
                    }
                }

                results.addHit(hit);
            }

            if (sortBy.isEmpty() && (maxResults > 0) && (results.getNumHits() >= maxResults))
                break;
        }

        LOGGER.debug("MCRJDOMSearcher results completed");
        return results;
    }

    /**
     * Returns true if the xml input document matches the xsl when condition in
     * the xsl stylesheet.
     */
    private boolean matches(Document xml, Transformer transformer, ByteArrayOutputStream out) {
        Source xmlsrc = new JDOMSource(xml);

        try {
            out.reset();
            transformer.transform(xmlsrc, new StreamResult(out));
            out.flush();

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
        Namespace extns = Namespace.getNamespace("ext", "xalan://org.mycore.backend.jdom.MCRJDOMSearcher");

        Element stylesheet = new Element("stylesheet");
        stylesheet.setAttribute("version", "1.0");
        stylesheet.setNamespace(MCRFieldDef.xslns);
        stylesheet.addNamespaceDeclaration(MCRFieldDef.xalanns);
        stylesheet.addNamespaceDeclaration(extns);
        stylesheet.setAttribute("extension-element-prefixes", "ext");

        Element output = new Element("output", MCRFieldDef.xslns);
        output.setAttribute("method", "text");
        stylesheet.addContent(output);

        Element template = new Element("template", MCRFieldDef.xslns);
        template.setAttribute("match", "/data");
        stylesheet.addContent(template);

        Element choose = new Element("choose", MCRFieldDef.xslns);
        template.addContent(choose);

        Element when = new Element("when", MCRFieldDef.xslns);
        when.addContent("t");

        Element otherwise = new Element("otherwise", MCRFieldDef.xslns);
        otherwise.addContent("f");
        choose.addContent(when).addContent(otherwise);

        return new Document(stylesheet);
    }

    /** Adds the condition as xsl when test attribute to the stylesheet template */
    private Transformer buildStylesheet(String condition) {
        if (xslTemplate == null) {
            xslTemplate = prepareStylesheet();
        }

        Document xsl = (Document) (xslTemplate.clone());
        xsl.getRootElement().getChild("template", MCRFieldDef.xslns).getChild("choose", MCRFieldDef.xslns).getChild("when", MCRFieldDef.xslns).setAttribute("test", condition);
        Source xslsrc = new JDOMSource(xsl);
        Transformer transformer;
        try {
            transformer = factory.newTransformer(xslsrc);
        } catch (TransformerConfigurationException ex) {
            String msg = "Could not compile XSL stylesheet to be used for searching";
            throw new MCRException(msg, ex);
        }

        return transformer;
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
                    sb.append("ext:compare(text(),'");
                    sb.append(sc.getValue());
                    sb.append("','");
                    sb.append(sc.getOperator());
                    sb.append("')");
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
            return "not(" + buildXSLCondition(nc.getChild()) + ")";
        } else if (cond instanceof MCRAndCondition) {
            MCRAndCondition ac = (MCRAndCondition) cond;
            return buildXSLCondition(ac.getChildren(), "and");
        } else if (cond instanceof MCROrCondition) {
            MCROrCondition oc = (MCROrCondition) cond;
            return buildXSLCondition(oc.getChildren(), "or");
        } else {
            return "";
        }
    }

    /** Builds a combined and/or XSL condition */
    private String buildXSLCondition(List children, String operator) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");

        for (int i = 0; i < children.size(); i++) {
            MCRCondition sc = (MCRCondition) (children.get(i));
            sb.append(buildXSLCondition(sc));

            if (i < (children.size() - 1)) {
                sb.append(" ").append(operator).append(" ");
            }
        }

        sb.append(")");
        return sb.toString();
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
        while (st.hasMoreTokens())

            if (value.indexOf(st.nextToken()) == -1) {
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

        if (!pattern.endsWith("*"))
            pattern = pattern + "*";
        if (!pattern.startsWith("*"))
            pattern = "*" + pattern;

        pattern = pattern.replaceAll("\\?", ".");
        pattern = pattern.replaceAll("\\*", "(.*)");

        LOGGER.debug("Search regex " + pattern + " in text \"" + value + "\"");

        return Pattern.matches(pattern, value);
    }

    /** Implements a string compare operator as Xalan function extension */
    public static boolean compare(String valueA, String valueB, String operator) {
        return MCRInputValidator.instance().compare(valueA, valueB, operator, "string", null);
    }

    public void addSortData(Iterator hits, List sortBy) {
        while (hits.hasNext()) {
            MCRHit hit = (MCRHit) hits.next();
            Document data = (Document) (map.get(hit.getID()));

            for (int j = 0; j < sortBy.size(); j++) {
                MCRFieldDef fd = (MCRFieldDef) sortBy.get(j);
                List values = data.getRootElement().getChildren(fd.getName());
                for (Iterator itv = values.iterator(); itv.hasNext();) {
                    Element value = (Element) (itv.next());
                    hit.addSortData(new MCRFieldValue(fd, value.getText()));
                }
            }
        }
    }
}
