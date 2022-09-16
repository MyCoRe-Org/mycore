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

package org.mycore.mods.enrichment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mods.merger.MCRMerger;
import org.mycore.mods.merger.MCRMergerFactory;

/**
 * Used to request publication data from a given data source,
 * trying the identifiers supported by this data source one by one.
 * When the data source returns publication data for an identifier,
 * the call is marked successful and the other identifiers are skipped.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRDataSourceCall implements Callable<Boolean> {

    private static final Logger LOGGER = LogManager.getLogger(MCRDataSourceCall.class);

    private MCRDataSource ds;

    private MCRIdentifierPool idPool;

    private Set<MCRIdentifier> consumedIDs = new HashSet<MCRIdentifier>();

    private List<Element> results = new ArrayList<Element>();

    MCRDataSourceCall(MCRDataSource ds, MCRIdentifierPool idPool) {
        this.ds = ds;
        this.idPool = idPool;
    }

    /**
     * Used to request publication data from a given data source,
     * trying the identifiers supported by this data source one by one.
     * When the data source returns publication data for an identifier,
     * the call is marked successful and the other identifiers are skipped.
     *
     * @return true, if the data source returned valid publication data
     */
    public Boolean call() {
        if (!isFinished()) {
            loop: for (MCRIdentifierResolver idResolver : ds.getResolvers()) {
                for (MCRIdentifier id : idPool.getNewIdentifiersOfType(idResolver.getType())) {
                    if (isFinished())
                        break loop;

                    if (!consumedIDs.contains(id)) {
                        consumedIDs.add(id);
                        Element result = idResolver.resolve(id.getValue());
                        if (result != null) {
                            results.add(result);
                        }

                        LOGGER.info(ds + " with " + id + " returned " + (result != null ? "" : "no ") + "valid data");
                    }
                }
            }
        }

        return wasSuccessful();
    }

    boolean wasSuccessful() {
        return !results.isEmpty();
    }

    private boolean isFinished() {
        return ds.shouldStopOnFirstResult() ? wasSuccessful() : false;
    }

    /**
     * Merges the publication data returned by the data source with the existing data
     */
    void mergeResultWith(Element existingData) {
        for (Element result : results) {
            if (existingData.getName().equals("relatedItem")) {
                // resolved is always mods:mods, transform to mods:relatedItem to be mergeable
                result.setName("relatedItem");
                result.setAttribute(existingData.getAttribute("type").clone());
            }

            MCRMerger a = MCRMergerFactory.buildFrom(existingData);
            MCRMerger b = MCRMergerFactory.buildFrom(result);
            a.mergeFrom(b);
        }
    }
}
