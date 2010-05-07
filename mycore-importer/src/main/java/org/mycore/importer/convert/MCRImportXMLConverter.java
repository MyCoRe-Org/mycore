package org.mycore.importer.convert;

import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
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
        Iterator<Element> it = toConvert.getDescendants(new ElementFilter());
        while(it.hasNext())
            addFields(record, it.next());
        return record;
    }

    /**
     * This method parses the element and converts text, attributes and namespaces
     * to fields. These fields are added to the record.
     * 
     * @param record where the fields are added
     * @param e element to parse
     */
    private void addFields(MCRImportRecord record, Element e) {
        String base = getElementBase(e);

        // add text
        if(e.getText() != null) {
            StringBuffer textId = new StringBuffer(base);
            textId.delete(textId.length() - 1, textId.length());
            MCRImportField textField = new MCRImportField(textId.toString(), e.getText());
            record.addField(textField);
        }

        // add attributes
        for(Attribute a : (List<Attribute>)e.getAttributes()) {
            StringBuffer attrId = new StringBuffer(base);
            attrId.append("@");
            String nsPrefix = a.getNamespacePrefix();
            if(nsPrefix != null && !nsPrefix.equals(""))
                attrId.append(nsPrefix).append(":");
            attrId.append(a.getName());
            MCRImportField attrField = new MCRImportField(attrId.toString(), a.getValue());
            record.addField(attrField);
        }

        // add namespaces
        addNamespace(record, base, e.getNamespace());       
        for(Namespace ns : (List<Namespace>)e.getAdditionalNamespaces())
            addNamespace(record, base, ns);
    }

    /**
     * Adds a namespace field to the record.
     * 
     * @param record where to add the namespace field
     * @param base
     * @param ns namespace to add as field
     */
    private void addNamespace(MCRImportRecord record, String base, Namespace ns) {
        if(ns.getURI() == null || ns.getURI().equals(""))
            return;
        StringBuffer nsId = new StringBuffer(base);
        nsId.append("@xmlns");
        if(ns.getPrefix() != null && !ns.getPrefix().equals(""))
            nsId.append(":").append(ns.getPrefix());
        MCRImportField nsField = new MCRImportField(nsId.toString(), ns.getURI());
        record.addField(nsField);
    }

    /**
     * <p>Returns a string in dependence of the hierarchy of the element.
     * The elements are separated a slash. E.g.:</p>
     * root/parent1/parent2/element/
     * 
     * @param e the element where the get the hierarchy as string
     * @return
     */
    private String getElementBase(Element e) {
        StringBuffer base = new StringBuffer();
        while(e != null) {
            base.insert(0, "/");
            base.insert(0, e.getQualifiedName());
            e = e.getParentElement();
        }
        return base.toString();
    }
}
