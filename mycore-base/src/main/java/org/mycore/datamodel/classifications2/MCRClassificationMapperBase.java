
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * {@link MCRClassificationMapperBase} is a base implementation for data model specific event handlers
 * that map classifications in MyCoRe objects. To do so it uses {@link MCRClassificationMapperBase.Generator}
 * instances that each implement a strategy to obtain classifications based on the information present in an
 * alternate representation of the MyCoRe object.
*
 * @param <R> An alternate representation of a MyCoRe object that, if available, can be used to
 * (a) remove all existing mappings, (b) obtain new mappings and (c) add the set of mappings.
 * @param <G> The actual {@link MCRClassificationMapperBase.Generator} type used by an implementation.
 */
public abstract class MCRClassificationMapperBase<R, G extends MCRClassificationMapperBase.Generator<R>>
    implements MCRClassificationMapper {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String GENERATORS_KEY = "Generators";

    private final Map<String, Generator<R>> generators;

    public MCRClassificationMapperBase(Map<String, G> generators) {
        this.generators = new HashMap<>(Objects
            .requireNonNull(generators, "Generators must not be null"));
        this.generators.forEach((name, generator) -> Objects
            .requireNonNull(generator, "Generator " + name + "must not be null"));
    }

    @Override
    public final void createMapping(MCRObject object) {
        logger.info("creating mappings for {}", object::getId);
        getRepresentation(object).ifPresent(representation -> createMapping(object, representation));
    }

    @Override
    public final void clearMappings(MCRObject object) {
        logger.info("clearing mappings in {}", object::getId);
        getRepresentation(object).ifPresent(representation -> removeExistingMappings(object, representation));
    }

    private void createMapping(MCRObject object, R representation) {

        removeExistingMappings(object, representation);

        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();

        Set<Mapping> mappings = new LinkedHashSet<>();
        generators.forEach((name, generator) -> {
            if (logger.isInfoEnabled()) {
                logger.info("generate mappings with {} / {}", name, generator.getClass().getName());
            }
            mappings.addAll(generator.generateMappings(dao, representation));
        });

        if (!mappings.isEmpty()) {
            addNewMappings(object, representation, mappings);
        }

        logger.info("checked for mappings");

    }

    protected abstract Optional<R> getRepresentation(MCRObject object);

    protected abstract void removeExistingMappings(MCRObject object, R representation);

    protected abstract void addNewMappings(MCRObject object, R representation, Set<Mapping> mappings);

    public record Mapping(String generatorName, MCRCategoryID categoryId) {
    }

    public interface Generator<R> {

        List<Mapping> generateMappings(MCRCategoryDAO dao, R representation);

    }

}
