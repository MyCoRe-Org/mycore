/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.solr.index.file;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrPathDocumentFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ACCUMULATOR_LIST_PROPERTY_NAME = SOLR_CONFIG_PREFIX + "Indexer.File.AccumulatorList";

    private static final List<MCRSolrFileIndexAccumulator> ACCUMULATOR_LIST = resolveAccumulators();

    private static final MCRSolrPathDocumentFactory SHARED_INSTANCE = MCRConfiguration2
        .getInstanceOfOrThrow(MCRSolrPathDocumentFactory.class, SOLR_CONFIG_PREFIX + "SolrInputDocument.Path.Factory");

    public static MCRSolrPathDocumentFactory obtainInstance() {
        return SHARED_INSTANCE;
    }

    /**
     * @return a list of instances of class listet in {@link #ACCUMULATOR_LIST_PROPERTY_NAME}
     */
    private static List<MCRSolrFileIndexAccumulator> resolveAccumulators() {
        return MCRConfiguration2.getString(ACCUMULATOR_LIST_PROPERTY_NAME)
            .stream()
            .flatMap(MCRConfiguration2::splitValue)
            .map((accumulatorClassRef) -> {
                try {
                    Class<? extends MCRSolrFileIndexAccumulator> accumulatorClass = Class
                        .forName(accumulatorClassRef)
                        .asSubclass(MCRSolrFileIndexAccumulator.class);

                    return accumulatorClass.getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException(
                        "AccumulatorClass configured in " + ACCUMULATOR_LIST_PROPERTY_NAME + " not found : "
                            + accumulatorClassRef,
                        e);

                } catch (ReflectiveOperationException e) {
                    throw new MCRConfigurationException(
                        "Constructor of the AccumulatorClass configured in " + ACCUMULATOR_LIST_PROPERTY_NAME
                            + " can not be invoked.",
                        e);

                }
            })
            .filter(MCRSolrFileIndexAccumulator::isEnabled)
            .collect(Collectors.toList());
    }

    /**
     * Generates a {@link SolrInputDocument} from a {@link MCRPath} instance.
     * 
     * @see MCRSolrFileIndexHandler
     * @see MCRSolrFilesIndexHandler
     * @see MCRSolrIndexHandlerFactory
     */
    public SolrInputDocument getDocument(Path input, BasicFileAttributes attr) throws MCRPersistenceException {
        SolrInputDocument doc = new SolrInputDocument();

        Consumer<? super MCRSolrFileIndexAccumulator> accumulate = (accumulator) -> {
            LOGGER.debug("{} accumulates {}", accumulator, input);
            try {
                accumulator.accumulate(doc, input, attr);
            } catch (IOException e) {
                LOGGER.error("Error in Accumulator!", e);
            }
        };

        ACCUMULATOR_LIST.forEach(accumulate);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MCRFile {} transformed to:\n{}", input, doc);
        }

        return doc;
    }

}
