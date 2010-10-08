package org.mycore.migration20_21.cli;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

;

/**
 * This command converts a datamodel 1 file to a datamodel 2 one.
 * The source file wont be overwritten. A new file with the addition
 * "-dm2" will be created.
 * 
 * @author Matthias Eichner
 */
public class MCRDatamodelToDatamodel2Command {

    private static HashMap<String, String> typeReferences;

    private static HashMap<String, String> styleReferences;

    private static final Logger LOGGER = Logger.getLogger(MCRDatamodelToDatamodel2Command.class);

    private static boolean carryOverComments = false;

    static {
        typeReferences = new HashMap<String, String>();
        typeReferences.put("MCRMetaLangText", "text");
        typeReferences.put("MCRMetaBoolean", "boolean");
        typeReferences.put("MCRMetaClassification", "classification");
        typeReferences.put("MCRMetaISO8601Date", "date");
        typeReferences.put("MCRMetaLinkID", "link");
        typeReferences.put("MCRMetaLink", "href");
        typeReferences.put("MCRMetaDerivateLink", "derlink");
        typeReferences.put("MCRMetaXML", "xml");
        typeReferences.put("MCRMetaNumber", "number");

        styleReferences = new HashMap<String, String>();
        styleReferences.put("classification", "select");
        styleReferences.put("link", "subselect");
        styleReferences.put("xml", "dontknow");
    }

    public static void convert(String filePath) throws Exception {
        // read datamodel1 file
        SAXBuilder builder = new SAXBuilder();
        Document dm1Doc = builder.build(filePath);

        // create the new datamodel2 file
        Element dm2RootElement = new Element("objecttype");
        try {
            dm2RootElement.addNamespaceDeclaration(XSI_NAMESPACE);
            dm2RootElement.setAttribute("noNamespaceSchemaLocation", "datamodel.xsd", XSI_NAMESPACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // name
        String type = dm1Doc.getRootElement().getAttributeValue("type");
        if (type != null)
            dm2RootElement.setAttribute("name", type);

        // children, parents, derivates
        convertStructure(dm1Doc.getRootElement(), dm2RootElement);
        // metadata
        convertMetadata(dm1Doc.getRootElement(), dm2RootElement);

        // save new datamodel2 file
        String newFile = filePath.replace(".", "-dm2.");
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream output = new FileOutputStream(newFile);
        outputter.output(new Document(dm2RootElement), output);
    }

    private static void convertStructure(Element dm1RE, Element dm2RE) {
        boolean isChild = false;
        boolean isParent = false;
        boolean hasDerivates = false;

        Element structureElement = dm1RE.getChild("structure");
        if (structureElement != null) {
            for (Object o : structureElement.getChildren("element")) {
                if (!(o instanceof Element))
                    continue;
                Element e = (Element) o;
                if (e.getAttributeValue("maxOccurs").equals("1")) {
                    // check if the object is a parent and can have children
                    if (e.getAttributeValue("name").equals("children"))
                        isParent = true;
                    // check if the object is a child
                    else if (e.getAttributeValue("name").equals("parents"))
                        isChild = true;
                    // check if derivates could be added
                    else if (e.getAttributeValue("name").equals("derobjects"))
                        hasDerivates = true;
                }

            }
        }
        // set infos to the new datamodel2 root element
        dm2RE.setAttribute("isChild", Boolean.toString(isChild));
        dm2RE.setAttribute("isParent", Boolean.toString(isParent));
        dm2RE.setAttribute("hasDerivates", Boolean.toString(hasDerivates));
        if (isParent) {
            Element newChildrenElement = new Element("children");
            Comment comment = new Comment("TODO: <child type=\"enter a child type here\" />");
            newChildrenElement.addContent(comment);
            dm2RE.addContent(newChildrenElement);
        }
    }

    private static void convertMetadata(Element dm1RE, Element dm2RE) throws Exception {
        Element dm1MetadataElement = dm1RE.getChild("metadata");
        @SuppressWarnings("unchecked")
        List<Content> contentOfMetadata = dm1MetadataElement.getContent();

        Element dm2MetadataElement = new Element("metadata");
        dm2RE.addContent(dm2MetadataElement);

        for (Content c : contentOfMetadata) {
            // for all elements in metadata 
            if (c instanceof Element && ((Element) c).getName().equals("element")) {
                Element parent = (Element) c;
                if (parent.getChildren().size() != 1)
                    throw new Exception("Not supported child count at element " + parent);

                // get all attributes from datamodel 1 element
                Element inner = (Element) parent.getChildren().get(0);
                String name = inner.getAttributeValue("name");
                // for both minOccurs and maxOccurs default values are 1
                String minOccurs = parent.getAttributeValue("minOccurs");
                if (minOccurs != null && minOccurs.equals("1"))
                    minOccurs = null;
                String maxOccurs = inner.getAttributeValue("maxOccurs");
                if (maxOccurs != null && maxOccurs.equals("1"))
                    maxOccurs = null;
                String classAttr = inner.getAttributeValue("class");
                String type = typeReferences.get(classAttr);
                if (type == null) {
                    type = "unknown-" + classAttr;
                    LOGGER.warn("There is no type defined for class '" + classAttr + "', setting type=" + type);
                }
                String style = styleReferences.get(type);
                String wrapperName = parent.getAttributeValue("name");
                // set enclosing name only if its not default
                if (wrapperName.equals("def." + name))
                    wrapperName = null;

                if (style == null)
                    style = "small";

                // set attributes for datamodel 2 element
                Element dm2Element = new Element("element");
                dm2Element.setAttribute("name", name);
                dm2Element.setAttribute("type", type);
                if (minOccurs != null)
                    dm2Element.setAttribute("minOccurs", minOccurs);
                if (maxOccurs != null)
                    dm2Element.setAttribute("maxOccurs", maxOccurs);
                dm2Element.setAttribute("style", style);
                if (wrapperName != null)
                    dm2Element.setAttribute("wrapper", wrapperName);

                // set notinherit and heritable to ignonre to avoid inconsistency
                dm2Element.setAttribute("notinherit", "ignore");
                dm2Element.setAttribute("heritable", "ignore");

                // specaial case for classifications
                if (type.equals("classification")) {
                    Comment clComment = new Comment("TODO: <classification id=\"enter a classification id here\" />");
                    dm2Element.addContent(clComment);
                }

                // add element to the metadata
                dm2MetadataElement.addContent(dm2Element);
            } else if (c instanceof Comment && carryOverComments) {
                dm2MetadataElement.addContent(new Comment(((Comment) c).getText()));
            }
        }
    }
}
