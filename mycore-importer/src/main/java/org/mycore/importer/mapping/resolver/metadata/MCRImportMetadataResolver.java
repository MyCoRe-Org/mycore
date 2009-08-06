package org.mycore.importer.mapping.resolver.metadata;

import java.util.List;

import org.jdom.Element;
import org.mycore.importer.MCRImportField;

/**
 * A metadata resolver is responsible for the mapping of a field list
 * to create a metadata element.
 * 
 * @author Matthias Eichner
 */
public interface MCRImportMetadataResolver {

    /**
     * This method resolves a list of <code>MCRImportField</code>s to
     * create a metadata element. For example something like:
     * &lt;note xml:lang="de" form="plain"&gt;Test note&lt;note/&gt;
     * 
     * @param map describes the mapping
     * @param fieldList a list of <code>MCRImportField</code>s
     * @param saveToElement the element where to resolve
     * @return true if the resolving was successfull and the saveToElement is valid
     */
    public boolean resolve(Element map, List<MCRImportField> fieldList, Element saveToElement);

}