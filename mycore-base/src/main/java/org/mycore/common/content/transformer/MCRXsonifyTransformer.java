package org.mycore.common.content.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;
import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.xml.MCREntityResolver;
import org.mycore.xsonify.serialize.Json2XmlSerializer;
import org.mycore.xsonify.serialize.SerializationException;
import org.mycore.xsonify.serialize.SerializerSettings;
import org.mycore.xsonify.serialize.SerializerStyle;
import org.mycore.xsonify.serialize.Xml2JsonSerializer;
import org.mycore.xsonify.xml.XmlDocument;
import org.mycore.xsonify.xml.XmlDocumentLoader;
import org.mycore.xsonify.xml.XmlEntityResolverDocumentLoader;
import org.mycore.xsonify.xml.XmlName;
import org.mycore.xsonify.xml.XmlParseException;
import org.mycore.xsonify.xml.XmlSaxParser;
import org.mycore.xsonify.xsd.Xsd;
import org.mycore.xsonify.xsd.XsdParseException;
import org.mycore.xsonify.xsd.XsdParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Locale;

import static org.mycore.xsonify.serialize.SerializerSettings.AdditionalNamespaceDeclarationStrategy;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_ADDITIONAL_NAMESPACE_DECLARATION_STRATEGY;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_ATTRIBUTE_PREFIX_HANDLING;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_ELEMENT_PREFIX_HANDLING;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_JSON_STRUCTURE;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_MIXED_CONTENT_HANDLING;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_NAMESPACE_HANDLING;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_NORMALIZE_TEXT;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_OMIT_ROOT_ELEMENT;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_PLAIN_TEXT_HANDLING;
import static org.mycore.xsonify.serialize.SerializerSettings.DEFAULT_XS_ANY_NAMESPACE_STRATEGY;
import static org.mycore.xsonify.serialize.SerializerSettings.JsonStructure;
import static org.mycore.xsonify.serialize.SerializerSettings.MixedContentHandling;
import static org.mycore.xsonify.serialize.SerializerSettings.NamespaceDeclaration;
import static org.mycore.xsonify.serialize.SerializerSettings.PlainTextHandling;
import static org.mycore.xsonify.serialize.SerializerSettings.PrefixHandling;
import static org.mycore.xsonify.serialize.SerializerSettings.XsAnyNamespaceStrategy;

/**
 * <p>Content Transformer to convert XML to JSON and vice versa.
 * Utilizes XML Schema Definition (XSD) to ensure accurate serialization and deserialization.</p>
 * <p>
 * The MCRXsonifyTransformer is configured by mycore.properties.
 * <pre>
 * {@code
 * MCR.ContentTransformer.mods-json-normal.Class=org.mycore.common.content.transformer.MCRXsonifyTransformer
 * }
 * </pre>
 * The Schema property points to a XSD and is required.
 * <pre>
 * {@code
 * MCR.ContentTransformer.mods-json-normal.Schema=datamodel-mods.xsd
 * }
 * </pre>
 * Other settings and the style of the json can also be defined by mycore.properties. See {@link SerializerSettings}
 * and {@link SerializerStyle} for more information.
 * <pre>
 * {@code
 * # settings
 * MCR.ContentTransformer.mods-json-normal.Settings.Class=org.mycore.common.content.transformer.MCRXsonifyTransformer$SettingsBuilder
 * MCR.ContentTransformer.mods-json-normal.Settings.OmitRootElement=false
 * MCR.ContentTransformer.mods-json-normal.Settings.ElementPrefixHandling=RETAIN_ORIGINAL
 * # style
 * MCR.ContentTransformer.mods-json-normal.Style.Class=org.mycore.common.content.transformer.MCRXsonifyTransformer$StyleBuilder
 * MCR.ContentTransformer.mods-json-normal.Style.TextKey=text
 * }
 * </pre>
 */
@Singleton
public class MCRXsonifyTransformer extends MCRContentTransformer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Path to the XSD in the classpath.
     */
    @MCRProperty(name = "Schema")
    public String schema;

    /**
     * Local Name of the root element. By default, this is <b>mycoreobject</b>. This setting is only required in the
     * json2xml serialisation process and if {@link SerializerSettings#omitRootElement()} is set to <b>true</b>.
     */
    @MCRProperty(name = "RootName", required = false)
    public String rootName = "mycoreobject";

    /**
     * Map of namespaces. This setting is required for the json2xml serialisation process if the
     * {@link SerializerSettings#namespaceDeclaration()} is set to <b>OMIT</b>.
     * TODO: Map property doesn't seem to work right now
     */
    //@MCRProperty(name = "Namespaces", required = false)
    //public Map<String, String> namespaces;

    @MCRInstance(name = "Settings", valueClass = SettingsBuilder.class, required = false)
    public SettingsBuilder settingsBuilder;

    @MCRInstance(name = "Style", valueClass = StyleBuilder.class, required = false)
    public StyleBuilder styleBuilder;

    private Xml2JsonSerializer xml2JsonSerializer;

    private Json2XmlSerializer json2XmlSerializer;

    /**
     * Initializes the transformer. Takes care of setting up the required serializers.
     *
     * @throws ParserConfigurationException if there is a problem with the XML parser configuration.
     * @throws SAXException                 if there is a SAX-related issue.
     */
    @MCRPostConstruction
    public void init() throws ParserConfigurationException, SAXException, SerializationException, XsdParseException {
        XmlSaxParser saxParser = new XmlSaxParser();
        XmlDocumentLoader documentLoader = new XmlEntityResolverDocumentLoader(MCREntityResolver.instance(), saxParser);
        XsdParser xsdParser = new XsdParser(documentLoader);
        Xsd xsd = xsdParser.parse(this.schema);

        SerializerSettings settings = settingsBuilder != null ? settingsBuilder.get() : new SerializerSettings();
        SerializerStyle style = styleBuilder != null ? styleBuilder.get() : new SerializerStyle();

        this.xml2JsonSerializer = new Xml2JsonSerializer(xsd, settings, style);
        this.json2XmlSerializer = new Json2XmlSerializer(xsd, settings, style);

        if (this.rootName != null) {
            // TODO setRootName should set local name -> fix in xsonify API
            this.json2XmlSerializer.setRootName(new XmlName(this.rootName, null));
        }
        /*if (this.namespaces != null && !this.namespaces.isEmpty()) {
            List<XmlNamespace> namespaceList = namespaces.entrySet().stream()
                .map(entry -> new XmlNamespace(entry.getKey(), entry.getValue()))
                .toList();
            this.json2XmlSerializer.setNamespaces(namespaceList);
        }*/
    }

    /**
     * Transforms the given {@code MCRContent} source into XML or JSON, depending on its mime type.
     *
     * @param source The content to be transformed.
     * @return The transformed content.
     * @throws IOException if there's an error in input-output operations.
     */
    @Override
    public MCRContent transform(MCRContent source) throws IOException {
        String mimeType = source.getMimeType().toLowerCase(Locale.ROOT);
        try {
            if (mimeType.endsWith("xml")) {
                return xml2json(source);
            } else if (mimeType.endsWith("json")) {
                return json2xml(source);
            }
        } catch (SerializationException serializerException) {
            throw new MCRException("unable to serialize source.", serializerException);
        }
        throw new MCRException(
            "Unable to transform source because mimeType '" + mimeType + "' is neither xml nor json.");
    }

    /**
     * Converts XML content to JSON.
     *
     * @param source The XML content to be transformed.
     * @return The transformed JSON content.
     */
    protected MCRContent xml2json(MCRContent source) throws SerializationException {
        XmlDocument xml = getXmlDocument(source);
        ObjectNode json = this.xml2JsonSerializer.serialize(xml);
        return asContent(json, source.isUsingSession());
    }

    /**
     * Converts JSON content to XML.
     *
     * @param source The JSON content to be transformed.
     * @return The transformed XML content.
     */
    protected MCRContent json2xml(MCRContent source) throws SerializationException {
        ObjectNode json = getJsonObject(source);
        XmlDocument xml = this.json2XmlSerializer.serialize(json);
        return asContent(xml, source.isUsingSession());
    }

    protected XmlDocument getXmlDocument(MCRContent source) {
        try {
            XmlSaxParser saxParser = new XmlSaxParser();
            return saxParser.parse(source.getInputStream());
        } catch (ParserConfigurationException | SAXException | IOException | XmlParseException e) {
            throw new MCRException("Unable to parse xml source " + source.getName(), e);
        }
    }

    protected ObjectNode getJsonObject(MCRContent source) {
        try {
            JsonNode jsonNode = MAPPER.readTree(source.asString());
            if (jsonNode.isObject()) {
                return (ObjectNode) jsonNode;
            }
            throw new MCRException("json is not a json object '" + jsonNode + "'");
        } catch (IOException ioException) {
            throw new MCRException("Unable to parse json source", ioException);
        }
    }

    protected MCRContent asContent(ObjectNode json, boolean usingSession) {
        MCRContent result = new MCRStringContent(json.toString());
        result.setMimeType("application/json");
        result.setUsingSession(usingSession);
        return result;
    }

    protected MCRContent asContent(XmlDocument xml, boolean usingSession) {
        MCRContent result = new MCRStringContent(xml.toXml(false));
        result.setMimeType("text/xml");
        result.setUsingSession(usingSession);
        return result;
    }

    /**
     * SettingsBuilder is responsible for building settings used for customizing the serialization process. See
     * {@link SerializerSettings} for more information.
     */
    public static class SettingsBuilder {

        @MCRProperty(name = "OmitRootElement", required = false)
        public String omitRootElement = String.valueOf(Boolean.valueOf(DEFAULT_OMIT_ROOT_ELEMENT));

        @MCRProperty(name = "NamespaceDeclaration", required = false)
        public String namespaceDeclaration = DEFAULT_NAMESPACE_HANDLING.name();

        @MCRProperty(name = "NormalizeText", required = false)
        public String normalizeText = String.valueOf(Boolean.valueOf(DEFAULT_NORMALIZE_TEXT));

        @MCRProperty(name = "ElementPrefixHandling", required = false)
        public String elementPrefixHandling = DEFAULT_ELEMENT_PREFIX_HANDLING.name();

        @MCRProperty(name = "AttributePrefixHandling", required = false)
        public String attributePrefixHandling = DEFAULT_ATTRIBUTE_PREFIX_HANDLING.name();

        @MCRProperty(name = "JsonStructure", required = false)
        public String jsonStructure = DEFAULT_JSON_STRUCTURE.name();

        @MCRProperty(name = "PlainTextHandling", required = false)
        public String plainTextHandling = DEFAULT_PLAIN_TEXT_HANDLING.name();

        @MCRProperty(name = "MixedContentHandling", required = false)
        public String mixedContentHandling = DEFAULT_MIXED_CONTENT_HANDLING.name();

        @MCRProperty(name = "AdditionalNamespaceDeclarationStrategy", required = false)
        public String additionalNamespaceDeclarationStrategy = DEFAULT_ADDITIONAL_NAMESPACE_DECLARATION_STRATEGY.name();

        @MCRProperty(name = "XsAnyNamespaceStrategy", required = false)
        public String xsAnyNamespaceStrategy = DEFAULT_XS_ANY_NAMESPACE_STRATEGY.name();

        public SerializerSettings get() {
            return new SerializerSettings(
                Boolean.parseBoolean(omitRootElement),
                NamespaceDeclaration.valueOf(namespaceDeclaration),
                Boolean.parseBoolean(normalizeText),
                PrefixHandling.valueOf(elementPrefixHandling),
                PrefixHandling.valueOf(attributePrefixHandling),
                JsonStructure.valueOf(jsonStructure),
                PlainTextHandling.valueOf(plainTextHandling),
                MixedContentHandling.valueOf(mixedContentHandling),
                AdditionalNamespaceDeclarationStrategy.valueOf(additionalNamespaceDeclarationStrategy),
                XsAnyNamespaceStrategy.valueOf(xsAnyNamespaceStrategy)
            );
        }

    }

    /**
     * StyleBuilder is responsible for building styles used for customizing the serialization process. See
     * {@link SerializerStyle} for more information.
     */
    public static class StyleBuilder {

        @MCRProperty(name = "AttributePrefix", required = false)
        public String attributePrefix = new SerializerStyle().attributePrefix();

        @MCRProperty(name = "XmlnsPrefix", required = false)
        public String xmlnsPrefix = new SerializerStyle().xmlnsPrefix();

        @MCRProperty(name = "TextKey", required = false)
        public String textKey = new SerializerStyle().textKey();

        @MCRProperty(name = "MixedContentKey", required = false)
        public String mixedContentKey = new SerializerStyle().mixedContentKey();

        @MCRProperty(name = "MixedContentElementNameKey", required = false)
        public String mixedContentElementNameKey = new SerializerStyle().mixedContentElementNameKey();

        @MCRProperty(name = "IndexKey", required = false)
        public String indexKey = new SerializerStyle().indexKey();

        public SerializerStyle get() {
            return new SerializerStyle(
                attributePrefix,
                xmlnsPrefix,
                textKey,
                mixedContentKey,
                mixedContentElementNameKey,
                indexKey
            );
        }

    }

}
