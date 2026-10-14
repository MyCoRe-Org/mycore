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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xsl.MCRXSLTProcessorRegistry;
import org.mycore.common.xsl.MCRXalanTransformerFactory;
import org.mycore.common.xsl.MCRXSLTProcessorSelector;
import org.mycore.common.xsl.uriresolver.MCRXSLStyleURIResolver.Flavor;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRXSLTransformerTest {

    private static final String TRANSFORMER_PREFIX = "MCR.ContentTransformer.Legacy";

    private static final String LAYOUT_PROCESSOR_PROPERTY = "MCR.LayoutService.XSLTProcessor";

    private static final String LEGACY_LAYOUT_PROCESSOR_PROPERTY = "MCR.LayoutService.TransformerFactoryClass";

    private static final String FO_PROCESSOR_PROPERTY = "MCR.LayoutService.FoFormatter.XSLTProcessor";

    private static final String LEGACY_FO_PROCESSOR_PROPERTY = "MCR.LayoutService.FoFormatter.transformerFactoryImpl";

    private static final String FLAVOR_PREFIX = "MCR.Test.LegacyFlavor";

    @BeforeEach
    public void resetLegacyWarnings() {
        MCRXSLTProcessorSelector.resetLegacyWarnings();
    }

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
            + ".XSLTProcessor=slowXalan'."), warnings);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROCESSOR_PROPERTY, empty = true),
        @MCRTestProperty(key = LEGACY_LAYOUT_PROCESSOR_PROPERTY, classNameOf = TransformerFactoryImpl.class),
        @MCRTestProperty(key = LEGACY_FO_PROCESSOR_PROPERTY, classNameOf = TransformerFactoryImpl.class)
    })
    public void supportsLegacyLayoutAndFoFactoryProperties() {
        List<String> warnings = collectWarnings(() -> {
            String defaultProcessorId = MCRXSLTProcessorSelector.getDefaultProcessorId();
            assertEquals("slowXalan", defaultProcessorId);
            assertEquals("slowXalan", MCRXSLTProcessorSelector.getProcessorId(FO_PROCESSOR_PROPERTY,
                LEGACY_FO_PROCESSOR_PROPERTY, defaultProcessorId));
        });

        assertEquals(List.of(
            "Configuration property 'MCR.LayoutService.TransformerFactoryClass' is deprecated. Replace it with "
                + "'MCR.LayoutService.XSLTProcessor=slowXalan'.",
            "Configuration property 'MCR.LayoutService.FoFormatter.transformerFactoryImpl' is deprecated. Replace "
                + "it with 'MCR.LayoutService.FoFormatter.XSLTProcessor=slowXalan'."),
            warnings);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROCESSOR_PROPERTY, string = "xalan")
    })
    public void resolvesDefaultProcessorIdAtCallTime() {
        MCRXSLTProcessorRegistry registry = MCRXSLTProcessorRegistry.obtainInstance();
        assertEquals("xalan", MCRXSLTProcessorSelector.getDefaultProcessorId());
        assertSame(registry.getProcessor("xalan"), registry.getDefaultProcessor());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = LAYOUT_PROCESSOR_PROPERTY, empty = true),
        @MCRTestProperty(key = LEGACY_LAYOUT_PROCESSOR_PROPERTY, empty = true)
    })
    public void rejectsMissingDefaultProcessorConfiguration() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTProcessorRegistry.obtainInstance().getDefaultProcessor());

        assertTrue(exception.getMessage().contains(LAYOUT_PROCESSOR_PROPERTY));
    }

    private List<String> collectWarnings(Runnable action) {
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(MCRXSLTProcessorSelector.class);
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
    public void cachesXSL2XMLTransformersPerProcessorId() throws Exception {
        String stylesheet = "xsl/reflection.xsl";
        MCRJDOMContent source = new MCRJDOMContent(new Element("root"));

        MCRXSL2XMLTransformer saxon = MCRXSL2XMLTransformer.obtainInstanceByProcessor("saxon", stylesheet);
        MCRXSL2XMLTransformer xalan = MCRXSL2XMLTransformer.obtainInstanceByProcessor("xalan", stylesheet);

        assertEquals("Saxonica", saxon.transform(source).asXML().getRootElement().getAttributeValue("vendor"));
        assertEquals("Apache Software Foundation",
            xalan.transform(source).asXML().getRootElement().getAttributeValue("vendor"));
    }

    @Test
    public void cacheKeySeparatesProcessorAndStylesheetSegments() {
        MCRXSLTransformer.obtainInstanceByProcessor("saxon", "collision_A_B");

        assertThrows(MCRConfigurationException.class,
            () -> MCRXSLTransformer.obtainInstanceByProcessor("Saxon_collision_A", "B"));
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
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = FLAVOR_PREFIX + ".Class", classNameOf = Flavor.class),
        @MCRTestProperty(key = FLAVOR_PREFIX + ".XSLTProcessor", string = "saxon"),
        @MCRTestProperty(
            key = FLAVOR_PREFIX + ".TransformerFactory.Class",
            classNameOf = MCRXalanTransformerFactory.class),
        @MCRTestProperty(key = FLAVOR_PREFIX + ".XSLFolder", string = "xsl")
    })
    public void legacyFlavorFactoryClassPropertyOverridesInheritedProcessorId() {
        Flavor flavor = MCRConfiguration2.getInstanceOfOrThrow(Flavor.class, FLAVOR_PREFIX);

        assertEquals("xalan", flavor.getXSLTProcessorId());
    }

    @Test
    @SuppressWarnings("removal")
    public void migratesLegacyFlavorClassValue() {
        Flavor flavor = new Flavor();

        List<String> warnings = collectWarnings(
            () -> flavor.setXSLTProcessorId(UnregisteredTransformerFactory.class.getName()));

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
