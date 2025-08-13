
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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * {@link MCRGeneratorClassificationMapperBase} is a base implementation for data model specific
 * implementations of {@link MCRClassificationMapper} that uses multiple {@link Generator}
 * instances that each implement a strategy to obtain classifications based on the information
 * present in a data model representation of the MyCoRe object.
 */
public abstract class MCRGeneratorClassificationMapperBase implements MCRClassificationMapper {

    protected final Logger logger = LogManager.getLogger(getClass());

    public static final String GENERATORS_KEY = "Generators";

    private final Map<String, Generator> generators;

    public MCRGeneratorClassificationMapperBase(Map<String, Generator> generators) {
        this.generators = new HashMap<>(Objects
            .requireNonNull(generators, "Generators must not be null"));
        this.generators.forEach((name, generator) -> Objects
            .requireNonNull(generator, "Generator " + name + "must not be null"));
    }

    @Override
    public final void createMappings(MCRObject object) {
        logger.info("creating mappings for {}", object::getId);
        if (isSupported(object)) {
            replaceMappings(object);
        } else {
            logger.info("object not supported");
        }
    }

    @Override
    public final void clearMappings(MCRObject object) {
        logger.info("clearing mappings in {}", object::getId);
        if (isSupported(object)) {
            removeExistingMappings(object);
        } else {
            logger.info("object not supported");
        }
    }

    private void replaceMappings(MCRObject object) {

        removeExistingMappings(object);

        MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();

        Set<Mapping> mappings = new LinkedHashSet<>();
        generators.forEach((name, generator) -> {
            logger.info("generating mappings with {}", name);
            if (generator.isSupported(object)) {
                mappings.addAll(generator.generate(dao, object));
            } else {
                logger.info("object not supported by generator {}", name);
            }
        });

        if (!mappings.isEmpty()) {
            insertNewMappings(object, mappings);
        }

    }

    protected abstract boolean isSupported(MCRObject object);

    protected abstract void removeExistingMappings(MCRObject object);

    protected abstract void insertNewMappings(MCRObject object, Set<Mapping> mappings);

    public record Mapping(String generatorName, MCRCategoryID categoryId) {
    }

    public interface Generator {

        boolean isSupported(MCRObject object);

        List<Mapping> generate(MCRCategoryDAO dao, MCRObject object);

    }

}
