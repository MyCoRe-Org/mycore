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
package org.mycore.common.events;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jdom2.Document;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

/**
 * A {@link MCRClassificationMappingEventHandler} maps classifications in metadata documents.
 * To do so, it uses {@link Mapper} instances that each implement a strategy
 * to obtain classifications based on the information present in the metadata document.
 * <p>
 * Obtained classification values are added to the metadata document as a <code>mappings</code> element containing
 * <code>mapping</code> elements with <code>categid</code> and <code>classid</code> attributes corresponding to
 * that value.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRClassificationMappingEventHandlerBase#MAPPERS_KEY} can be used to
 * specify the map of mappers to be used.
 * <li> For each mapper, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that mapper from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.MCRClassificationMappingEventHandler
 * [...].Mappers.foo.Class=foo.bar.FooClassificationMapper
 * [...].Mappers.foo.Enabled=true
 * [...].Mappers.foo.Key1=Value1
 * [...].Mappers.foo.Key2=Value2
 * [...].Mappers.bar.Class=foo.bar.BarClassificationMapper
 * [...].Mappers.bar.Enabled=false
 * [...].Mappers.bar.Key1=Value1
 * [...].Mappers.bar.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRClassificationMappingEventHandler.Factory.class)
public class MCRClassificationMappingEventHandler extends MCRClassificationMappingEventHandlerBase<MCRObjectMetadata,
    Document, MCRCategoryID, MCRClassificationMappingEventHandler.Mapper> {

    public static final String ELEMENT_MAPPINGS = "mappings";

    public MCRClassificationMappingEventHandler(Map<String, Mapper> mappers) {
        super(mappers);
    }

    @Override
    protected Optional<MCRObjectMetadata> getBaseRepresentation(MCRObject object) {
        return Optional.ofNullable(object.getMetadata());
    }

    @Override
    protected void removeExistingMappings(MCRObjectMetadata metadata) {
        metadata.removeMetadataElement(ELEMENT_MAPPINGS);
    }

    @Override
    protected Document getIntermediateRepresentation(MCRObjectMetadata metadata) {
        return new Document(metadata.createXML().detach());
    }

    @Override
    protected void addNewMappings(MCRObjectMetadata metadata, Set<MCRCategoryID> mappedIds) {
        MCRMetaElement newMappings = createEmptyMappingsElement();
        mappedIds.forEach(categoryId -> newMappings.addMetaObject(toMetaClassification(categoryId)));
        metadata.setMetadataElement(newMappings);
    }

    private MCRMetaElement createEmptyMappingsElement() {
        MCRMetaElement mappings = new MCRMetaElement();
        mappings.setTag(ELEMENT_MAPPINGS);
        mappings.setClass(MCRMetaClassification.class);
        mappings.setHeritable(false);
        mappings.setNotInherit(true);
        return mappings;
    }

    private static MCRMetaClassification toMetaClassification(MCRCategoryID categoryId) {
        return new MCRMetaClassification("mapping", 0, null, categoryId.getRootID(), categoryId.getId());
    }

    public interface Mapper extends MCRClassificationMappingEventHandlerBase.Mapper<Document, MCRCategoryID> {

        @Override
        List<MCRCategoryID> findMappings(MCRCategoryDAO dao, Document intermediateRepresentation);

    }

    public static class Factory implements Supplier<MCRClassificationMappingEventHandler> {

        @MCRInstanceMap(name = MAPPERS_KEY, valueClass = Mapper.class, sentinel = @MCRSentinel)
        public Map<String, Mapper> mappers;

        @Override
        public MCRClassificationMappingEventHandler get() {
            return new MCRClassificationMappingEventHandler(mappers);
        }

    }

}
