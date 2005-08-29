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
package org.mycore.access;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

//import org.mycore.access.MCRAccessCtrlStore;

/**
 * The purpose of this interface is to make the choice of the persistence layer
 * configurable. Any concrete database-class which stores MyCoRe Access control
 * must implement this interface. Which database actually will be used can then
 * be configured by reading the value <code>MCR.accessstore_class_name</code>
 * from mycore.properties.
 * 
 * @author Arne Seifert
 * @version $Revision$ $Date$
 */
public abstract class MCRAccessStore {

    public abstract void createTables();
    public abstract MCRAccessRule getRule(String ruleID);
    public abstract String getRuleID(String objID, String ACPool);

    static private MCRAccessStore implementation;
    public static MCRAccessStore getInstance() 
    {
        if(implementation == null) {
            implementation = (MCRAccessStore)MCRConfiguration.instance().getSingleInstanceOf("MCR.accessstore_class_name", "org.mycore.access.MCRSQLAccessStore");
        }
        return implementation;
    }
}
