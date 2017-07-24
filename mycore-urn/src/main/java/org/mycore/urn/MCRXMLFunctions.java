/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 17, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.urn;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.parsers.DocumentBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.xml.MCRDOMUtils;
import org.mycore.urn.hibernate.MCRURN;
import org.mycore.urn.hibernate.MCRURNPK_;
import org.mycore.urn.hibernate.MCRURN_;
import org.mycore.urn.services.MCRURNManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@Deprecated
public class MCRXMLFunctions {

    private static Logger LOGGER = LogManager.getLogger(MCRXMLFunctions.class);

    private MCRXMLFunctions() {
    }

    /**
     * @return true if the given object has an urn assigned, false otherwise
     */
    public static boolean hasURNDefined(String objId) {
        if (objId == null) {
            return false;
        }
        try {
            return MCRURNManager.hasURNAssigned(objId);
        } catch (Exception ex) {
            LOGGER.error("Error while retrieving urn from database for object " + objId, ex);
            return false;
        }
    }

    /**
     * Method generates an alternative urn to a given urn by adding additional
     * text to the namespace specific part. Then a new checksum is calculated
     * and attached to the new generated urn<br>
     * <br>
     * Invoking method with
     * <code>"urn:nbn:de:urmel-37e1f5f1-54df-4a9c-8e54-c576f46c01f73"</code> and
     * <code>"dfg"</code> leads to
     * <code>"urn:nbn:de:urmel-dfg-37e1f5f1-54df-4a9c-8e54-c576f46c01f738"</code>
     * 
     * @param urn
     *            the source urn
     * @param toAppend
     *            the string to append to the namespace specific part
     * @return the given urn but to the namespace specific part the value stored
     *         in the <code>toAppend</code> parameter is attached
     */
    public static String createAlternativeURN(String urn, String toAppend) {
        LOGGER.info("Base URN: " + urn + ", adding string '" + toAppend + "'");

        String[] parts = urn.split("-");
        StringBuilder b = new StringBuilder(parts[0] + "-" + toAppend);
        for (int i = 1; i < parts.length; i++) {
            b.append("-" + parts[i]);
        }

        org.mycore.urn.services.MCRURN u = org.mycore.urn.services.MCRURN.create(b.toString());
        return u.toString();
    }

    /**
     * returns the URN for <code>mcrid</code> and children if <code>mcrid</code>
     * is a derivate.
     * 
     * @param mcrid
     *            MCRObjectID of object or derivate
     * @return list of mcrid|file to urn mappings
     */
    @SuppressWarnings("unchecked")
    public static NodeList getURNsForMCRID(String mcrid) {
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            Document document = documentBuilder.newDocument();
            EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<MCRURN> query = cb.createQuery(MCRURN.class);
            Root<MCRURN> root = query.from(MCRURN.class);
            Element rootElement = document.createElement("urn");
            document.appendChild(rootElement);

            LOGGER.info("Getting all urns for object " + mcrid);
            long start = System.currentTimeMillis();
            List<MCRURN> results = em
                .createQuery(
                    query.where(
                        cb.equal(root.get(MCRURN_.key).get(MCRURNPK_.mcrid), mcrid)))
                .getResultList();
            long temp = start;

            LOGGER.debug("This took " + (System.currentTimeMillis() - start) + " ms");
            LOGGER.debug("Processing the result list");

            for (MCRURN result : results) {
                LOGGER.debug("Processing urn " + result.getURN());
                start = System.currentTimeMillis();

                String path = result.getPath();
                String filename = result.getFilename();

                if (path != null && filename != null) {
                    path = path.trim();
                    if (path.length() > 0 && path.charAt(0) == '/') {
                        path = path.substring(1);
                    }

                    path += filename.trim();

                    Element file = document.createElement("file");
                    file.setAttribute("urn", result.getKey().getMcrurn());
                    file.setAttribute("name", path);
                    rootElement.appendChild(file);

                } else {
                    rootElement.setAttribute("mcrid", result.getKey().getMcrid());
                    rootElement.setAttribute("urn", result.getKey().getMcrurn());
                }
                em.detach(result);
                long duration = System.currentTimeMillis() - start;
                LOGGER.debug("URN processed in " + duration + " ms");
            }
            LOGGER.debug("Processing all URN took " + (System.currentTimeMillis() - temp) + " ms");
            return rootElement.getChildNodes();
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    public static String getURNforFile(String derivate, String path) {
        String fileName = path;
        String pathToFile = "/";
        if (path.contains("/")) {
            pathToFile = path.substring(0, path.lastIndexOf("/") + 1);
            fileName = path.substring(path.lastIndexOf("/") + 1);
        }
        return MCRURNManager.getURNForFile(derivate, pathToFile, fileName);
    }
}
