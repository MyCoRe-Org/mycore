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

package org.mycore.datamodel.ifs;

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.mycore.common.MCRArgumentChecker;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.services.query.MCRTextSearchInterface;

/**
 * This class manages instances of MCRContentStore and MCRAudioVideoExtender 
 * and provides methods to get these for a given Store ID or MCRFile instance.
 * The class is responsible for looking up, loading, instantiating and
 * remembering the implementations of MCRContentStore and MCRAudioVideoExtender 
 * that are used in the system.
 *
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRContentStoreFactory {
	/** Hashtable StoreID to MCRContentStore instance */
	protected static Hashtable STORES;

	/** Hashtable StoreID to Class that implements MCRAudioVideoExtender */
	protected static Hashtable EXTENDER_CLASSES;

	/** Hashtable StoreID to MCRContentStore implementing {@link org.mycore.services.query.MCRTextSearchInterface} */
	protected static Hashtable INDEX_STORES;

	/** The MCRContentStoreSelector implementation that will be used */
	protected static MCRContentStoreSelector STORE_SELECTOR;
	private static final Logger LOGGER =
			Logger.getLogger(MCRContentStoreFactory.class);

	/**
	 * Returns the MCRContentStore instance that is configured for this
	 * StoreID. The instance that is returned is configured by the property
	 * <tt>MCR.IFS.ContentStore.<StoreID>.Class</tt> in mycore.properties.
	 * 
	 * @param storeID the non-null ID of the MCRContentStore implementation
	 * @return the MCRContentStore instance that uses this storeID
	 * @throws MCRConfigurationException if no MCRContentStore implementation is configured for this storeID
	 */
	public static MCRContentStore getStore(String storeID) {
		if (STORES == null)
			STORES = new Hashtable();
		if (INDEX_STORES == null)
			INDEX_STORES = new Hashtable();
		MCRArgumentChecker.ensureNotEmpty(storeID, "Store ID");
		if (!STORES.containsKey(storeID)) {
			try {
				String storeClass =
					"MCR.IFS.ContentStore." + storeID + ".Class";
				LOGGER.debug("getting StoreClass: "+storeClass);
				Object obj =
					MCRConfiguration.instance().getInstanceOf(storeClass);
				MCRContentStore s = (MCRContentStore) (obj);
				s.init(storeID);
				STORES.put(storeID, s);
				if (s instanceof MCRTextSearchInterface) {
					INDEX_STORES.put(storeID, s);
				}
			} catch (Exception ex) {
				String msg =
					"Could not load MCRContentStore with store ID = " + storeID;
				throw new MCRConfigurationException(msg, ex);
			}
		}
		return (MCRContentStore) (STORES.get(storeID));
	}

	/**
	 * returns all ContentStores which implements {@link org.mycore.services.query.MCRTextSearchInterface}
	 * @return Array of {@link org.mycore.services.query.MCRTextSearchInterface} ContentStores
	 */
	public static MCRTextSearchInterface[] getAllIndexables() {
		if (STORE_SELECTOR == null)
			initStoreSelector();
		String[] s = STORE_SELECTOR.getAvailableStoreIDs();
		if (s.length==0 && INDEX_STORES==null){
			INDEX_STORES=new Hashtable();
		}
		else for (int i = 0; i < s.length; i++)
			getStore(s[i]);
		if (INDEX_STORES.size()==0)
			return new MCRTextSearchInterface[0];
		return (MCRTextSearchInterface[]) INDEX_STORES.values().toArray(
				new MCRTextSearchInterface[INDEX_STORES.size()]);
	}

	/**
	 * Returns the MCRContentStore instance that should be used
	 * to store the content of the given file. The configured
	 * MCRContentStoreSelector is used to make this decision.
	 *
	 * @see MCRContentStoreSelector
	 * @see MCRContentStore
	 **/
	public static MCRContentStore selectStore(MCRFile file) {
		if (STORE_SELECTOR == null)
			initStoreSelector();
		String store = STORE_SELECTOR.selectStore(file);
		return getStore(store);
	}

	private static void initStoreSelector() {
		String property = "MCR.IFS.ContentStoreSelector.Class";
		Object obj = MCRConfiguration.instance().getInstanceOf(property);
		STORE_SELECTOR = (MCRContentStoreSelector) obj;
	}

	/**
	 * Returns the Class instance that implements the MCRAudioVideoExtender for 
	 * the MCRContentStore with the given ID. That class is configured by 
	 * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
	 * mycore.properties.
	 * 
	 * @param storeID the non-null StoreID of the MCRContentStore
	 * @return the Class that implements MCRAudioVideoExtender for the StoreID given, or null
	 * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
	*/
	protected static Class getExtenderClass(String storeID) {
		if (EXTENDER_CLASSES == null)
			EXTENDER_CLASSES = new Hashtable();
		MCRArgumentChecker.ensureNotNull(storeID, "store ID");

		String storeClass = "MCR.IFS.AVExtender." + storeID + ".Class";

		String value = MCRConfiguration.instance().getString(storeClass, "");
		if (value.equals(""))
			return null;

		if (!EXTENDER_CLASSES.containsKey(storeID)) {
			try {
				Class cl = Class.forName(value);
				EXTENDER_CLASSES.put(storeID, cl);
			} catch (Exception ex) {
				String msg = "Could not load AudioVideoExtender class " + value;
				throw new MCRConfigurationException(msg, ex);
			}
		}

		return (Class) (EXTENDER_CLASSES.get(storeID));
	}

	/**
	 * Returns true if the MCRContentStore with the given StoreID provides an 
	 * MCRAudioVideoExtender implementation, false otherwise.
	 * The MCRAudioVideoExtender for a certain MCRContentStore is configured by 
	 * the property <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in 
	 * mycore.properties.
	 * 
	 * @param storeID the non-null StoreID of the MCRContentStore
	 * @return the MCRAudioVideoExtender for the StoreID given, or null
	 * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
	*/
	static boolean providesAudioVideoExtender(String storeID) {
		return (getExtenderClass(storeID) != null);
	}

	/**
	 * If the MCRContentStore of the MCRFile given provides an 
	 * MCRAudioVideoExtender implementation, this method creates and initializes 
	 * the MCRAudioVideoExtender instance for the MCRFile. 
	 * The instance that is returned is configured by the property
	 * <tt>MCR.IFS.AVExtender.<StoreID>.Class</tt> in mycore.properties.
	 * 
	 * @param file the non-null MCRFile that should get an MCRAudioVideoExtender
	 * @return the MCRAudioVideoExtender for the MCRFile given, or null
	 * @throws MCRConfigurationException if the MCRAudioVideoExtender implementation class could not be loaded
	 */
	static MCRAudioVideoExtender buildExtender(MCRFileReader file) {
		MCRArgumentChecker.ensureNotNull(file, "file");

		if (!providesAudioVideoExtender(file.getStoreID()))
			return null;

		Class cl = getExtenderClass(file.getStoreID());

		try {
			Object obj = cl.newInstance();
			MCRAudioVideoExtender ext = (MCRAudioVideoExtender) obj;
			ext.init(file);
			return ext;
		} catch (Exception exc) {
			if (exc instanceof MCRException)
				throw (MCRException) exc;

			String msg = "Could not build MCRAudioVideoExtender instance";
			throw new MCRConfigurationException(msg, exc);
		}
	}
}
