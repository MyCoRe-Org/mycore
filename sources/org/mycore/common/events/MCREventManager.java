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

package org.mycore.common.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * Acts as a multiplexer to forward events that are created to all registered
 * event handlers, in the order that is configured in mycore properties. For
 * information how to configure, see MCREventHandler javadocs.
 * 
 * @see MCREventHandler
 * @see MCREventHandlerBase
 * 
 * @author Frank Lützenkirchen
 */
public class MCREventManager {
	private static Logger logger = Logger.getLogger(MCREventManager.class);

	private static MCREventManager instance;

	/**
	 * The singleton manager instance
	 * 
	 * @return the single event manager
	 */
	public static synchronized MCREventManager instance() {
		if (instance == null)
			instance = new MCREventManager();
		return instance;
	}

	/** Table of all configured event handlers * */
	private Hashtable handlers;

	private MCREventManager() {
		handlers = new Hashtable();
		MCRConfiguration config = MCRConfiguration.instance();

		String prefix = "MCR.EventHandler.";
		String suffix = ".class";

		Properties props = config.getProperties(prefix);
		if( props == null ) return;
		
		List names = new ArrayList();
		names.addAll( props.keySet() );
		Collections.sort( names );
		List instances = null;
		
		for( int i = 0; i < names.size(); i++ )
		{
		  String name = (String)( names.get(i) );
		  if( ! name.endsWith( ".class" ) ) continue;
		  
		  StringTokenizer st = new StringTokenizer( name, "." );
		  st.nextToken(); 
		  st.nextToken();
		  String type = st.nextToken();
		  int nr = Integer.parseInt( st.nextToken() );
		  
		  if( nr == 1 )
		  {
		    instances = new ArrayList();
		    handlers.put(type,instances); 
		  }
		  
		  MCREventHandler handler = (MCREventHandler)(config.getSingleInstanceOf(name));
 		  logger.debug("EventManager instantiating handler " + config.getString(name)
 		      + " for type " + type );
		  instances.add(handler);
		}
	}

	/**
	 * This method is called by the component that created the event and acts as
	 * a multiplexer that invokes all registered event handlers doHandleEvent
	 * methods.
	 * 
	 * @see MCREventHandler#doHandleEvent
	 * @see MCREventHandlerBase
	 * 
	 * @param evt
	 *            the event that happened
	 */
	public void handleEvent(MCREvent evt) throws MCRException {
	    List list = (List)(handlers.get(evt.getObjectType()));
		for (int i = 0; (list != null) && (i < list.size()); i++) {
			MCREventHandler eh = (MCREventHandler) (list.get(i));
			logger.debug("EventManager calling handler "
					+ eh.getClass().getName());
			eh.doHandleEvent(evt);
		}
	}
}
