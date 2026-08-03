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

import static jakarta.servlet.ServletContext.ORDERED_LIBS;
import static org.mycore.common.config.MCRRuntimeComponentDetector.ComponentOrder.LOWEST_PRIORITY_FIRST;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDirSetup;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.log.MCRTableMessage;

import jakarta.servlet.ServletContext;

/**
 * Initializes classes that implement {@link AutoExecutable} interface that are defined via
 * <code>MCR.Startup.Class</code> property.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRStartupHandler {

    /**
     * Can set <code>true</code> or <code>false</code> as {@link ServletContext#setAttribute(String, Object)} to skip
     * errors on startup.
     */
    public static final String HALT_ON_ERROR = "MCR.Startup.haltOnError";

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean isWebApp;

    public static void startUp(ServletContext servletContext) {
        //setup configuration
        MCRConfigurationDirSetup dirSetup = new MCRConfigurationDirSetup();
        dirSetup.startUp(servletContext);
        isWebApp = servletContext != null;
        //initialize ClassLoader here, so it can be used later reliably.
        MCRClassTools.updateClassLoader();

        ClassLoader resourceClassLoader = MCRClassTools.getClassLoader();
        LOGGER.info("The following ClassLoader is used: {}", resourceClassLoader);

        MCRTableMessage<MCRComponent> componentTable = new MCRTableMessage<>(
            new MCRTableMessage.Column<>("Type", MCRStartupHandler::toType),
            new MCRTableMessage.Column<>("Name", MCRComponent::getFullName),
            new MCRTableMessage.Column<>("Priority", MCRComponent::getPriority),
            new MCRTableMessage.Column<>("Version", MCRStartupHandler::toVersion),
            new MCRTableMessage.Column<>("Build time", MCRStartupHandler::toManifestModificationDate),
            new MCRTableMessage.Column<>("Location", MCRStartupHandler::toJarFile));
        MCRRuntimeComponentDetector.getAllComponents(LOWEST_PRIORITY_FIRST).forEach(componentTable::add);
        LOGGER.info(() -> componentTable.logMessage("Detected components:"));

        if (servletContext != null) {
            LOGGER.info("Library order: {}", () -> servletContext.getAttribute(ORDERED_LIBS));
        }

        if (MCRConfiguration2.getBoolean("MCR.Startup.CheckClassProperties").orElse(false)) {
            SortedSet<PropertyClassStatus> propertyClassStatuses = checkClassProperties();
            if (!propertyClassStatuses.isEmpty()) {
                MCRTableMessage<PropertyClassStatus> propertyTable = new MCRTableMessage<>(
                    new MCRTableMessage.Column<>("Property", PropertyClassStatus::property),
                    new MCRTableMessage.Column<>("Class", PropertyClassStatus::className),
                    new MCRTableMessage.Column<>("Status", PropertyClassStatus::status));
                propertyClassStatuses.forEach(propertyTable::add);
                LOGGER.warn(() -> propertyTable.logMessage("Configured Classes report:"));
            }
        }

        MCRTableMessage<AutoExecutable> executableTable = new MCRTableMessage<>(
            new MCRTableMessage.Column<>("Name", AutoExecutable::getName),
            new MCRTableMessage.Column<>("Priority", AutoExecutable::getPriority),
            new MCRTableMessage.Column<>("Class", executable -> executable.getClass().getName()));

        List<AutoExecutable> executables = MCRConfiguration2
            .instantiateClasses(AutoExecutable.class, "MCR.Startup.Class")
            .sorted()
            .peek(executableTable::add)
            .toList();

        LOGGER.info(() -> executableTable.logMessage("Detected auto executables:"));
        executables.forEach(autoExecutable -> startExecutable(servletContext, autoExecutable));

    }

    private static SortedSet<PropertyClassStatus> checkClassProperties() {
        Pattern classPattern = Pattern.compile(MCRConfiguration2.getString("MCR.Startup.CheckClassRegEx").orElse("^$"));
        return MCRConfiguration2.getPropertiesMap()
            .entrySet()
            .stream()
            .filter(entry -> isClassProperty(entry.getKey(), entry.getValue(), classPattern))
            .flatMap(entry -> MCRConfiguration2.splitValue(entry.getValue())
                .map(value -> new PropertyClassStatus(entry.getKey(), value, null)))
            .filter(pcs -> !pcs.className.isBlank())
            .map(pcs -> {
                try {
                    Class<?> aClass = Class.forName(pcs.className(), false, MCRClassTools.getClassLoader());
                    if (aClass.getAnnotation(Deprecated.class) != null) {
                        return new PropertyClassStatus(pcs.property(), pcs.className(), ClassStatus.DEPRECATED);
                    }
                } catch (ClassNotFoundException | LinkageError e) {
                    return new PropertyClassStatus(pcs.property(), pcs.className(), ClassStatus.NOT_FOUND);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean isClassProperty(String key, String value, Pattern classPattern) {
        return key.endsWith(".Class") || classPattern.matcher(value).find();
    }

    private static String toType(MCRComponent component) {
        if (component.isMyCoReBaseComponent()) {
            return "MyCoRe base component";
        } else if (component.isMyCoReComponent()) {
            return "MyCoRe component";
        } else {
            return "Application module";
        }
    }

    private static Object toVersion(MCRComponent component) {
        String version = component.getManifestMainAttribute("Implementation-Version");
        return version != null ? version : "n/a";
    }

    private static Object toJarFile(MCRComponent component) {
        return component.getJarFile().getAbsolutePath();
    }

    private static Object toManifestModificationDate(MCRComponent component) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try (JarFile jarFile = new JarFile(component.getJarFile())) {
            return formatter.format(jarFile.getEntry("META-INF/MANIFEST.MF").getLastModifiedTime()
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        } catch (IOException e) {
            return "n/a";
        }
    }

    public static boolean isWebApp() {
        return isWebApp;
    }

    private static void startExecutable(ServletContext servletContext, AutoExecutable autoExecutable) {
        LOGGER.info("{}: Starting {}", autoExecutable::getPriority, autoExecutable::getName);
        try {
            autoExecutable.startUp(servletContext);
        } catch (ExceptionInInitializerError | RuntimeException e) {
            boolean haltOnError = servletContext == null || servletContext.getAttribute(HALT_ON_ERROR) == null
                || Boolean.parseBoolean((String) servletContext.getAttribute(HALT_ON_ERROR));

            if (haltOnError) {
                throw e;
            }
            LOGGER.warn(e::toString);
        }
    }

    public interface AutoExecutable extends Comparable<AutoExecutable> {
        /**
         * returns a name to display on start-up.
         */
        String getName();

        /**
         * If order is important returns as 'heigher' priority.
         */
        int getPriority();

        /**
         * This method get executed by {@link MCRStartupHandler#startUp(ServletContext)}
         */
        void startUp(ServletContext servletContext);

        @Override
        default int compareTo(AutoExecutable other) {
            return Integer.compare(other.getPriority(), getPriority());
        }

    }

    private enum ClassStatus {
        NOT_FOUND("not found"), DEPRECATED("deprecated");

        private final String tableValue;

        ClassStatus(String tableValue) {
            this.tableValue = tableValue;
        }

        @Override
        public String toString() {
            return tableValue;
        }
    }

    private record PropertyClassStatus(String property, String className, ClassStatus status)
        implements Comparable<PropertyClassStatus> {

        private static final Comparator<PropertyClassStatus> COMPARATOR =
            Comparator.comparing(PropertyClassStatus::status)
                .thenComparing(PropertyClassStatus::property)
                .thenComparing(PropertyClassStatus::className);

        @Override
        public int compareTo(PropertyClassStatus other) {
            return COMPARATOR.compare(this, other);
        }
    }
}
