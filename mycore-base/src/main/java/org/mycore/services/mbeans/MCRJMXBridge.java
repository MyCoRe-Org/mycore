/**
 * 
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
package org.mycore.services.mbeans;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRJMXBridge implements Closeable {

    static final WeakReference<MCRJMXBridge> SINGLETON = new WeakReference<MCRJMXBridge>(new MCRJMXBridge());

    private static final Logger LOGGER = Logger.getLogger(MCRJMXBridge.class);

    private static java.util.List<WeakReference<ObjectName>> ONAME_LIST = new ArrayList<WeakReference<ObjectName>>();

    private static boolean shutdown;

    private MCRJMXBridge() {
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    public static void register(Object mbean, String type, String component) {
        if (shutdown) {
            return;
        }
        ObjectName name;
        try {
            name = getObjectName(type, component);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            return;
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            if (mbs.isRegistered(name)) {
                unregister(type, component);
            }
            mbs.registerMBean(mbean, name);
            ONAME_LIST.add(new WeakReference<ObjectName>(name));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void unregister(String type, String component) {
        if (shutdown) {
            return;
        }
        ObjectName name;
        try {
            name = getObjectName(type, component);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            return;
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            if (mbs.isRegistered(name)) {
                mbs.unregisterMBean(name);
            }
            // As WeakReference does not overwrite Object.equals():
            ONAME_LIST.stream()
                      .filter(wr -> name.equals(wr.get()))
                      .findFirst()
                      .ifPresent(ONAME_LIST::remove);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ObjectName getObjectName(String type, String component) throws MalformedObjectNameException {
        return new ObjectName(MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe-Application").replace(':', ' ') + ":type="
            + type + ",component=" + component);
    }

    public void prepareClose() {
        shutdown = true;
    }

    public void close() {
        LOGGER.debug("Shutting down " + MCRJMXBridge.class.getSimpleName());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Iterator<WeakReference<ObjectName>> wrIterator = ONAME_LIST.iterator();
        while (wrIterator.hasNext()) {
            try {
                ObjectName objectName = wrIterator.next().get();
                LOGGER.debug("Unregister " + objectName.getCanonicalName());
                mbs.unregisterMBean(objectName);
                wrIterator.remove();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SINGLETON.clear();
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY;
    }

    public static ObjectInstance getMBean(String type, String component) throws MalformedObjectNameException, InstanceNotFoundException {
        ObjectName name = getObjectName(type, component);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        if (mbs.isRegistered(name)) {
            return mbs.getObjectInstance(name);
        }

        return null;
    }
}
