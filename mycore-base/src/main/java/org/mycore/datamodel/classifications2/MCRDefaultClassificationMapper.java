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
package org.mycore.datamodel.classifications2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jdom2.Document;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

/**
 * A {@link MCRDefaultClassificationMapper} maps classifications in default metadata documents.
 * To do so, it uses {@link Generator} instances that each implement a strategy
 * to obtain classifications based on the information present in the default metadata document.
 * <p>
 * Obtained classification values are added to the default metadata document as a <code>mappings</code>
 * element containing <code>mapping</code> elements with <code>categid</code> and <code>classid</code>
 * attributes corresponding to that value.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRClassificationMapperBase#GENERATORS_KEY} can be used to
 * specify the map of generators to be used.
 * <li> For each generator, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that generator from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.datamodel.classifications2.MCRDefaultClassificationMapper
 * [...].Generators.foo.Class=foo.bar.FooClassificationGenerator
 * [...].Generators.foo.Enabled=true
 * [...].Generators.foo.Key1=Value1
 * [...].Generators.foo.Key2=Value2
 * [...].Generators.bar.Class=foo.bar.BarClassificationGenerators
 * [...].Generators.bar.Enabled=false
 * [...].Generators.bar.Key1=Value1
 * [...].Generators.bar.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRDefaultClassificationMapper.Factory.class)
public final class MCRDefaultClassificationMapper extends MCRClassificationMapperBase<MCRObjectMetadata, Document,
    MCRDefaultClassificationMapper.Generator> {

    public static final String ELEMENT_MAPPINGS = "mappings";

    public MCRDefaultClassificationMapper(Map<String, Generator> generators) {
        super(generators);
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
    protected void addNewMappings(MCRObjectMetadata metadata, Set<Mapping> mappedIds) {
        MCRMetaElement newMappings = createEmptyMappingsElement();
        mappedIds.forEach(mapping -> newMappings.addMetaObject(toMetaClassification(mapping.categoryId())));
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

    public interface Generator extends MCRClassificationMapperBase.Generator<Document> {

        @Override
        List<Mapping> generateMappings(MCRCategoryDAO dao, Document intermediateRepresentation);

    }

    public static class Factory implements Supplier<MCRDefaultClassificationMapper> {

        @MCRInstanceMap(name = GENERATORS_KEY, valueClass = Generator.class, sentinel = @MCRSentinel)
        public Map<String, Generator> generators;

        @Override
        public MCRDefaultClassificationMapper get() {
            return new MCRDefaultClassificationMapper(generators);
        }

    }

}
