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
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

/**
 * Used to request publication data from a given data source,
 * trying the identifiers supported by this data source one by one.
 * 
 * Depending on the configuration properties  
 * MCR.MODS.EnrichmentResolver.DefaultStopOnFirstResult=true|false
 * and
 * MCR.MODS.EnrichmentResolver.DataSource.[ID].StopOnFirstResult=true|false
 * the data source will stop trying to resolve publication data after 
 * the first successful call with a given identifier and skip others, 
 * or will try to retrieve data for all given identifiers.
 * 
 * @see MCRDataSource
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRDataSourceCall implements Callable<Boolean> {

    private static final Logger LOGGER = LogManager.getLogger(MCRDataSourceCall.class);

    private MCRDataSource ds;

    private MCRIdentifierPool idPool;

    private List<Element> results = new ArrayList<Element>();

    private boolean gotResults = false;

    MCRDataSourceCall(MCRDataSource ds, MCRIdentifierPool idPool) {
        this.ds = ds;
        this.idPool = idPool;
    }

    /**
     * Used to request publication data from a given data source,
     * trying the identifiers supported by this data source one by one.
     *
     * @return true, if the data source returned valid publication data
     */
    public Boolean call() {
        if (!isFinished()) {
            loop: for (MCRIdentifierResolver idResolver : ds.getResolvers()) {
                for (MCRIdentifier id : idPool.getCurrentIdentifiersOfType(idResolver.getType())) {
                    if (isFinished()) {
                        break loop;
                    }

                    Element result = idResolver.resolve(id.getValue());
                    if (result != null) {
                        gotResults = true;
                        results.add(result);
                        idPool.addIdentifiersFrom(result);
                    }

                    LOGGER.info(ds + " with " + id + " returned " + (result != null ? "" : "no ") + "valid data");
                }
            }
        }

        return wasSuccessful();
    }

    boolean wasSuccessful() {
        return gotResults;
    }

    private boolean isFinished() {
        return ds.shouldStopOnFirstResult() ? wasSuccessful() : false;
    }

    List<Element> getResults() {
        return results;
    }

    void clearResults() {
        results.clear();
    }
}
