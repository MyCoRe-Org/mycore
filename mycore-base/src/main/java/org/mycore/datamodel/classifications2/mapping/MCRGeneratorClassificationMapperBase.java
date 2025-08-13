
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
package org.mycore.datamodel.classifications2.mapping;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * {@link MCRGeneratorClassificationMapperBase} is a base implementation for data model specific event handlers
 * that map classifications in MyCoRe objects. To do so it uses {@link MCRGeneratorClassificationMapperBase.Generator}
 * instances that each implement a strategy to obtain classifications based on the information present in a
 * data model representation of the MyCoRe object.
*
 * @param <D> A data mode representation of a MyCoRe object that, if available, can be used to
 * (a) remove all existing mappings, (b) obtain new mappings and (c) add the set of mappings.
 * @param <G> The actual {@link MCRGeneratorClassificationMapperBase.Generator} type used by an implementation.
 */
public abstract class MCRGeneratorClassificationMapperBase<D,
    G extends MCRGeneratorClassificationMapperBase.Generator<D>> implements MCRClassificationMapper {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String GENERATORS_KEY = "Generators";

    private final Map<String, Generator<D>> generators;

    public MCRGeneratorClassificationMapperBase(Map<String, G> generators) {
        this.generators = new HashMap<>(Objects
            .requireNonNull(generators, "Generators must not be null"));
        this.generators.forEach((name, generator) -> Objects
            .requireNonNull(generator, "Generator " + name + "must not be null"));
    }

    @Override
    public final void createMappings(MCRObject object) {
        logger.info("creating mappings for {}", object::getId);
        getDataModel(object).ifPresent(dataModel -> createMappings(object, dataModel));
    }

    @Override
    public final void clearMappings(MCRObject object) {
        logger.info("clearing mappings in {}", object::getId);
        getDataModel(object).ifPresent(dataModel -> removeExistingMappings(object, dataModel));
    }

    private void createMappings(MCRObject object, D dataModel) {

        removeExistingMappings(object, dataModel);

        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();

        Set<Mapping> mappings = new LinkedHashSet<>();
        generators.forEach((name, generator) -> {
            if (logger.isInfoEnabled()) {
                logger.info("generate mappings with {} / {}", name, generator.getClass().getName());
            }
            mappings.addAll(generator.generate(dao, object, dataModel));
        });

        if (!mappings.isEmpty()) {
            insertNewMappings(object, dataModel, mappings);
        }

        logger.info("checked for mappings");

    }

    protected abstract Optional<D> getDataModel(MCRObject object);

    protected abstract void removeExistingMappings(MCRObject object, D dataModel);

    protected abstract void insertNewMappings(MCRObject object, D dataModel, Set<Mapping> mappings);

    public record Mapping(String generatorName, MCRCategoryID categoryId) {
    }

    public interface Generator<D> {

        List<Mapping> generate(MCRCategoryDAO dao, MCRObject object, D dataModel);
        
    }

}
