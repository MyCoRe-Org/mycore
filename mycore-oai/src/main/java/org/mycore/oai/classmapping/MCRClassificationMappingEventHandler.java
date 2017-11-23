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
package org.mycore.oai.classmapping;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This class implements an event handler, which reloads classification entries
 * stored in datafield mappings/mapping. These entries are retrieved from other 
 * classifications where they are stored in as labels with language "x-mapping".
 * 
 * @author Robert Stephan
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationMappingEventHandler extends MCREventHandlerBase {

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static Logger LOGGER = LogManager.getLogger(MCRClassificationMappingEventHandler.class);

    private MCRMetaElement oldMappings = null;

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    @Override
    protected void undoObjectUpdated(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    @Override
    protected void undoObjectRepaired(MCREvent evt, MCRObject obj) {
        undo(obj);
    }

    private void createMapping(MCRObject obj) {
        MCRMetaElement mappings = obj.getMetadata().getMetadataElement("mappings");
        if (mappings != null) {
            oldMappings = mappings.clone();
            obj.getMetadata().removeMetadataElement("mappings");
        }

        Element currentClassElement = null;
        try {
            Document doc = new Document(obj.getMetadata().createXML().detach());
            XPathExpression<Element> classElementPath = XPathFactory.instance().compile("//*[@categid]",
                Filters.element());
            List<Element> classList = classElementPath.evaluate(doc);
            if (classList.size() > 0) {
                mappings = new MCRMetaElement();
                mappings.setTag("mappings");
                mappings.setClass(MCRMetaClassification.class);
                mappings.setHeritable(false);
                mappings.setNotInherit(true);
                obj.getMetadata().setMetadataElement(mappings);
            }
            for (Element classElement : classList) {
                currentClassElement = classElement;
                MCRCategory categ = DAO.getCategory(new MCRCategoryID(classElement.getAttributeValue("classid"),
                    classElement.getAttributeValue("categid")), 0);
                addMappings(mappings, categ);
            }
        } catch (Exception je) {
            if (currentClassElement == null) {
                LOGGER.error("Error while finding classification elements", je);
            } else {
                LOGGER.error("Error while finding classification elements for {}",
                    new XMLOutputter().outputString(currentClassElement), je);
            }
        } finally {
            if (mappings == null || mappings.size() == 0) {
                obj.getMetadata().removeMetadataElement("mappings");
            }
        }
    }

    private void addMappings(MCRMetaElement mappings, MCRCategory categ) {
        if (categ != null) {
            categ.getLabel("x-mapping").ifPresent(label -> {
                String[] str = label.getText().split("\\s");
                for (String s : str) {
                    if (s.contains(":")) {
                        String[] mapClass = s.split(":");
                        MCRMetaClassification metaClass = new MCRMetaClassification("mapping", 0, null, mapClass[0],
                            mapClass[1]);
                        mappings.addMetaObject(metaClass);
                    }
                }
            });
        }
    }

    private void undo(MCRObject obj) {
        if (oldMappings == null) {
            obj.getMetadata().removeMetadataElement("mappings");
        } else {
            MCRMetaElement mmap = obj.getMetadata().getMetadataElement("mappings");

            for (int i = 0; i < oldMappings.size(); i++) {
                mmap.addMetaObject(oldMappings.getElement(i));
            }
        }
    }
}
