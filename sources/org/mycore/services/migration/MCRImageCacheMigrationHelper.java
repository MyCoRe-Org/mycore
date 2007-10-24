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
package org.mycore.services.migration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;

public class MCRImageCacheMigrationHelper {
    private static final Logger LOGGER = Logger.getLogger(MCRImageCacheMigrationHelper.class);

    @SuppressWarnings("unchecked")
    public static void removeImageCacheRoot() {
        // Get the old imgCache root (due to a bug there could be a few)
        final String cacheFolder = "imgCache";
        Map<String, String> propValues = new HashMap<String, String>();
        propValues.put("owner", cacheFolder);
        propValues.put("name", cacheFolder);
        Session session = MCRHIBConnection.instance().getSession();
        List<MCRFSNODES> imgRoots = session.createCriteria(MCRFSNODES.class).add(Restrictions.allEq(propValues)).list();
        if (imgRoots.size() == 0) {
            LOGGER.warn("Cannot find an 'imgCache' root node.");
            return;
        }
        List<String> parentIDs = new LinkedList<String>();
        for (MCRFSNODES rootNode : imgRoots) {
            if (rootNode.getPid() == null) {
                parentIDs.add(rootNode.getId());
            }
        }
        // push every cache entry one level up (as root node)
        List<MCRFSNODES> cacheNodes = session.createCriteria(MCRFSNODES.class).add(Restrictions.in("pid", parentIDs)).list();
        for (MCRFSNODES cacheEntry : cacheNodes) {
            cacheEntry.setPid(imgRoots.get(0).getPid());
            cacheEntry.setName(cacheFolder + cacheEntry.getName());
            cacheEntry.setOwner(cacheEntry.getName());
            // set new owner of 'Thumb', 'Cache', 'Orig'
            List<MCRFSNODES> cachedFiles = session.createCriteria(MCRFSNODES.class).add(Restrictions.eq("pid", cacheEntry.getId())).list();
            for (MCRFSNODES cachedFile : cachedFiles) {
                cachedFile.setOwner(cacheEntry.getOwner());
            }
        }
        // delete old root nodes
        for (MCRFSNODES rootNode : imgRoots) {
            if (rootNode.getPid() == null) {
                session.delete(rootNode);
            }
        }
    }
}
