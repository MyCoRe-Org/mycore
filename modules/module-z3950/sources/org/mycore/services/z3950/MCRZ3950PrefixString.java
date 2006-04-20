package org.mycore.services.z3950;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * Diese Klasse ist eine Hilfsklasse zum Parsen einer Z39.50-Suchanfrage im
 * Prefixformat. Dabei wird nur die Attributmenge "bib-1" berücksichtigt.
 * Für alle sechs Attributtypen gibt es eine verkettete Liste, die die
 * gesetzten Attribute speichert.
 * @author Andreas de Azevedo
 * @version 1.0
 * 
 */
public class MCRZ3950PrefixString {
	
	private static Logger logger = Logger.getLogger(MCRZ3950QueryService.class);
	
	private LinkedList attributes1;
	private LinkedList attributes2;
	private LinkedList attributes3;
	private LinkedList attributes4;
	private LinkedList attributes5;
	private LinkedList attributes6;
	
	// Alle Attribute
	private HashMap attributes;
	
	// Kann es mehrere Sets in einer Anfrage geben?
	private String attributeSet;
	
	// Der Suchterm als String
	private String term;
	
	// Die Z39.50-Query als String
	private String query;
	
    public MCRZ3950PrefixString() {
		attributes1 = new LinkedList();
		attributes2 = new LinkedList();
		attributes3 = new LinkedList();
		attributes4 = new LinkedList();
		attributes5 = new LinkedList();
		attributes6 = new LinkedList();
		attributes = new HashMap();
		attributes.put(new Integer(1), attributes1);
		attributes.put(new Integer(2), attributes2);
		attributes.put(new Integer(3), attributes3);
		attributes.put(new Integer(4), attributes4);
		attributes.put(new Integer(5), attributes5);
		attributes.put(new Integer(6), attributes6);
	}
    
    public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
		parse();
	}
	
	public LinkedList getAttributes(int type) {
		return (LinkedList) attributes.get(new Integer(type));
	}
	
	private void parse() {
		// Beispiel: @attrset bib-1 @attr 1=4 water
		if (query == null) return;
		StringTokenizer st = new StringTokenizer(query, "@");
		logger.debug("#################### So, es wird geparst!");
		logger.debug("#################### Query: " + query);
		while (st.hasMoreTokens()) {
			String next = st.nextToken();
			logger.debug("#################### next = " + next);
			StringTokenizer token = new StringTokenizer(next, " ");
			while (token.hasMoreTokens()) {
				String nextToken = token.nextToken();
				logger.debug("#################### nextToken = " + nextToken);
				if (nextToken.equals("attrset"))
					attributeSet = token.nextToken();
				if (nextToken.equals("attr")) {
					String attrType = token.nextToken();
					logger.debug("#################### attrType = " + attrType);
					/* Unterscheidung, eventuell Leerzeichen zwischen '=' und
					 * Attributen.
					 */
					if (attrType.length() > 1) {
						Integer type = Integer.valueOf(attrType.substring(0, 1));
						LinkedList ll = (LinkedList) attributes.get(type);
						String attrValue = attrType.substring(
								attrType.indexOf('=') + 1, attrType.length());
						Integer value = Integer.valueOf(attrValue);
						ll.add(value);
					} else {
						Integer type = Integer.valueOf(attrType);
						LinkedList ll = (LinkedList) attributes.get(type);
						// '='-Zeichen überspringen
						token.nextToken();
						String attrValue = token.nextToken();
						Integer value = Integer.valueOf(attrValue);
						ll.add(value);		
					}
					/* Der letzte Wert ist der Term (eventuell)
					 * Mehrere Wörter sind so "[x,y,z]" codiert. */ 
					if (token.hasMoreTokens()) {
						term = token.nextToken("[]");
						
						// Mehrere Wörter
						while (token.hasMoreTokens()) {
							term += token.nextToken("[]");
						}
					}
					logger.debug("#################### term = " + term);
				}
			}
		}
	}

	public String getAttributeSet() {
		return attributeSet;
	}

	public String getTerm() {
		return term;
	}
	
}
