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
package org.mycore.mods.classification.mapping;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.jdom2.Element;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.mapping.MCRGeneratorClassificationMapperBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRClassMapper;

/**
 * A {@link MCRMODSGeneratorClassificationMapper} maps classifications in MODS documents.
 * To do so, it uses {@link MCRGeneratorClassificationMapperBase.Generator} instances that each
 * implement a strategy to obtain classifications based on the information present in the MODS document.
 * <p>
 * Obtained classification values are added to the MODS document as <code>classification</code> elements
 * with <code>authorityURI</code> and <code>valueURI</code> attributes corresponding to that value and a
 * descriptive <code>generator</code> attribute whose name is returned alongside the classification value
 * (and expanded by suffix <code>-mycore</code>) by 
 * {@link MCRGeneratorClassificationMapperBase.Generator#generate(MCRCategoryDAO, MCRObject)}.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRMODSGeneratorClassificationMapper#GENERATORS_KEY} can be used to
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
@MCRConfigurationProxy(proxyClass = MCRMODSGeneratorClassificationMapper.Factory.class)
public final class MCRMODSGeneratorClassificationMapper extends MCRGeneratorClassificationMapperBase {

    public static final String GENERATOR_SUFFIX = "-mycore";

    public static final String EXISTING_MAPPINGS_XPATH = "mods:classification[contains(@generator, '"
        + GENERATOR_SUFFIX + "')]";

    public MCRMODSGeneratorClassificationMapper(Map<String, Generator> generators) {
        super(generators);
    }

    @Override
    protected boolean isSupported(MCRObject object) {
        return MCRMODSWrapper.isSupported(object);
    }

    @Override
    protected void removeExistingMappings(MCRObject object) {
        new MCRMODSWrapper(object).getElements(EXISTING_MAPPINGS_XPATH).forEach(Element::detach);
    }

    @Override
    protected void insertNewMappings(MCRObject object, Set<Mapping> mappings) {
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
        mappings.forEach(mapping -> {
            Element mappedClassification = modsWrapper.addElement("classification");
            mappedClassification.setAttribute("generator", mapping.generatorName() + GENERATOR_SUFFIX);
            MCRClassMapper.assignCategory(mappedClassification, mapping.categoryId());
        });
    }

    public static class Factory implements Supplier<MCRMODSGeneratorClassificationMapper> {

        @MCRInstanceMap(name = GENERATORS_KEY, valueClass = Generator.class, required = false, sentinel = @MCRSentinel)
        public Map<String, Generator> generators;

        @Override
        public MCRMODSGeneratorClassificationMapper get() {
            return new MCRMODSGeneratorClassificationMapper(generators);
        }

    }

}
