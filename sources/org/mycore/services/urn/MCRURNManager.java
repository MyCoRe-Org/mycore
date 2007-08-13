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
package org.mycore.services.urn;

import java.util.Hashtable;
import java.util.Properties;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;

/**
 * Provides methods to create URNs (urn:nbn:de) and assign them to documents. A
 * URN (uniform resource name) is a special kind of persistent identifier. This
 * class handles URNs from the german subnamespace of NBN (national
 * bibliographic number), these URNs all start with urn:nbn:de:... More
 * information on persistent identifiers can be found at
 * 
 * http://www.persistent-identifier.de/
 * 
 * URNs are described by RFC 2141 and have the following syntax:
 * urn:[NID]:[SNID]-[NISS][Checksum] NID = namespace ID, in this implementation
 * always "nbn:de" SNID = subnamespace ID, a unique identifier for an
 * organization or public library that creates and assigns URNs within its
 * subnamespace NISS = namespace-specific string, a unique ID Checksum: all
 * nbn:de URNs end with one digit that is a checksum
 * 
 * Example: urn:nbn:de:465-miless-20060622-213404-0017
 * 
 * A MyCoRe systen can generate URNs for more than one subnamespace. There must
 * be one or more configurations that control the prefix (subnamespace) of
 * generated URNs and the algorithm used to build new NISS within that
 * subnamespace. Each configuration has a unique "subnamespace configuration ID"
 * in mycore.properties, and optional additional properties depending on the
 * implementation.
 * 
 * MCR.URN.SubNamespace.[ConfigID].Prefix=[URNPrefix], for example
 * MCR.URN.SubNamespace.Essen.Prefix=urn.nbn.de:hbz:465-
 * 
 * @author Frank Lützenkirchen
 */
public class MCRURNManager {

    /** The MCRURNStore implementation to use */
    private static MCRURNStore store;

    /** The table of character codes for calculating the checksum */
    private static Properties codes;

    static {
        codes = new Properties();
        codes.put("0", "1");
        codes.put("1", "2");
        codes.put("2", "3");
        codes.put("3", "4");
        codes.put("4", "5");
        codes.put("5", "6");
        codes.put("6", "7");
        codes.put("7", "8");
        codes.put("8", "9");
        codes.put("9", "41");
        codes.put("a", "18");
        codes.put("b", "14");
        codes.put("c", "19");
        codes.put("d", "15");
        codes.put("e", "16");
        codes.put("f", "21");
        codes.put("g", "22");
        codes.put("h", "23");
        codes.put("i", "24");
        codes.put("j", "25");
        codes.put("k", "42");
        codes.put("l", "26");
        codes.put("m", "27");
        codes.put("n", "13");
        codes.put("o", "28");
        codes.put("p", "29");
        codes.put("q", "31");
        codes.put("r", "12");
        codes.put("s", "32");
        codes.put("t", "33");
        codes.put("u", "11");
        codes.put("v", "34");
        codes.put("w", "35");
        codes.put("x", "36");
        codes.put("y", "37");
        codes.put("z", "38");
        codes.put("-", "39");
        codes.put(":", "17");

        Object obj = MCRConfiguration.instance().getSingleInstanceOf("MCR.Persistence.URN.Store.Class");
        store = (MCRURNStore) obj;
    }

    /**
     * Calculates the checksum for the given urn:nbn:de. The algorithm is
     * specified by the "Carmen AP-4" project.
     * 
     * @return the checksum for the given urn:nbn:de
     */
    public static String buildChecksum(String urn) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < urn.length(); i++) {
            String character = urn.substring(i, i + 1);
            if (!codes.containsKey(character)) {
                String msg = "URN \"" + urn + "\" contains illegal character: '" + character + "'";
                throw new MCRConfigurationException(msg);
            }
            buffer.append(codes.getProperty(character));
        }

        String digits = buffer.toString();
        long sum = 0;
        long digit = 0;

        for (int i = 0; i < digits.length(); i++) {
            digit = Long.parseLong(digits.substring(i, i + 1));
            sum += (digit * (i + 1));
        }

        String quotient = String.valueOf(sum / digit);

        return quotient.substring(quotient.length() - 1);
    }

    /** A map from configID to MCRNissBuilder objects */
    private static Hashtable builders = new Hashtable();

    /**
     * Builds a URN with a custom, given NISS.
     * 
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     * @param niss
     *            the custom NISS
     * @return the complete URN including prefix, NISS and calculated checksum
     */
    public static String buildURN(String configID, String niss) {
        String base = "MCR.URN.SubNamespace." + configID + ".";
        String prefix = MCRConfiguration.instance().getString(base + "Prefix");

        StringBuffer buffer = new StringBuffer(prefix);
        buffer.append(niss);
        buffer.append(buildChecksum(buffer.toString()));
        return buffer.toString();
    }

    /**
     * Builds a URN using a MCRNISSBuilder object.
     * 
     * @param configID
     *            the ID of a subnamespace configuration in mycore.properties
     * @return the complete URN including prefix, niss and calculated checksum
     */
    public static synchronized String buildURN(String configID) {
        String base = "MCR.URN.SubNamespace." + configID + ".";

        MCRNISSBuilder builder = (MCRNISSBuilder) (builders.get(configID));
        if (builder == null) {
            Object obj = MCRConfiguration.instance().getSingleInstanceOf(base + "NISSBuilder");
            builder = (MCRNISSBuilder) (obj);
            builder.init(configID);
            builders.put(configID, builder);
        }

        String niss = builder.buildNISS();
        return buildURN(configID, niss);
    }

    /**
     * Returns true if the given URN has a valid structure and the checksum is
     * correct.
     */
    public static boolean isValid(String urn) {
        if ((urn == null) || (urn.length() < 14) || (!urn.startsWith("urn:nbn:")) || (!urn.toLowerCase().equals(urn))) {
            return false;
        } else {
            String start = urn.substring(0, urn.length() - 1);
            String check = buildChecksum(start);
            return urn.endsWith(check);
        }
    }

    /** Returns true if the given urn is assigned to a document ID */
    public static boolean isAssigned(String urn) {
        return store.isAssigned(urn);
    }

    /** Assigns the given urn to the given document ID */
    public static void assignURN(String urn, String documentID) {
        store.assignURN(urn, documentID);
    }

    /**
     * Retrieves the URN that is assigned to the given document ID
     * 
     * @return the urn, or null if no urn is assigned to this ID
     */
    public static String getURNforDocument(String documentID) {
        return store.getURNforDocument(documentID);
    }

    /**
     * Retrieves the document ID that is assigned to the given urn
     * 
     * @return the ID, or null if no ID is assigned to this urn
     */
    public static String getDocumentIDforURN(String urn) {
        return store.getDocumentIDforURN(urn);
    }

    /**
     * Removes the urn (and assigned document ID) from the persistent store
     */
    public static void removeURN(String urn) {
        store.removeURN(urn);
    }
    
    /**
     * Create and Assign a new URN to the given Document
     * Ensure that new created URNs do not allready exist in URN store
     * @param documentID a MCRID
     * @param configID - the configurationID of the URN Builder 
     * @return the URN
     */
    public static synchronized String buildAndAssignURN(String documentID, String configID){
        String urn=null;
        do{
        	urn = buildURN(configID);
        }
        while(isAssigned(urn));	
        
    	assignURN(urn, documentID);
        return urn;
    }
}
