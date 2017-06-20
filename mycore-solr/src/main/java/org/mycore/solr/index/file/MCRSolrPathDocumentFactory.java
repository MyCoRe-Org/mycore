/*
 * $Id$
 * $Revision: 5697 $ $Date: May 3, 2013 $
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

package org.mycore.solr.index.file;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

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
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory;
import org.mycore.solr.index.handlers.stream.MCRSolrFileIndexHandler;
import org.mycore.solr.index.handlers.stream.MCRSolrFilesIndexHandler;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrPathDocumentFactory {

    private static final String ACCUMULATOR_LIST_PROPERTY_NAME = CONFIG_PREFIX + "Indexer.File.AccumulatorList";

    private static Logger LOGGER = LogManager.getLogger(MCRSolrPathDocumentFactory.class);

    private static MCRSolrPathDocumentFactory instance = MCRConfiguration.instance()
        .<MCRSolrPathDocumentFactory> getInstanceOf(CONFIG_PREFIX + "SolrInputDocument.Path.Factory", (String) null);

    private static final List<MCRSolrFileIndexAccumulator> ACCUMULATOR_LIST = resolveAccumulators();

    /**
     * @return a list of instances of class listet in {@link #ACCUMULATOR_LIST_PROPERTY_NAME}
     */
    private static List<MCRSolrFileIndexAccumulator> resolveAccumulators() {
        return MCRConfiguration
            .instance()
            .getStrings(ACCUMULATOR_LIST_PROPERTY_NAME)
            .stream()
            .map((accumulatorClassRef) -> {
                try {
                    Class<? extends MCRSolrFileIndexAccumulator> accumulatorClass = Class
                        .forName(accumulatorClassRef)
                        .asSubclass(MCRSolrFileIndexAccumulator.class);

                    return accumulatorClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException(
                        "AccumulatorClass configurated in " + ACCUMULATOR_LIST_PROPERTY_NAME + " not found : "
                            + accumulatorClassRef,
                        e);

                } catch (IllegalAccessException | InstantiationException e) {
                    throw new MCRConfigurationException(
                        "Construxtor of the AccumulatorClass configurated in " + ACCUMULATOR_LIST_PROPERTY_NAME
                            + " can not be invoked.",
                        e);

                }
            }).collect(Collectors.toList());
    }

    public static MCRSolrPathDocumentFactory getInstance() {
        return instance;
    }

    /**
     * Generates a {@link SolrInputDocument} from a {@link MCRPath} instance.
     * 
     * @see MCRSolrFileIndexHandler
     * @see MCRSolrFilesIndexHandler
     * @see MCRSolrIndexHandlerFactory
     */
    public SolrInputDocument getDocument(Path input, BasicFileAttributes attr) throws IOException,
        MCRPersistenceException {
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
            LOGGER.debug("MCRFile " + input.toString() + " transformed to:\n" + doc.toString());
        }

        return doc;
    }

}
