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

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.mods.MCRMODSSorter;
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

    private Element result;

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
     * @return true, if the data source returned publication data
     */
    public Boolean call() {
        if (!wasSuccessful()) {
            for (MCRIdentifierResolver idResolver : ds.getResolvers()) {
                MCRIdentifierType type = idResolver.getType();
                for (MCRIdentifier id : idPool.getIdentifiersOfType(type)) {
                    result = idResolver.resolve(id.getValue());
                    LOGGER.info(ds.getID() + " with " + id + " returned " + (wasSuccessful() ? "" : "no ") + "data ");
                    if (wasSuccessful()) {
                        MCRMODSSorter.sort(result);
                        idPool.addIdentifiersFrom(result);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    boolean wasSuccessful() {
        return result != null;
    }

    /**
     * Merges the publication data returned by the data source with the existing data
     */
    void mergeResultWith(Element existingData) {
        if (existingData.getName().equals("relatedItem")) {
            // resolved is always mods:mods, transform to mods:relatedItem to be mergeable
            result.setName("relatedItem");
            result.setAttribute(existingData.getAttribute("type").clone());
        }

        MCRMerger a = MCRMergerFactory.buildFrom(existingData);
        MCRMerger b = MCRMergerFactory.buildFrom(result);
        a.mergeFrom(b);
    }

    void reset() {
        this.result = null;
    }
}
