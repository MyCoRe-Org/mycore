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
package org.mycore.services.query;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.remote.MCRRemoteAccessInterface;
import org.mycore.backend.remote.MCRServletCommunication;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.datamodel.classifications.MCRClassification;

/**
 * This class makes use of the MCRCache functionality and implements
 * the MCRQueryInterface if someone need this.
 * configure options:
 * MCR.query_cache_capacitity_class: integer of amount of objects stored in the classification cache
 * MCR.query_cache_capacitity_other: integer of amount of other objects stored in cache
 * MCR.query_cache_time: time a "other" object is valid in cache in minutes
 * MCR.query_cache_time_class: time a classification stays valid in cache
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRQueryCache {
	
	public static final int CACHETYPE_CLASS=0;
	public static final int CACHETYPE_OTHER=1;
	protected static final int CACHETYPE_COUNT=2;
	protected static MCRCache[] queryCache;
	private static MCRClassification cl;
	private static Document jdom;
	private static MCRConfiguration config;
	private static long timePassed_cl;
	private static long timePassed;
	private static boolean initialized=false;
	private static MCRQueryInterface queryint;
	private static MCRRemoteAccessInterface comm;
	static Logger logger;
	
	/**
	 * checks if cache is initialized
	 * @return
	 */
	public static boolean isInitialized(){
		return initialized;
	}
	
	/**
	 * initializes the cache to be used
	 *
	 */
	public synchronized static void init(){
		logger=Logger.getLogger(MCRQueryCache.class.getName());
		config = MCRConfiguration.instance();
		PropertyConfigurator.configure(config.getLoggingProperties());
		queryint = (MCRQueryInterface)config.
						getInstanceOf("MCR.persistence_"+
		                      config.getString( "MCR.XMLStore.Type" ).toLowerCase()+
							  "_merger_name");
		int[] capacity= new int[2];
		capacity[0]=config.getInt("MCR.query_cache_capacitity_class",100);
		capacity[1]=config.getInt("MCR.query_cache_capacitity_other",100);
		queryCache=new MCRCache[CACHETYPE_COUNT];
		for (int i=0; i < queryCache.length; i++){
			queryCache[i]=new MCRCache(capacity[i]);
		}
		cl=new MCRClassification();
		timePassed=config.getLong("MCR.query_cache_time_other",10*60);
		timePassed*=1000;
		timePassed_cl=config.getLong("MCR.query_cache_time_class",5*60*60);
		timePassed_cl*=1000;
		jdom=new Document(new Element("root"));
		comm= new MCRServletCommunication();
		initialized=true;
		logger.info("MCRQueryCache initialized!");
		logger.debug("MCRQueryCache initial capacity: "
		                       + queryCache[0].getCapacity() + " & "
		                       + queryCache[1].getCapacity());
		logger.debug("MCRQueryCache update time: "
		                       + (timePassed_cl/1000) + " & "
		                       + (timePassed/1000)+ " seconds!");
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.query.MCRQueryInterface#getResultList(java.lang.String, java.lang.String, int)
	 */
	public static MCRXMLContainer getResultList(
		String query,
		String type,
		int maxresults) {
		if (!isInitialized()){
			//first we need to initialize this cache
			init();
		}
		return getResultList("local",query,type,maxresults);
	}

	/* returns the search result from the cache and keep it up to date.
	 * @see org.mycore.services.query.MCRQueryInterface#getResultList(java.lang.String, java.lang.String, int)
	 */
	public static MCRXMLContainer getResultList(
		String host,
		String query,
		String type,
		int maxresults) {
	if (!isInitialized()){
		//first we need to initialize this cache
		init();
	}
	// all should be running fine now
		if (type.equalsIgnoreCase("class")) {
			return getClass(host,query);			
		}
		//no classification, so we see the other cache
		return getOther(host,query,type,maxresults);
	}
	
	/**
	 * use this as a (fast) replacement of MCRClassification.search(query)
	 * @param query
	 * @return resulting MCRXMLContainer
	 * @see org.mycore.datamodel.classifications.MCRClassification#search(java.lang.String)
	 */
	public final static MCRXMLContainer getClass(String query){
		if (isInitialized())
			return getClass("local", query);
		return null;
	}
	private final static MCRXMLContainer getClass(String host, String query){
		if (isInitialized()){
			MCRXMLContainer returns=new MCRXMLContainer();
			Object cacheObject;
			String key=host+"$"+query;
			cacheObject=queryCache[CACHETYPE_CLASS].
								 getIfUpToDate(key,(System.currentTimeMillis()-timePassed_cl));
			if (cacheObject!=null){
				returns=(MCRXMLContainer)cacheObject;
				logger.debug("Found class "+query+" in cache!");
			}
			else
				synchronized(jdom){
					if (!host.equals("local"))
						synchronized(comm){
							comm = (MCRRemoteAccessInterface)
							  config.getInstanceOf("MCR.remoteaccess_"+host+"_query_class");
							returns.importElements(comm.requestQuery(host,"class",query));
						}//was local
					else{
						synchronized(cl){
							jdom = cl.search(query);
						}
						if (jdom != null) {
							returns.add(host,
								  jdom.getRootElement().getAttributeValue("ID"),
								  1,
								  (Element)jdom.getRootElement().clone());
						}
					}//was not local
					queryCache[CACHETYPE_CLASS].put(key,returns);
					logger.debug("Put class "+query+" in cache!");
				}
			return returns;
		}
		return null; //isInitialised()==false
	}

	private final static MCRXMLContainer getOther(
		String host,
		String query,
		String type,
		int maxresults)
	{
		if (isInitialized()){
			MCRXMLContainer returns=new MCRXMLContainer();
			Object cacheObject;
			String key=host+"$"+type+"$"+query;
			cacheObject=queryCache[CACHETYPE_OTHER].
								 getIfUpToDate(key,(System.currentTimeMillis()-timePassed));
			if (cacheObject!=null){
				returns=(MCRXMLContainer)cacheObject;
				logger.debug("Found "+key+" in cache!");
			}
			else{
				if (!host.equals("local"))
					synchronized(comm){
						comm = (MCRRemoteAccessInterface)
						  config.getInstanceOf("MCR.remoteaccess_"+host+"_query_class");
						returns.importElements(comm.requestQuery(host,type,query));
					}
				else
					synchronized(queryint){
						returns.importElements(queryint.getResultList(query,type,maxresults));
					}
				queryCache[CACHETYPE_OTHER].put(key,returns);
				logger.debug("Put "+key+" in cache!");
			}
			return returns;
		}
		return null; //isInitialized()==false
	}
	
	
	/**
	 * clean the cache
	 * this is used by the java garbage collector
	 */
	public synchronized void finalize(){
		queryCache[0].setCapacity(0);
		queryCache[1].setCapacity(0);
		queryCache[0]=null;
		queryCache[1]=null;
		queryCache=null;
		cl=null;
		jdom=null;
		config=null;
		queryint=null;
		timePassed=0;
		timePassed_cl=0;
		initialized=false;
	}
	
	/**
	 * clean the cache
	 * this is just does the same as finally()
	 *
	 */
	public void close(){
		finalize();
	}
	
	public void pullStatus(){
		logger.info(isInitialized()? "MCRQueryCache initialized":"MCRQueryCache not initialized");
		
	}

}
