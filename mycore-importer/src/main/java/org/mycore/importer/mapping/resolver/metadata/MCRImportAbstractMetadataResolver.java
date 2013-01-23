package org.mycore.importer.mapping.resolver.metadata;

import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.mycore.common.MCRConstants;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.condition.MCRImportParser;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;
import org.mycore.parsers.bool.MCRCondition;

/**
 * The abstract metadate resolver is the default implementation of a metadata resolver.
 * It resolves text, attributes and children, furthermore a validation of the final
 * metadata element is possible.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRImportAbstractMetadataResolver implements MCRImportMetadataResolver {

    private static final Logger LOGGER = Logger.getLogger(MCRImportAbstractMetadataResolver.class);

    protected Element map;
    protected MCRImportFieldValueResolver fieldResolver;

    protected Hashtable<String, String> enclosingAttributes;

    protected MCRImportParser conditionParser;

    protected Hashtable<String, MCRCondition> conditionMap;

    /**
     * The return element.
     */
    protected Element saveToElement;

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver#resolve(org.jdom2.Element, java.util.List)
     */
    public boolean resolve(Element map, List<MCRImportField> fieldList, Element saveToElement) {
        this.map = map;
        this.fieldResolver = new MCRImportFieldValueResolver(fieldList);
        this.saveToElement = saveToElement;
        this.enclosingAttributes = new Hashtable<String, String>();
        this.conditionParser = new MCRImportParser();
        this.conditionMap = new Hashtable<String, MCRCondition>();
        // resolve conditions
        resolveConditions(map);
        // check default condition
        if(this.conditionMap.containsKey("_default"))
            if(!checkCondition("_default"))
                return false;
        // enclosing attributes
        resolveEnclosingAttributes(map);
        // attributes
        if(hasAttributes())
            resolveAttributes(map, saveToElement);
        // children
        if(hasChildren())
            resolveChildren(map, saveToElement);
        // and additional stuff
        resolveAdditional();
        // text
        if(hasText())
            resolveMainText();
        return isValid();
    }

    /**
     * Do additional resolving to manipulate the metadata element.
     */
    protected void resolveAdditional() {}

    /**
     * Checks if the metadata element is valid. If the element
     * is not valid, the resolve method will be null return.
     * 
     * @return true if the metadata element is valid, otherwise false
     */
    protected abstract boolean isValid();

    /**
     * Checks if the metadata element can have text.
     * By default this is true.
     * 
     * @return true if the metadata element can have text.
     */
    protected boolean hasText() {
        return true;
    }
    
    /**
     * Checks if the metadata element can have attributes.
     * By default this is true.
     * 
     * @return true if the metadata element can have attributes.
     */
    protected boolean hasAttributes() {
        return true;
    }

    /**
     * Checks if the metadata element can have children.
     * By default this is false.
     * 
     * @return true if the metadata element can have children.
     */
    protected boolean hasChildren() {
        return false;
    }

    /**
     * Returns all attributes from the surrounding metadata element.
     * 
     * @return a hash table of all attributes
     */
    public Hashtable<String, String> getEnclosingAttributes() {
        return this.enclosingAttributes;
    }

    /**
     * Returns all conditions.
     * 
     * @return
     */
    public Hashtable<String, MCRCondition> getConditions() {
        return this.conditionMap;
    }

    /**
     * This method parses the conditions element and resolves all
     * containing conditions. The conditions are added to conditionMap.
     */
    @SuppressWarnings("unchecked")
    protected void resolveConditions(Element parentElement) {
        Element conditionsElement = parentElement.getChild("conditions");
        if(conditionsElement == null)
            return;
        for(Element conditionElement : (List<Element>)conditionsElement.getChildren("condition")) {
            // get condition name
            String name = conditionElement.getAttributeValue("name");
            if(name == null)
                name = "_default";
            // get format
            String format = conditionElement.getAttributeValue("format");
            if(format.equals("xml")) {
                if(conditionElement.getChildren().size() <= 0)
                    continue;
                Element conditionContent = (Element)conditionElement.getChildren().get(0);
                MCRCondition condition = conditionParser.parse(conditionContent);
                this.conditionMap.put(name, condition);
            } else {
                LOGGER.warn("Unknown format '"+ format + "' in condition element. " +
                            "<condition format=\"xml\"> instead.");
            }
        }
    }

    protected boolean checkCondition(String conditionName) {
        MCRCondition cond = this.conditionMap.get(conditionName);
        if(cond == null) {
            LOGGER.warn("Unknown condition '" + conditionName + "'!");
            return false;
        }
        return cond.evaluate(this.fieldResolver);
    }

    protected boolean checkCondition(Element elementToCheck) {
        String conditionName = elementToCheck.getAttributeValue("condition");
        if(conditionName != null)
            return checkCondition(conditionName);
        return true;
    }

    /**
     * Resolves the attributes of the enclosing metadata element. That could
     * be something like:
     * <p>
     * &lt;names class="MCRMetaXML" <b>form="plain"</b>&gt;
     * ...
     * &lt;/names&gt;
     * </p>
     * To define a parent attribute in your mapping file use the 'enclosingAttributes'-
     * and the 'attribute'-element. For example:
     * <p>
     * &lt;map fields="vorname,nachname" to="names"&gt;</br>
     * <b>&nbsp;&nbsp;&lt;enclosingAttributes&gt</br>
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;attribute name="form" value="plain" /&gt;</br>
     * &nbsp;&nbsp;&lt;/enclosingAttributes&gt</b></br>
     * &nbsp;&nbsp;&lt;text value="{nachname}, {vorname}" /&gt;</br>
     * &lt;/map&gt;
     * </p>
     * 
     * @param fromElement the source element where the attribute mapping informations are set
     */
    @SuppressWarnings("unchecked")
    protected void resolveEnclosingAttributes(Element fromElement) {
        // get the enclosing attributes element
        Element enclosingAttributesElement = fromElement.getChild("enclosingAttributes");
        if(enclosingAttributesElement == null) {
            // try old parent
            enclosingAttributesElement = fromElement.getChild("parentAttributes");
            if(enclosingAttributesElement == null)
                return;
            else
                LOGGER.warn("The use of parentAttributes is deprecated. Use enclosingAttributes instead.");
        }
        // check condition
        if(!checkCondition(enclosingAttributesElement))
            return;
        // set enclosing attributes
        List<Element> attributes = enclosingAttributesElement.getChildren("attribute");
        for(Element attributeElement : attributes) {
            Attribute attr = resolveAttribute(attributeElement);
            if(attr != null)
                enclosingAttributes.put(attr.getName(), attr.getValue());
        }
    }

    /**
     * Pass through the attributes of the of the fromElement and tries
     * to resolve them. The results are saved in the saveToElement.
     * 
     * @param fromElement the source element where the attribute mapping informations are set
     * @param saveToElement where to save the resolved attributes
     */
    @SuppressWarnings("unchecked")
    protected void resolveAttributes(Element fromElement, Element saveToElement) {
        Element attributesElement = fromElement.getChild("attributes");
        if(attributesElement == null)
            return;
        List<Element> attributes = attributesElement.getChildren("attribute");

        for(Element attributeElement : attributes) {
            Attribute attr = resolveAttribute(attributeElement);
            if(attr != null)
                saveToElement.setAttribute(attr);
        }
    }

    protected Attribute resolveAttribute(Element attributeElement) {
        // check condition
        if(!checkCondition(attributeElement))
            return null;
        String name = attributeElement.getAttributeValue("name");
        String value = attributeElement.getAttributeValue("value");
        String namespace = attributeElement.getAttributeValue("namespace");
        String uri = attributeElement.getAttributeValue("resolver");

        // resolve fields
        String resolvedName = fieldResolver.resolveFields(name);
        String resolvedValue = resolveValue(value, uri);

        if(resolvedValue == null || resolvedValue.equals(""))
            return null;
        // namespace
        if(namespace == null || namespace.equals(""))
            return new Attribute(resolvedName, resolvedValue);
        else {
            Namespace ns = getNamespace(namespace);
            return new Attribute(resolvedName, resolvedValue, ns);
        }
    }

    /**
     * Parses the incoming namespace string to return a valid
     * jdom namespace. The syntax is "['xml'] | [prefix,url]".
     * 
     * @param namespace the namespace string which have to be parsed
     * @return a jdom namespace
     */
    protected Namespace getNamespace(String namespace) {
        if(namespace.equals("xml"))
            return Namespace.XML_NAMESPACE;
        else if(namespace.equals("xlink"))
            return MCRConstants.XLINK_NAMESPACE;
        else if(namespace.equals("xsi"))
            return MCRConstants.XSI_NAMESPACE;
        else if(namespace.equals("xsl"))
            return MCRConstants.XSL_NAMESPACE;
        else if(namespace.equals("dv"))
            return MCRConstants.DV_NAMESPACE;

        String split[] = namespace.split(",");
        return Namespace.getNamespace(split[0], split[1]);
    }

    /**
     * This method does two things to resolve a value. At first it
     * parses the incoming string with the field resolver. So all
     * variables will be resolved. After that the result string 
     * will be resolved by the given uri resolver.
     * 
     * @param oldValue the incoming string
     * @param uri
     * @return a new parsed and resolved string
     */
    protected String resolveValue(String oldValue, String uri) {
        String resolvedValue = fieldResolver.resolveFields(oldValue);

        // is a resolver defined?
        if(uri != null && !uri.equals("")) {
            // maybe in the uri are some field values -> do a field resolve
            String resolvedUri = fieldResolver.resolveFields(uri);
            // try to resolve the uri to get the new value
            resolvedValue = MCRImportMappingManager.getInstance().getURIResolverManager().resolveURI(resolvedUri, resolvedValue);
        }
        return resolvedValue;
    }

    /**
     * This method tries to resolve the text for the metadataChild element.
     * If no child with the text tag exists, the text will be build from
     * the values of all not used fields.
     */
    protected void resolveMainText() {
        Element textElement = map.getChild("text");
        // no text element defined -> every field which is not used
        // will added to the text.
        if(textElement == null) {
            StringBuilder textBuffer = new StringBuilder();
            // go through the not used list
            for(MCRImportField field : fieldResolver.getNotUsedFields()) {
                // add every field to the text
                textBuffer.append("[{");
                textBuffer.append(field.getId());
                textBuffer.append("}]");
            }
            if(textBuffer.length() > 0) {
                // do text resolving
                String text = resolveValue(textBuffer.toString(), null);
                // add text to element
                saveToElement.addContent(new Text(text));
            }
            return;
        }
        // resolve by the text tag
        resolveText(map, saveToElement);
    }

    /**
     * Resolves the text tag. For example:</br>
     * &lt;text value="{field1}" resolver="mapping:genderMapping" /&gt;
     * 
     * @param fromElement
     * @param saveToElement
     */
    protected void resolveText(Element fromElement, Element saveToElement) {
        Element textElement = fromElement.getChild("text");
        // check condition
        if(!checkCondition(textElement))
            return;
        // parse the text element
        String value = textElement.getAttributeValue("value");
        String uri = textElement.getAttributeValue("resolver");
        // resolve value
        String resolvedValue = resolveValue(value, uri);
        if(resolvedValue != null && !resolvedValue.equals("")) {
            Text text = new Text(resolvedValue);
            saveToElement.addContent(text);
        }
    }

    /**
     * Pass through all children of the element, creates for each
     * child a new metadata element with attributes and text. If
     * the child again has children this method is called recursive.
     * 
     * @param fromElement the element which is parsed
     * @param saveToElement the element where the new created children are saved
     */
    @SuppressWarnings("unchecked")
    protected void resolveChildren(Element fromElement, Element saveToElement) {
        Element childsElement = fromElement.getChild("children");
        if(childsElement == null)
            return;

        List<Element> children = childsElement.getChildren("child");
        for(Element mapChild : children) {
            if(!checkCondition(mapChild))
                return;
            // create a new child element for each child in the list
            String tag = mapChild.getAttributeValue("tag");
            Element metadataChild = new Element(tag);

            // resolve attributes and text
            resolveAttributes(mapChild, metadataChild);
            resolveText(mapChild, metadataChild);
            // resolve recursive further childs of the current one
            resolveChildren(mapChild, metadataChild);
            // add only childs with content
            if(isValidChild(metadataChild))
                saveToElement.addContent(metadataChild);
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean isValidChild(Element childElement) {
        if(childElement.getText() != null && !childElement.getText().equals(""))
            return true;
        boolean valid = false;
        List<Element> childList= (List<Element>)childElement.getContent(Filters.element());
        for (Element aChildList : childList) {
            if (isValidChild(aChildList))
                valid = true;
            else
                childElement.removeContent(aChildList);
        }
        return valid;
    }
}