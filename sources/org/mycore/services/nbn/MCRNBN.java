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

package org.mycore.services.nbn;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mycore.common.MCRArgumentChecker;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;

/**
 * Represents a "National Bibliographic Name" URN that is
 * used as a persistent identifier for digital documents 
 * like electronic dissertations, as proposed by "Deutsche Bibliothek"
 * in the project "Carmen - AP 4". A typical NBN URN looks like
 * the following:<p>
 *
 * <code>urn:nbn:de:hbz:465-123456788</code>
 *
 * @author Frank Lützenkirchen
 * @author Werner Greßhoff
 * @version $Revision$ $Date$
 */
public class MCRNBN {
	
	/** Logger */
	static Logger logger = Logger.getLogger(MCRNBN.class);
	
	private static MCRConfiguration config;
	
	/** The persistence interface */
	private static MCRNBNManager manager;
	
	/** The URN prefix, that is the part of the URN before the NISS */
	protected static String prefix;  
 
	/** The last URN that has been created */
	protected static String last;

	/** The table of character codes for calculating the URN checksum */
	protected static Properties codes;

	/** Initializes the class */
	static {
		MCRConfiguration.instance().reload(true);
		config = MCRConfiguration.instance();
    	
		codes = new Properties();
		codes.put( "0",  "1" );
		codes.put( "1",  "2" );
		codes.put( "2",  "3" );
		codes.put( "3",  "4" );
		codes.put( "4",  "5" );
		codes.put( "5",  "6" );
		codes.put( "6",  "7" );
		codes.put( "7",  "8" );
		codes.put( "8",  "9" );
		codes.put( "9", "41" );
		codes.put( "a", "18" );
		codes.put( "b", "14" );
		codes.put( "c", "19" );
		codes.put( "d", "15" );
		codes.put( "e", "16" );
		codes.put( "f", "21" );
		codes.put( "g", "22" );
		codes.put( "h", "23" );
		codes.put( "i", "24" );
		codes.put( "j", "25" );
		codes.put( "k", "42" );
		codes.put( "l", "26" );
		codes.put( "m", "27" );
		codes.put( "n", "13" );
		codes.put( "o", "28" );
		codes.put( "p", "29" );
		codes.put( "q", "31" );
		codes.put( "r", "12" );
		codes.put( "s", "32" );
		codes.put( "t", "33" );
		codes.put( "u", "11" );
		codes.put( "v", "34" );
		codes.put( "w", "35" );
		codes.put( "x", "36" );
		codes.put( "y", "37" );
		codes.put( "z", "38" );
		codes.put( "-", "39" );
		codes.put( ":", "17" );

		try {
			prefix = config.getString("MCR.NBN.NamespacePrefix");
	    	manager = (MCRNBNManager) config.getInstanceOf("MCR.NBN.ManagerImplementation");
		} catch (MCRConfigurationException mcrx) {
			String msg = "Missing configuration data.";
            logger.fatal(msg);
		}
	}
   
	/** 
	 * Method getLocalPrefix. Creates a new NISS, the local ID of the ressource.
	 * This implementation creates a NISS of eight digits 
	 * length that is derived from the current time measured
	 * in seconds. The method guarantees uniqueness and therefore
	 * will block to produce only one NISS per second.
	 * 
	 * @return a new NISS
	 */
	protected static synchronized String produceNISS() {
		String niss;  
      
		do {
			Calendar now = new GregorianCalendar();
			int yyy = 2268 - now.get(Calendar.YEAR);
			int ddd = 500 - now.get(Calendar.DAY_OF_YEAR);
			int hh = now.get( Calendar.HOUR_OF_DAY);
			int mm = now.get( Calendar.MINUTE);
			int ss = now.get( Calendar.SECOND);
			int sss = 99999 - (hh * 3600 + mm * 60 + ss);

			String DDDDD = String.valueOf(yyy * 366 + ddd);
      
			StringBuffer buffer = new StringBuffer();
			buffer.append(DDDDD.charAt(4));
			buffer.append(DDDDD.charAt(2));
			buffer.append(DDDDD.charAt(1));
			buffer.append(DDDDD.charAt(3));
			buffer.append(DDDDD.charAt(0));
			buffer.append(sss);
			niss = buffer.toString();
		}
		
		while (niss.equals(last));

		logger.info("New NISS created: " + niss);
		return (last = niss);
	}

	/**
	 * Calculates the checksum for the given URN. The algorithm
	 * is specified by the "Carmen AP-4" project.
	 *
	 * @return the checksum for the given URN
	 */ 
	protected static String buildChecksum(String urn) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < urn.length(); i++) { 
			String character = urn.substring(i, i + 1);
			buffer.append(codes.getProperty(character));
		}
    
		String digits = buffer.toString();
		long sum = 0;
		long digit = 0;
    
		for (int i = 0; i < digits.length(); i++) {
			digit = Long.parseLong(digits.substring(i, i + 1));
			sum += digit * (i + 1);
		}
    
		String quotient = String.valueOf(sum / digit);
		return quotient.substring(quotient.length() - 1);
	} 

	/**
	 * Method getLocalPrefix. Returns the local prefix of the urn's
	 * @return the local prefix of an urn
	 */
	public static String getLocalPrefix() {
  		return prefix;
	}
  
	protected String  urn;
	protected Boolean valid;
	
	protected String author;
	protected String comment = null;
  
	/**
	 * Creates a new local NBN and calculates a unique NISS
	 * and checksum for it.
	 * This implementation creates a NISS of eight digits
	 * length that is derived from the current time measured
	 * in seconds. The constructor guarantees uniqueness and therefore
	 * will block to produce only one NBN per second.
	 * This is only left for compability and should be deprecated!
	 */
	public MCRNBN() {
		StringBuffer buffer = new StringBuffer(prefix);
		buffer.append(produceNISS());
		buffer.append(buildChecksum(buffer.toString()));
		urn = buffer.toString();

		valid = Boolean.TRUE;
		author = new String("");
		manager.reserveURN(this);
	}
  
	public MCRNBN(String author, String comment) {
		StringBuffer buffer = new StringBuffer(prefix);
		buffer.append(produceNISS());
		buffer.append(buildChecksum(buffer.toString()));
		urn = buffer.toString();

		valid = Boolean.TRUE;
		this.author = author;
		this.comment = comment;
		manager.reserveURN(this);
	}
  
	/**
	 * Method MCRNBN. Creates a new NBN object from the given URN. If the
	 * urn is local but not in the persistence store, it will be stored.
	 * 
	 * @param the urn to create a NBN from.
	 */
	public MCRNBN(String urn) {
		MCRArgumentChecker.ensureNotEmpty(urn, "urn");  
		this.urn = urn; 
		
		if (isLocal()) {
			HashMap map = (HashMap) manager.listURNs(getNISSandChecksum());
			if (map.isEmpty()) {
				manager.reserveURN(this);
			} else {
				author = manager.getAuthor(this);
				comment = manager.getComment(this);
			}
		}
	}
  
	public static boolean exists(String urn) {
		MCRArgumentChecker.ensureNotEmpty(urn, "urn");
		if (urn.startsWith(prefix)) {
			logger.info("Looking for NBN: " + urn.substring(urn.lastIndexOf("-") + 1));
			HashMap map = (HashMap) manager.listURNs(urn.substring(urn.lastIndexOf("-") + 1));
			if (!map.isEmpty()) {
				return true;
			}
		}
		
		return false;
	}
	  
	/**
	 * Method getAuthor. Returns the author data from the reservation.
	 * @return String
	 */
	public String getAuthor() {
		return author;
	}
	
	/**
	 * Method getComment. Returns the comment data from the reservation.
	 * @return String
	 */
	public String getComment() {
		return comment;
	}
	
	/**
	 * Method getDate. Returns the reservation date.
	 * @return GregorianCalendar the reservation date.
	 */
	public GregorianCalendar getDate() {
		return manager.getDate(this);
	}
	
	/**
	 * Method delete. Removes a URN from the store and makes frees the 
	 * non-static resources.
	 */
	public void delete() {
		manager.removeURN(this);
		urn = null;
		valid = null;
	}
	
	/**
	 * Method getURL. Returns the URL which has been locally attached with the URN
	 * @return the URL belonging to the URN
	 */
	public String getURL() {
		if (isLocal()) {
			return manager.getURL(this);
		}
		
		return null;
	}
	
	/**
	 * Method setURL. Sets the URL which is locally attached with the URN
	 * @param the url which should be set for the URN.
	 */
	public void setURL(String url) {
		if (isLocal()) {
			manager.setURL(this, url);
		}
	}
	
	/**
	 * @return the document id belonging to the URN
	 */
	public String getDocumentId() {
		if (isLocal()) {
			manager.getDocumentId(this);
		}
		return null;
	}
	
	/**
	 * Method setDocumentId. Sets the document id which is locally attached with the URN
	 * @param the url which should be set for the URN.
	 */
	public void setDocumentId(String documentId) {
		if (isLocal()) {
			manager.setDocumentId(this, documentId);
		}
	}
	
	/**
	 * Returns the namespace of this URN, that is the part
	 * between "urn:" and the last "-" in the URN. Returns null
	 * if this NBN is not valid.
	 */
	public String getNamespace() {
		return (isValid() ? urn.substring(4, urn.lastIndexOf("-")) : null);
	}
  
	/**
	 * Returns the NISS and checksum part of this NBN URN.
	 * Returns null if this NBN is not valid.
	 */
	public String getNISSandChecksum() {
		return (isValid() ? urn.substring(urn.lastIndexOf("-") + 1) : null);
	}
  
	/**
	 * Returns the NBN part of this URN, that is the part
	 * after "urn:", or null if this NBN is not valid.
	 */
	public String getNBN() {
		return (isValid() ? urn.substring(4) : null);
	}
  
	/**
	 * Returns the URN.
	 */
	public String getURN() {
		return (isValid() ? urn : null);
	}
  
	/**
	 * Returns true if this NBN is valid and starts with
	 * the local NBN prefix.
	 */
	public boolean isLocal() {
		return isValid() && urn.startsWith(prefix);
	}
  
	/**
	 * Returns true if this NBN has a valid structure and 
	 * the checksum is correct.
	 */
	public boolean isValid() {
		if (valid == null) {
			if ((urn == null) || (urn.length() < 19)
					|| (!urn.startsWith("urn:nbn:"))
					|| (!urn.toLowerCase().equals(urn))) {
				valid = Boolean.FALSE;
			} else {
				String start = urn.substring(0, urn.length() - 1);
				String check = buildChecksum(start);
				valid = new Boolean(urn.endsWith(check));
			}
		}
		
		return valid.booleanValue();
	}
  
	/** Returns the URN this NBN object represents **/
	public String toString() {
		return urn;
	}

	/** 
	 * A simple test application that generates and tests some NBN URNs
	 */
	public static void main(String[] args) {
		System.out.println("NBN URN produced : " + new MCRNBN());  
		System.out.println("NBN URN produced : " + new MCRNBN());  
		System.out.println("NBN URN produced : " + new MCRNBN());  
		System.out.println();
    
		MCRNBN urn = new MCRNBN("urn:nbn:de:bv:333-123456788");
		System.out.println("  NBN URN : " + urn);
		System.out.println(" is valid : " + urn.isValid());
		System.out.println(" is local : " + urn.isLocal());
		System.out.println("      nbn : " + urn.getNBN());
		System.out.println("   niss+p : " + urn.getNISSandChecksum());
		System.out.println("namespace : " + urn.getNamespace());
		System.out.println();
    
		urn = new MCRNBN();
		System.out.println("  NBN URN : " + urn);
		System.out.println(" is valid : " + urn.isValid());
		System.out.println(" is local : " + urn.isLocal());
		System.out.println("      nbn : " + urn.getNBN());
		System.out.println("   niss+p : " + urn.getNISSandChecksum());
		System.out.println("namespace : " + urn.getNamespace());
	}
  
}

