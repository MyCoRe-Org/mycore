/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.services.mbeans;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRJMXBridge implements Closeable {

    static final WeakReference<MCRJMXBridge> SINGLETON = new WeakReference<>(new MCRJMXBridge());

    private static final Logger LOGGER = LogManager.getLogger(MCRJMXBridge.class);

    private static java.util.List<WeakReference<ObjectName>> ONAME_LIST = Collections
        .synchronizedList(new ArrayList<WeakReference<ObjectName>>());

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
            ONAME_LIST.add(new WeakReference<>(name));
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
            ONAME_LIST.removeIf(wr -> name.equals(wr.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static ObjectName getObjectName(String type, String component) throws MalformedObjectNameException {
        return new ObjectName(
            MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe-Application").replace(':', ' ')
                + ":type="
                + type + ",component=" + component);
    }

    @Override
    public void prepareClose() {
        shutdown = true;
    }

    @Override
    public void close() {
        LOGGER.debug("Shutting down {}", MCRJMXBridge.class.getSimpleName());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Iterator<WeakReference<ObjectName>> wrIterator = ONAME_LIST.iterator();
        while (wrIterator.hasNext()) {
            try {
                ObjectName objectName = wrIterator.next().get();
                LOGGER.debug("Unregister {}", objectName.getCanonicalName());
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

    public static ObjectInstance getMBean(String type, String component)
        throws MalformedObjectNameException, InstanceNotFoundException {
        ObjectName name = getObjectName(type, component);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        if (mbs.isRegistered(name)) {
            return mbs.getObjectInstance(name);
        }

        return null;
    }
}
