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
package org.mycore.mods.classification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.jdom2.Element;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRClassificationMapperBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * A {@link MCRMODSClassificationMapper} maps classifications in MODS documents.
 * To do so, it uses {@link Generator} instances that each implement a strategy
 * to obtain classifications based on the information present in the MODS document.
 * <p>
 * Obtained classification values are added to the MODS document as <code>classification</code> elements
 * with <code>authorityURI</code> and <code>valueURI</code> attributes corresponding to that value and a
 * descriptive <code>generator</code> attribute whose name is returned alongside the classification value
 * (and expanded by suffix <code>-mycore</code>) by 
 * {@link Generator#generateMappings(MCRCategoryDAO, MCRMODSWrapper)}.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRMODSClassificationMapper#GENERATORS_KEY} can be used to
 * specify the map of generators to be used.
 * <li> For each generator, the property suffix {@link MCRSentinel#ENABLED_KEY} can be used to
 * excluded that generator from the configuration.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.mods.classification.MCRMODSClassificationMapper
 * [...].Generators.foo.Class=foo.bar.FooClassificationGenerator
 * [...].Generators.foo.Enabled=true
 * [...].Generators.foo.Key1=Value1
 * [...].Generators.foo.Key2=Value2
 * [...].Generators.bar.Class=foo.bar.BarClassificationGenerator
 * [...].Generators.bar.Enabled=false
 * [...].Generators.bar.Key1=Value1
 * [...].Generators.bar.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRMODSClassificationMapper.Factory.class)
public final class MCRMODSClassificationMapper extends MCRClassificationMapperBase<MCRMODSWrapper, MCRMODSWrapper,
    MCRMODSClassificationMapper.Mapping, MCRMODSClassificationMapper.Generator> {

    public static final String GENERATOR_SUFFIX = "-mycore";

    public static final String EXISTING_MAPPINGS_XPATH = "mods:classification[contains(@generator, '"
        + GENERATOR_SUFFIX + "')]";

    public MCRMODSClassificationMapper(Map<String, Generator> generators) {
        super(generators);
    }

    @Override
    protected Optional<MCRMODSWrapper> getBaseRepresentation(MCRObject object) {
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        return wrapper.getMODS() == null ? Optional.empty() : Optional.of(wrapper);
    }

    @Override
    protected void removeExistingMappings(MCRMODSWrapper wrapper) {
        wrapper.getElements(EXISTING_MAPPINGS_XPATH).forEach(Element::detach);
    }

    @Override
    protected MCRMODSWrapper getIntermediateRepresentation(MCRMODSWrapper wrapper) {
        return wrapper;
    }

    @Override
    protected void addNewMappings(MCRMODSWrapper wrapper, Set<Mapping> mappings) {
        mappings.forEach(mapping -> {
            Element mappedClassification = wrapper.addElement("classification");
            mappedClassification.setAttribute("generator", mapping.generatorName() + GENERATOR_SUFFIX);
            MCRClassMapper.assignCategory(mappedClassification, mapping.categoryId());
        });
    }

    public record Mapping(String generatorName, MCRCategoryID categoryId) {
    }

    public interface Generator extends MCRClassificationMapperBase.Generator<MCRMODSWrapper, Mapping> {

        @Override
        List<Mapping> generateMappings(MCRCategoryDAO dao, MCRMODSWrapper intermediateRepresentation);

    }

    public static class Factory implements Supplier<MCRMODSClassificationMapper> {

        @MCRInstanceMap(name = GENERATORS_KEY, valueClass = Generator.class, sentinel = @MCRSentinel)
        public Map<String, Generator> generators;

        @Override
        public MCRMODSClassificationMapper get() {
            return new MCRMODSClassificationMapper(generators);
        }

    }

}
