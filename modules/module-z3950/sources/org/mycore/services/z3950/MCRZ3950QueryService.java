package org.mycore.services.z3950;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.query.MCRQueryCache;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.mycore.services.fieldquery.*;

/**
 * Diese Klasse ist eine Implementierung eines Suchservice für die Z39.50-
 * Schnittstelle. Dabei werden nur Z39.50-Anfragen im Prefixformat
 * entgegengenommen.
 * @author Andreas de Azevedo
 * @version 1.0
 * 
 */
public class MCRZ3950QueryService implements MCRZ3950Query {
	
	protected static MCRConfiguration CONFIG = MCRConfiguration.instance();
	
	private static Logger logger = Logger.getLogger(MCRZ3950QueryService.class);
	
	// Die Z39.50-Anfrage als String
	private String query;
	
	// Das Ergebnis im XMLContainer gespeichert
	private MCRXMLContainer mycoreResults;
	
	// Wir geben immer nur ein Ergebnis zurück, normalerweise das erste
	private int index;
	
	// Der geparste QueryString
	private MCRZ3950PrefixString prefixString;
	

	public MCRZ3950QueryService() {
		this(null);
	}
	
    public MCRZ3950QueryService(String query) {
		this.query = query;
		prefixString = new MCRZ3950PrefixString();
		prefixString.setQuery(query);
		index = 0;
	}
	
	public void cutDownTo(int maxresults) {
		if (mycoreResults.size() > 0 && maxresults > 0) 
			mycoreResults.cutDownTo(maxresults);
	}
	
	public void sort() {}
	
	public Document getDocument() {
		Document result = null;
		if (mycoreResults.size() > 0) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Create the builder and parse the file
			try {
				result = factory.newDocumentBuilder().parse(
						new ByteArrayInputStream(mycoreResults.exportAllToByteArray()));
			} catch (SAXException se) {
				logger.error("Error while parsing results.", se);
				se.printStackTrace();
			} catch (IOException ioe) {
				logger.error("I/O Error while parsing results.", ioe);
				ioe.printStackTrace();
			} catch (ParserConfigurationException pce) {
				logger.error("Could not create DocumentBuilder", pce);
				pce.printStackTrace();
			}
		}
		return result;	
	}
	
	/**
	 * Gibt alle Ergebnisse als Bytestrom zurück.
	 * @return Das Ergebnisdokument als Byte-Array, null falls es keine Ergebnisse gab.
	 */
	public byte[] getDocumentAsByteArray() {
		byte[] result = null;
		if (mycoreResults.size() > 0)
			try {
				result = mycoreResults.exportAllToByteArray();
			} catch (IOException ioe) {
				
				ioe.printStackTrace();
			}
		return result;	
	}
	
	/**
	 * Führt eine Suchanfrage in MyCoRe aus.
	 * @return True falls es Ergebnisse gab, sonst False.
	 */
	public boolean search() {
		String meta = null;
        Element query = new Element("query");
        query.setAttribute("maxResults", "10");
        query.setAttribute("numPerPage", "10");
        
        Element conditions = new Element("conditions");
        query.addContent(conditions);
        
        conditions.setAttribute("format", "xml");
        Element b = new Element("boolean");
        b.setAttribute("operator", "and");
        conditions.addContent(b);
        
        
		if (prefixString.getAttributeSet().equals("bib-1")) {
			LinkedList attributes = prefixString.getAttributes(1);
			Iterator it = attributes.iterator();
			while (it.hasNext()) {
				Integer attrValue = (Integer) it.next();
				//MCR.z3950.1.4
				// Isoliere alle Mappings für Typ 1
				Properties p = CONFIG.getProperties("MCR.z3950.1.");
				//CONFIG.getString("MCR.z3950.1.4");
				// Iteriere alle Mappings für Typ 1 (auschließend)
				Enumeration e = p.propertyNames();
				while (e.hasMoreElements()) {
					String propertyName = (String) e.nextElement();
					Integer mycoreAttr = Integer.valueOf(propertyName.substring("MCR.z3950.1.".length(),
							propertyName.length()));
					// Vergleich der Konfiguration mit tatsächlicher Anfrage
					if (attrValue.equals(mycoreAttr)) {
						String xpath = CONFIG.getString("MCR.z3950.1." +
								mycoreAttr);
/* OLD QUERY                        
						// Sonderfälle: Dokumenten-Id und Suche in allen Feldern
						if (xpath.equals("@ID")) {
							// Dokumenten-Id
							meta = "/mycoreobject[@ID=" + prefixString.getTerm() + "]";
						} else if (xpath.equals("doctext()")) {
							meta = "/mycoreobject[" + xpath + " contains(" +
					                prefixString.getTerm() + ")] or " +
					                "/mycoreobject[ts() contains(" +
					                prefixString.getTerm() + ")]";
							
						} else meta = "/mycoreobject[" + xpath + " contains(" +
						        prefixString.getTerm() + ")]";
						logger.debug("Aus dem Kern: " + meta);
*/                        
                      Element cond = new Element("condition");
                      cond.setAttribute("field", xpath);
                      cond.setAttribute("operator", "contains");
                      cond.setAttribute("value", prefixString.getTerm());
                      b.addContent(cond);
                      
                      if (logger.isDebugEnabled())
                      {
                        org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
                        logger.debug("Aus dem Kern: " + outputter.outputString(query));
                      }
				    }
				}
			}
		}
        
        MCRResults result = MCRQueryManager.search( new org.jdom.Document(query));
        
		mycoreResults = new MCRXMLContainer();
		for (int i=0;i<result.getNumHits();i++)
		{
           MCRHit hit = result.getHit(i);
		   Element ele = MCRURIResolver.instance().resolve("mcrobject:"+hit.getID()); 
           mycoreResults.add("host", hit.getID(), 1, ele);
		}
		if (mycoreResults.size() > 0) return true;
		else return false;
	}
	
	/**
	 * Die Methode <code>fillClassificationsWithLabels</code> durchsucht alle
	 * Metadaten und untersucht deren benutzte Klassifikationen. Da in den
	 * Metadaten nur ein Verweis auf Klasse und Kategorie ist, wird dieser
	 * ergänzt durch sein Label.
	 */
	public void fillClassificationsWithLabels() {
		// Keine Ahnung, ob es 0 sein muss
    	Element results = mycoreResults.getXML(0);
    	// Unser Wurzelknoten
    	Element metadata = results.getChild("metadata");
    	// Alle Kinder des Knotens, also alle Metadaten
    	List metadataChildren = metadata.getChildren();
    	Iterator itm = metadataChildren.iterator();
    	// Iteriere über alle Knoten
    	while (itm.hasNext()) {
            // Prüfe, ob der Knoten eine Klassifikation benutzt
    		Element parent = (Element) itm.next();
    		String cl = parent.getAttributeValue("class");
    		if (cl.equals("MCRMetaClassification")) {
    			// Iteriere über alle Kinder des Knotens (z.B. Subject)    			
    			List children = parent.getChildren();
    			Iterator it = children.iterator();
    			while (it.hasNext()) {
    				Element e = (Element) it.next();
    				String classificationId = e.getAttributeValue("classid");
    				String categoryId = e.getAttributeValue("categid");
    				MCRCategoryItem category = 
    					MCRCategoryItem.getCategoryItem(classificationId,
    					                                categoryId);
    				// Fülle den Knoten mit dem Klassifiaktions-Label
    				e.setText(category.getText(0));
    			}
    		}
    	}
	}
	
	public int getSize() {
		return mycoreResults.size();
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
		mycoreResults = mycoreResults.exportElementToContainer(index);
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		prefixString.setQuery(query);
		this.query = query;
	}

}
