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

package org.mycore.mets.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Factory to create mets generator's. By default, this class uses the property 'MCR.Component.MetsMods.Generator' to
 * determine which generator is chosen. You can use either use {@link #setSelector(MCRMETSGeneratorSelector)} to add
 * your own selector or use the property 'MCR.Component.MetsMods.Generator.Selector'.
 *
 * @author Matthias Eichner
 */
public final class MCRMETSGeneratorFactory {

    private static MCRMETSGeneratorSelector generatorSelector;

    private static boolean ignoreMetsXml;

    static {
        // ignore mets.xml of the derivate by default
        ignoreMetsXml = true;

        // get selector
        Class<? extends MCRMETSGeneratorSelector> cn = MCRConfiguration2.<MCRMETSPropertyGeneratorSelector>getClass(
            "MCR.Component.MetsMods.Generator.Selector").orElse(MCRMETSPropertyGeneratorSelector.class);
        try {
            generatorSelector = cn.getDeclaredConstructor().newInstance();
        } catch (Exception cause) {
            generatorSelector = new MCRMETSPropertyGeneratorSelector();
            throw new MCRException(
                "Unable to instantiate " + cn + ". Please check the 'MCR.Component.MetsMods.Generator.Selector'."
                    + " Using default MCRMETSPropertyGeneratorSelector.",
                cause);
        }
    }

    private MCRMETSGeneratorFactory() {
    }

    /**
     * Returns a generator for the given derivate.
     *
     * @param derivatePath path to the derivate
     * @return new created generator
     * @throws MCRException the generator could not be instantiated
     */
    public static MCRMETSGenerator create(MCRPath derivatePath) throws MCRException {
        return create(derivatePath, new HashSet<>());
    }

    /**
     * Returns a generator for the given derivate.
     *
     * @param derivatePath path to the derivate
     * @param ignorePaths set of paths which should be ignored when generating the mets.xml
     * @return new created generator
     * @throws MCRException the generator could not be instantiated
     */
    public static MCRMETSGenerator create(MCRPath derivatePath, Set<MCRPath> ignorePaths) throws MCRException {
        MCRMETSGenerator generator = generatorSelector.get(derivatePath);
        if (generator instanceof MCRMETSAbstractGenerator abstractGenerator) {
            abstractGenerator.setDerivatePath(derivatePath);
            if (ignoreMetsXml) {
                ignorePaths.add(MCRPath.ofPath(derivatePath.resolve("mets.xml")));
            }
            abstractGenerator.setIgnorePaths(ignorePaths);
            try {
                getOldMets(derivatePath).ifPresent(abstractGenerator::setOldMets);
            } catch (Exception exc) {
                // we should not fail if the old mets.xml is broken
                LogManager.getLogger().error(() -> "Unable to read mets.xml of " + derivatePath.getOwner(), exc);
            }
        }
        return generator;
    }

    /**
     * If the mets.xml should be ignored for generating the mets.xml or not.
     *
     * @param ignore if the mets.xml should be added to the ignorePaths by default
     */
    public static void ignoreMetsXML(boolean ignore) {
        ignoreMetsXml = ignore;
    }

    /**
     * Checks if the mets.xml in the derivate is added to the ignorePaths by default. Ignoring is the default behaviour.
     * If the mets.xml is not ignored, then the old mets.xml in the derivate will appear in the newly generated.
     *
     * @return true if the mets.xml is ignored.
     */
    public static boolean isMetsXMLIgnored() {
        return ignoreMetsXml;
    }

    private static Optional<Mets> getOldMets(MCRPath derivatePath) throws IOException, JDOMException {
        if (derivatePath == null) {
            return Optional.empty();
        }
        Path metsPath = derivatePath.resolve("mets.xml");
        if (!Files.exists(metsPath)) {
            return Optional.empty();
        }
        SAXBuilder builder = new SAXBuilder();
        try (InputStream is = Files.newInputStream(metsPath)) {
            Document metsDocument = builder.build(is);
            return Optional.of(new Mets(metsDocument));
        }
    }

    /**
     * Sets a new selector for the factory.
     *
     * @param selector the selector which determines which generator is chosen
     */
    public static synchronized void setSelector(MCRMETSGeneratorSelector selector) {
        generatorSelector = selector;
    }

    /**
     * Base interface to select which mets generator should be chosen.
     */
    public interface MCRMETSGeneratorSelector {

        /**
         * Returns the generator.
         *
         * @param derivatePath path to the derivate
         * @return the generator
         * @throws MCRException something went wrong while getting the generator
         */
        MCRMETSGenerator get(MCRPath derivatePath) throws MCRException;

    }

    /**
     * The default selector. Selects the generator by the property 'MCR.Component.MetsMods.Generator'.
     */
    public static class MCRMETSPropertyGeneratorSelector implements MCRMETSGeneratorSelector {

        private static Class<? extends MCRMETSGenerator> metsGeneratorClass;

        @Override
        public MCRMETSGenerator get(MCRPath derivatePath) {
            String cn = MCRConfiguration2.getString("MCR.Component.MetsMods.Generator")
                .orElse(MCRMETSDefaultGenerator.class.getName());
            try {
                if (metsGeneratorClass == null || !cn.equals(metsGeneratorClass.getName())) {
                    metsGeneratorClass = MCRClassTools.forName(cn);
                }
                return metsGeneratorClass.getDeclaredConstructor().newInstance();
            } catch (Exception cause) {
                throw new MCRException("Unable to instantiate " + cn
                    + ". Please check the 'MCR.Component.MetsMods.Generator' property'.", cause);
            }
        }
    }

}
