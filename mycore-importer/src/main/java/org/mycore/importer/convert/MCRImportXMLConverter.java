package org.mycore.importer.convert;

import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.MCRImportRecordConverter;

/**
 * <p>This is a sample converter for xml files. The ids of
 * the fields have the following structure:</p>
 * <p>
 * <li><b>Text:</b> root/parent1/parent2/element</li>
 * <li><b>Attribute:</b> root/parent1/parent2/@attribute</li>
 * <li><b>Attribute with namespace:</b> root/parent1/parent2/@namespace:attribute</li>
 * </p>
 * @author Matthias Eichner
 */
public class MCRImportXMLConverter implements MCRImportRecordConverter<Document> {

    private static final String SEPARATOR = "/";

    private String name;

    /**
     * Creates a new instance of <code>MCRImportXMLConverter</code>.
     * 
     * @param name name of the converted records.
     */
    public MCRImportXMLConverter(String name) {
        this.name = name;
    }

    /**
     * Converts a xml document to a <code>MCRImportRecord</code>
     */
    @Override
    public MCRImportRecord convert(Document toConvert) {
        MCRImportRecord record = new MCRImportRecord(this.name);
        MCRImportField rootField = convertElement(null, toConvert.getRootElement());
        record.addField(rootField);
        return record;
    }

    /**
     * This method parses the element and converts text, attributes and namespaces
     * to fields. These fields are added to the record.
     * 
     * @param record where the fields are added
     * @param e element to parse
     */
    @SuppressWarnings("unchecked")
    private MCRImportField convertElement(MCRImportField parentField, Element e) {
        MCRImportField field = new MCRImportField(e.getQualifiedName(), null, SEPARATOR);

        // add text
        if(e.getText() != null && !e.getText().equals(""))
            field.setValue(e.getText());

        // add attributes as subfields
        for(Attribute a : (List<Attribute>)e.getAttributes()) {
            StringBuilder attrId = new StringBuilder("@");
            String nsPrefix = a.getNamespacePrefix();
            if(nsPrefix != null && !nsPrefix.equals(""))
                attrId.append(nsPrefix).append(":");
            attrId.append(a.getName());
            MCRImportField attrField = new MCRImportField(attrId.toString(), a.getValue());
            field.addField(attrField);
        }

        // add namespaces
        addNamespace(field, e.getNamespace());       
        for(Namespace ns : (List<Namespace>)e.getAdditionalNamespaces())
            addNamespace(field, ns);

        // go recursive through all children
        for(Element childElement : (List<Element>)e.getChildren())
            convertElement(field, childElement);

        if(parentField != null && !field.isEmpty())
            parentField.addField(field);

        return field;
    }

    /**
     * Adds a namespace field to the baseField.
     * 
     * @param baseField where to add
     * @param ns namespace to add as field
     */
    private void addNamespace(MCRImportField baseField, Namespace ns) {
        if(ns.getURI() == null || ns.getURI().equals(""))
            return;
        StringBuilder nsId = new StringBuilder("@xmlns");
        if(ns.getPrefix() != null && !ns.getPrefix().equals(""))
            nsId.append(":").append(ns.getPrefix());
        MCRImportField nsField = new MCRImportField(nsId.toString(), ns.getURI());
        baseField.addField(nsField);
    }

}
