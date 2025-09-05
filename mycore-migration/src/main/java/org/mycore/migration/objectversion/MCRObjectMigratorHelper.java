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

package org.mycore.migration.objectversion;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRExpandedObjectStructure;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.migration.cli.MCRMigrationCommands;
import org.mycore.migration.strategy.MCRChildrenOrderMigrationStrategy;
import org.mycore.migration.strategy.MCRNeverAddChildrenOrderStrategy;

public class MCRObjectMigratorHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Get the configured strategy for migrating &lt;children&gt; to &lt;childrenOrder&gt;.
     * If no strategy is configured, a default strategy is returned that never migrates &lt;children&gt; to &lt;childrenOrder&gt;.
     *
     * @return the configured strategy or a default strategy
     */
    public static MCRChildrenOrderMigrationStrategy getChildrenOrderMigrationStrategy() {
        return MCRConfiguration2
            .getSingleInstanceOf(MCRChildrenOrderMigrationStrategy.class,
                MCRMigrationCommands.CHILDREN_ORDER_STRATEGY_PROPERTY)
            .orElseGet(() -> {
                LOGGER.info("No strategy configured for '{}', using default: NeverAddChildrenOrderStrategy",
                    MCRMigrationCommands.CHILDREN_ORDER_STRATEGY_PROPERTY);
                return new MCRNeverAddChildrenOrderStrategy();
            });
    }

    /**
     * Migrate the given document representing an object to the latest structure version.
     * This includes:
     * <ul>
     *   <li>Optionally migrating &lt;children&gt; to &lt;childrenOrder&gt; based on the given strategy</li>
     *   <li>Repairing derivate links if requested</li>
     *   <li>Validating the object after migration if requested</li>
     *   <li>Normalizing the object structure</li>
     * </ul>
     *
     * @param objectID the ID of the object being migrated
     * @param document the XML document of the object to migrate
     * @param strategy the strategy to decide whether to migrate &lt;children&gt; to &lt;childrenOrder&gt;
     * @param repairDerivateLinks whether to repair derivate links
     * @param validate whether to validate the object after migration
     * @return the migrated XML document
     * @throws JDOMException if there is an error parsing or manipulating the XML
     * @throws IOException if there is an I/O error during processing
     */
    public static Document migrateObject(MCRObjectID objectID, Document document,
        MCRChildrenOrderMigrationStrategy strategy, boolean repairDerivateLinks, boolean validate)
        throws JDOMException, IOException {
        Element rootElement = document.getRootElement();

        Element structureElement = rootElement.getChild(MCRObjectStructure.XML_NAME);
        if (structureElement != null) {

            Element derivatesElement = structureElement.getChild(MCRObjectStructure.ELEMENT_DERIVATE_OBJECTS);

            if (derivatesElement != null && repairDerivateLinks) {
                // trigger repair on all derivates to recreate the derivate links in the right direction
                List<Element> derObjects = derivatesElement.getChildren("derobject");
                derObjects.stream()
                    .map(derObject -> derObject.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE))
                    .map(MCRObjectID::getInstance)
                    .map(MCRMetadataManager::retrieveMCRDerivate)
                    .forEach(MCRMetadataManager::fireRepairEvent);
            }

            Element childrenElement = structureElement.getChild(MCRExpandedObjectStructure.CHILDREN_ELEMENT_NAME);
            if (childrenElement != null) {
                if (strategy.shouldAddChildrenOrder(objectID, document)) {
                    LOGGER.info("Migrating <children> to <childrenOrder> for object {} based on strategy {}",
                        () -> objectID, () -> strategy.getClass().getSimpleName());
                    childrenElement.setName(MCRObjectStructure.CHILDREN_ORDER_ELEMENT_NAME);
                    List<Element> children = childrenElement.getChildren(MCRObjectStructure.CHILD_ELEMENT_NAME);

                    for (Element child : children) {
                        child.removeAttribute("title", MCRConstants.XLINK_NAMESPACE);
                        child.removeAttribute("inherited");
                    }
                } else {
                    LOGGER.info("Skipping <children> migration for object {} based on strategy {}",
                        () -> objectID, () -> strategy.getClass().getSimpleName());
                    // Remove the old <children> element as it's not needed in the normalized structure
                    // and the strategy decided against migrating it to <childrenOrder>.
                    // MCRMetadataManager.normalizeObject will handle the structure correctly later.
                    // However, explicitly removing it here makes the intent clearer for this migration step.
                    structureElement.removeChild(MCRExpandedObjectStructure.CHILDREN_ELEMENT_NAME);
                }
            }
        }

        MCRObject object = new MCRObject(document);

        if (validate) {
            MCRMetadataManager.validateObject(object);
        }
        MCRMetadataManager.normalizeObject(object);

        return object.createXML();
    }
}
