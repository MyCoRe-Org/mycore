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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This class is the result list of a XQuery question to the persistence
 * system or remote systems. the result ist transparent over all searched instances of
 * a common instance collection or of a local instance.
 * The method use, depending of the persitence type property,
 * a interface to a query transformer form XQuery to the target system
 * query language.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRQueryCollector {
	private final int nThreads;
	private final PoolWorker[] threads;
	private final LinkedList queue;
	private final MCRQueryAgent agent;

	//	The list of hosts from the configuration
 	private HashSet remoteAliasList = null;

	//	The instcnce of configuraion
 	private org.mycore.common.MCRConfiguration conf = null;


	public MCRQueryCollector(int cThreads, int aThreads)
	{
		nThreads = cThreads;
		queue = new LinkedList();
		threads = new PoolWorker[nThreads];

		for (int i=0; i<nThreads; i++) {
			threads[i] = new PoolWorker();
			threads[i].start();
		}
		// get an instance of configuration
		conf = org.mycore.common.MCRConfiguration.instance();
		agent=new MCRQueryAgent(aThreads,conf);
		// read host list from configuration
		String hostconf = conf.getString("MCR.remoteaccess_hostaliases","local");
		remoteAliasList = new HashSet();
		StringTokenizer tk=new StringTokenizer(hostconf,",");
		while (tk.hasMoreTokens())
			remoteAliasList.add(tk.nextToken());
		System.out.println("MCRCollector initialized");
	}

	/**
	 * Start asynchronous ResultCollection for a query. The results of the query
	 * will be put in the "result" MCRXMLContainer. As this is an asynchronous process
	 * you can work further to a point where you work with "result" again. Sample:
	 * <pre>
	 *  try {
	 *		synchronized(result){
	 *			collector.collectQueryResults(host,type,query,result);
	 *			//any code here between
	 *			result.wait();
	 *		}
	 *	} catch (InterruptedException ignored) {}
	 * </pre>
	 * @param hostlist hostlist seperated by a comma
	 * @param type type of resultobject
	 * @param query obvious the "query"
	 * @param result final result container
	 */
	public void collectQueryResults(String hostlist,String type,String query,MCRXMLContainer result) {
		if (result==null)
			result=new MCRXMLContainer();
		Mission m=new Mission(hostlist,type,query,result);
		synchronized(queue) {
			queue.addLast(m);
			queue.notify();
		}
	}
	
	private class Mission{
		private String hostlist;
		private String type;
		private String query;
		private MCRXMLContainer result;
		private LinkedList hosts;
		
		public Mission(String hostlist, String type, String query, MCRXMLContainer result){
			this.hostlist=hostlist;
			this.type=type;
			this.query=query;
			this.result=result;
			this.hosts=sepHosts(hostlist);
		}
		private LinkedList sepHosts(String hostlist){
			StringTokenizer tk=new StringTokenizer(hostlist,",");
			LinkedList returns=new LinkedList();
			String host=null;
			while (tk.hasMoreTokens()){
				host=tk.nextToken();
				if (remoteAliasList.contains(host))
					returns.addLast(host);
				else if (host.equals("local"))
					returns.addFirst(host);
				else
					throw new MCRException ("Host '"+host+"' is not in the list");
			}
			return returns;
		}
		public LinkedList getHosts(){
			return this.hosts;
		}
		public String getType(){
			return this.type;
		}
		public String getQuery(){
			return this.query;
		}
		public MCRXMLContainer getResultContainer(){
			return this.result;
		}
		public void accomplished(){
			synchronized(result){
				result.notify();
			}
		}
	}
	protected class ThreadCounter {
		private int threadNum;
			
		public ThreadCounter(int threadNum){
			this.threadNum=threadNum;
		}
		public synchronized void decrease(){
			this.threadNum--;
		}
			
	}

	private class PoolWorker extends Thread {
		
		public void run() {
			Mission m;

			while (true) {
				synchronized(queue) {
					while (queue.isEmpty()) {
						try
						{
							queue.wait();
						}
						catch (InterruptedException ignored)
						{
						}
					}

					m = (Mission) queue.removeFirst();
				}

				// If we don't catch RuntimeException, 
				// the pool could leak threads
				try {
					LinkedList hosts=m.getHosts();
					int threadsWait=hosts.size();
					ThreadCounter tc=new ThreadCounter(threadsWait);
					try {
						synchronized (tc){
							while (!hosts.isEmpty()) {
								agent.add((String)hosts.removeFirst(),m.getType(),m.getQuery(),m.getResultContainer(),tc);
							}
							while (tc.threadNum>0){
								tc.wait();
							}
						}
					} catch (InterruptedException ignored) {
						ignored.printStackTrace(System.err);
					}
					m.accomplished();
				}
				catch (RuntimeException e) {
					// You might want to log something here
				}
			}
		}
	}
}
