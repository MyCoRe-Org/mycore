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

package org.mycore.common.content.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xsl.MCRSAXTransformerFactoryManager;
import org.mycore.common.xsl.MCRXalanTransformerFactory;
import org.mycore.common.xsl.MCRTransformerFactorySelector;
import org.mycore.common.xsl.uriresolver.MCRXSLStyleURIResolver.Flavor;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRXSLTransformerTest {

    private static final String TRANSFORMER_PREFIX = "MCR.ContentTransformer.Legacy";

    private static final String LAYOUT_FACTORY_PROPERTY = "MCR.LayoutService.TransformerFactory";

    private static final String FO_FACTORY_PROPERTY = "MCR.LayoutService.FoFormatter.TransformerFactory";

    private static final String LEGACY_FO_FACTORY_PROPERTY = "MCR.LayoutService.FoFormatter.transformerFactoryImpl";

    private static final String FLAVOR_PREFIX = "MCR.Test.LegacyFlavor";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = TRANSFORMER_PREFIX + ".Stylesheet", string = "unused.xsl"),
        @MCRTestProperty(
            key = TRANSFORMER_PREFIX + ".TransformerFactoryClass",
            classNameOf = TransformerFactoryImpl.class)
    })
    public void warnsAboutLegacyTransformerFactoryClassProperty() {
        List<String> warnings = collectWarnings(() -> new MCRXSLTransformer().init("Legacy"));

        assertEquals(List.of("Configuration property '" + TRANSFORMER_PREFIX
            + ".TransformerFactoryClass' is deprecated. Replace it with '" + TRANSFORMER_PREFIX
            + ".TransformerFactory=SlowXalan'."), warnings);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_FACTORY_PROPERTY, empty = true),
        @MCRTestProperty(key = LAYOUT_FACTORY_PROPERTY + "Class", classNameOf = TransformerFactoryImpl.class),
        @MCRTestProperty(key = LEGACY_FO_FACTORY_PROPERTY, classNameOf = TransformerFactoryImpl.class)
    })
    public void supportsLegacyLayoutAndFoFactoryProperties() {
        List<String> warnings = collectWarnings(() -> {
            String defaultFactoryId = MCRTransformerFactorySelector.getDefaultFactoryId();
            assertEquals("SlowXalan", defaultFactoryId);
            assertEquals("SlowXalan", MCRTransformerFactorySelector.getFactoryId(FO_FACTORY_PROPERTY,
                LEGACY_FO_FACTORY_PROPERTY, defaultFactoryId));
        });

        assertEquals(List.of(
            "Configuration property 'MCR.LayoutService.TransformerFactoryClass' is deprecated. Replace it with "
                + "'MCR.LayoutService.TransformerFactory=SlowXalan'.",
            "Configuration property 'MCR.LayoutService.FoFormatter.transformerFactoryImpl' is deprecated. Replace "
                + "it with 'MCR.LayoutService.FoFormatter.TransformerFactory=SlowXalan'."),
            warnings);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_FACTORY_PROPERTY, string = "Xalan")
    })
    public void resolvesDefaultFactoryIdAtCallTime() {
        assertEquals("Xalan", MCRTransformerFactorySelector.getDefaultFactoryId());
        assertSame(MCRSAXTransformerFactoryManager.obtainInstance("Xalan"),
            MCRSAXTransformerFactoryManager.obtainInstance());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_FACTORY_PROPERTY, empty = true),
        @MCRTestProperty(key = LAYOUT_FACTORY_PROPERTY + "Class", empty = true)
    })
    public void rejectsMissingDefaultFactoryConfiguration() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            MCRSAXTransformerFactoryManager::obtainInstance);

        assertTrue(exception.getMessage().contains(LAYOUT_FACTORY_PROPERTY));
    }

    private List<String> collectWarnings(Runnable action) {
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MCRTransformerFactorySelector.class);
        CollectingAppender appender = new CollectingAppender();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return appender.getWarnMessages();
    }

    @Test
    public void cachesXSL2XMLTransformersPerFactoryId() throws Exception {
        String stylesheet = "xsl/reflection.xsl";
        MCRJDOMContent source = new MCRJDOMContent(new Element("root"));

        MCRXSL2XMLTransformer saxon = MCRXSL2XMLTransformer.obtainInstanceByFactory("Saxon", stylesheet);
        MCRXSL2XMLTransformer xalan = MCRXSL2XMLTransformer.obtainInstanceByFactory("Xalan", stylesheet);

        assertEquals("Saxonica", saxon.transform(source).asXML().getRootElement().getAttributeValue("vendor"));
        assertEquals("Apache Software Foundation",
            xalan.transform(source).asXML().getRootElement().getAttributeValue("vendor"));
    }

    @Test
    public void cacheKeySeparatesFactoryAndStylesheetSegments() {
        MCRXSLTransformer.obtainInstanceByFactory("Saxon", "collision_A_B");

        assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTransformer.obtainInstanceByFactory("Saxon_collision_A", "B"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = TRANSFORMER_PREFIX + ".Stylesheet", string = "xsl/reflection.xsl"),
        @MCRTestProperty(
            key = TRANSFORMER_PREFIX + ".TransformerFactoryClass",
            classNameOf = UnregisteredTransformerFactory.class)
    })
    public void supportsUnregisteredLegacyTransformerFactoryClass() throws Exception {
        MCRXSLTransformer transformer = new MCRXSLTransformer();
        List<String> warnings = collectWarnings(() -> transformer.init("Legacy"));

        assertTrue(warnings.stream().anyMatch(message -> message.contains("Register "
            + UnregisteredTransformerFactory.class.getName())));
        assertEquals("Apache Software Foundation", transformer.transform(
            new MCRJDOMContent(new Element("root"))).asXML().getRootElement().getAttributeValue("vendor"));
    }

    @Test
    @SuppressWarnings("removal")
    public void retainsLegacyFlavorMethods() {
        Flavor flavor = new Flavor(UnregisteredTransformerFactory.class, "xsl");

        assertEquals(UnregisteredTransformerFactory.class, flavor.getTransformerFactory());
        flavor.setTransformerFactory(TransformerFactoryImpl.class);
        assertEquals(TransformerFactoryImpl.class, flavor.getTransformerFactory());
    }

    @Test
    @SuppressWarnings("removal")
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = FLAVOR_PREFIX + ".Class", classNameOf = Flavor.class),
        @MCRTestProperty(
            key = FLAVOR_PREFIX + ".TransformerFactory.Class",
            classNameOf = UnregisteredTransformerFactory.class),
        @MCRTestProperty(key = FLAVOR_PREFIX + ".XSLFolder", string = "xsl")
    })
    public void loadsLegacyFlavorFactoryClassProperty() {
        Flavor flavor = MCRConfiguration2.getInstanceOfOrThrow(Flavor.class, FLAVOR_PREFIX);

        assertEquals(UnregisteredTransformerFactory.class, flavor.getTransformerFactory());
        assertEquals("xsl", flavor.getXslFolder());
    }

    @Test
    @SuppressWarnings("removal")
    public void migratesLegacyFlavorClassValue() {
        Flavor flavor = new Flavor();

        List<String> warnings = collectWarnings(
            () -> flavor.setTransformerFactoryId(UnregisteredTransformerFactory.class.getName()));

        assertEquals(UnregisteredTransformerFactory.class, flavor.getTransformerFactory());
        assertTrue(warnings.stream().anyMatch(message -> message.contains("class value")));
    }

    private static final class CollectingAppender extends AbstractAppender {

        private final List<LogEvent> events = new ArrayList<>();

        private CollectingAppender() {
            super(CollectingAppender.class.getSimpleName(), null, null, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        private List<String> getWarnMessages() {
            return events.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(event -> event.getMessage().getFormattedMessage())
                .toList();
        }

    }

    public static final class UnregisteredTransformerFactory extends MCRXalanTransformerFactory {
    }

}
