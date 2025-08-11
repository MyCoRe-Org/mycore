
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
 * that map classifications in MyCoRe objects. To do so it uses {@link Generator} instances that each implement
 * a strategy to obtain classifications based on the information present in an intermediate representation
 * of the MyCoRe object.
*
 * @param <B> A base representation of a MyCoRe object that, if available, can be used to (a) remove all existing
 * mappings, (b) obtain an intermediate representation and (c) add as set of mappings.
 * @param <I> An intermediate representation of the MyCoRe object that is passed to the {@link Generator} instances
 * in order to obtain classifications.
 * @param <V> The type of value that is returned by the {@link Generator} instances.
 * @param <G> The actual {@link Generator} type used by an implementation.
 */
public abstract class MCRClassificationMapperBase<B, I, V, G extends MCRClassificationMapperBase.Generator<I, V>>
    implements MCRClassificationMapper {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String GENERATORS_KEY = "Generators";

    private final Map<String, Generator<I, V>> generators;

    public MCRClassificationMapperBase(Map<String, G> generators) {
        this.generators = new HashMap<>(Objects
            .requireNonNull(generators, "Generators must not be null"));
        this.generators.forEach((name, generator) -> Objects
            .requireNonNull(generator, "Generator " + name + "must not be null"));
    }

    @Override
    public final void createMapping(MCRObject object) {
        logger.info("creating mappings for {}", object::getId);
        getBaseRepresentation(object).ifPresent(this::createMapping);
    }

    @Override
    public final void clearMappings(MCRObject object) {
        logger.info("clearing mappings in {}", object::getId);
        getBaseRepresentation(object).ifPresent(this::removeExistingMappings);
    }

    private void createMapping(B baseRepresentation) {

        removeExistingMappings(baseRepresentation);

        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();

        I intermediateRepresentation = getIntermediateRepresentation(baseRepresentation);
        Set<V> mappedValues = new LinkedHashSet<>();
        generators.forEach((name, generator) -> {
            if (logger.isInfoEnabled()) {
                logger.info("generate mappings with {} / {}", name, generator.getClass().getName());
            }
            mappedValues.addAll(generator.generateMappings(dao, intermediateRepresentation));
        });

        if (!mappedValues.isEmpty()) {
            addNewMappings(baseRepresentation, mappedValues);
        }

        logger.info("checked for mappings");

    }

    protected abstract Optional<B> getBaseRepresentation(MCRObject object);

    protected abstract void removeExistingMappings(B baseRepresentation);

    protected abstract I getIntermediateRepresentation(B baseRepresentation);

    protected abstract void addNewMappings(B baseRepresentation, Set<V> mappedValues);

    public interface Generator<I, V> {

        List<V> generateMappings(MCRCategoryDAO dao, I intermediateRepresentation);

    }

}
