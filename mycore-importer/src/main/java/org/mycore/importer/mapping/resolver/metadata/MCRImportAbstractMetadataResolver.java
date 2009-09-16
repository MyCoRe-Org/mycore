package org.mycore.importer.mapping.resolver.metadata;

import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.filter.ElementFilter;
import org.mycore.common.MCRConstants;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.mapping.MCRImportMappingManager;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;

/**
 * The abstract metadate resolver is the default implementation of a metadata resolver.
 * It resolves text, attributes and children, furthermore a validation of the final
 * metadata element is possible.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRImportAbstractMetadataResolver implements MCRImportMetadataResolver {

    protected Element map;
    protected MCRImportFieldValueResolver fieldResolver;

    /**
     * The return element.
     */
    protected Element saveToElement;

    /*
     * (non-Javadoc)
     * @see org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver#resolve(org.jdom.Element, java.util.List)
     */
    public boolean resolve(Element map, List<MCRImportField> fieldList, Element saveToElement) {
        this.map = map;
        this.fieldResolver = new MCRImportFieldValueResolver(fieldList);
        this.saveToElement = saveToElement;
        // attributes
        if(hasAttributes());
            resolveAttributes(map, saveToElement);
        // children
        if(hasChildren())
            resolveChildren(map, saveToElement);
        // and additional stuff
        resolveAdditional();
        // text
        if(hasText())
            resolveMainText();
        if(!isValid())
            return false;
        return true;
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
     * Pass through the attributes of the of the fromElement and tries
     * to resolve them. The results are saved in the saveToElement.
     * 
     * @param fromElement the source element where the attribute mapping informations are set
     * @param saveToElement where to save the resolved attributes
     */
    @SuppressWarnings("unchecked")
    public void resolveAttributes(Element fromElement, Element saveToElement) {
        Element attributesElement = fromElement.getChild("attributes");
        if(attributesElement == null)
            return;
        List<Element> attributes = attributesElement.getChildren("attribute");

        for(Element attributeElement : attributes) {
            String name = attributeElement.getAttributeValue("name");
            String value = attributeElement.getAttributeValue("value");
            String namespace = attributeElement.getAttributeValue("namespace");
            String uri = attributeElement.getAttributeValue("resolver");

            // resolve fields
            String resolvedName = fieldResolver.resolveFields(name);
            String resolvedValue = resolveValue(value, uri);

            if(resolvedValue == null)
                return;

            // namespace
            if(namespace == null || namespace.equals(""))
                saveToElement.setAttribute(resolvedName, resolvedValue);
            else {
                Namespace ns = getNamespace(namespace);
                saveToElement.setAttribute(resolvedName, resolvedValue, ns);
            }
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
    public String resolveValue(String oldValue, String uri) {
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
            StringBuffer textBuffer = new StringBuffer();
            // go through the not used list
            for(MCRImportField field : fieldResolver.getNotUsedFields()) {
                // add every field value to the text
                textBuffer.append(field.getValue());
            }
            if(textBuffer.length() > 0) {
                Text text = new Text(textBuffer.toString());
                saveToElement.addContent(text);
            }
            return;
        }
        // resolve by the text tag
        resolveText(map, saveToElement);
    }

    /**
     * Resolves the text tag. For example:</br>
     * &lt;text value="{field1}" resolve="mapping:genderMapping" /&gt;
     * 
     * @param fromElement
     * @param saveToElement
     */
    public void resolveText(Element fromElement, Element saveToElement) {
        Element textElement = fromElement.getChild("text");
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
    public void resolveChildren(Element fromElement, Element saveToElement) {
        Element childsElement = fromElement.getChild("children");
        if(childsElement == null)
            return;

        List<Element> children = childsElement.getChildren("child");
        for(Element mapChild : children) {
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
        List<Element> childList= (List<Element>)childElement.getContent(new ElementFilter());
        for(int i = 0; i < childList.size(); i++) {
           if(isValidChild(childList.get(i)))
               valid = true;
           else
               childElement.removeContent(childList.get(i));
        }
        return valid;
    }
}