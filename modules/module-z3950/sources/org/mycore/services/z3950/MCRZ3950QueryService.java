package org.mycore.services.z3950;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
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
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
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
	

	public MCRZ3950QueryService() {
		this(null);
	}
	
    public MCRZ3950QueryService(String query) {
		this.query = query;
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
        MCRZ3950PrefixQueryParser pqs = new MCRZ3950PrefixQueryParser( new StringReader( query ) );
        MCRCondition condition = pqs.parse();
        
        if (logger.isDebugEnabled())
            logger.debug("Transformed query: " + condition.toString());

        MCRResults result = MCRQueryManager.search(new MCRQuery( condition ));

        mycoreResults = new MCRXMLContainer();
        for (int i = 0; i < result.getNumHits(); i++) {
            MCRHit hit = result.getHit(i);
            Element ele = MCRURIResolver.instance().resolve("mcrobject:" + hit.getID());
            mycoreResults.add("host", hit.getID(), 1, ele);
        }
        if (mycoreResults.size() > 0)
            return true;
        else
            return false;
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
		this.query = query;
	}

}
