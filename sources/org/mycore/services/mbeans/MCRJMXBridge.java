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
package org.mycore.services.mbeans;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.mycore.common.MCRConfiguration;

public class MCRJMXBridge {

    public static void registerMe(Object mbean, String type, String component) {
        ObjectName name;
        try {
            name = new ObjectName(MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe-Application").replace(':', ' ') + ":type=" + type
                    + ",component=" + component);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
            return;
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(mbean, name);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
