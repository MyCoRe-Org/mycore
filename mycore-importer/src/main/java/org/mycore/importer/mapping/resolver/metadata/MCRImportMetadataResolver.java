package org.mycore.importer.mapping.resolver.metadata;

import java.util.List;

import org.jdom.Element;
import org.mycore.importer.MCRImportField;

public interface MCRImportMetadataResolver {

    public Element resolve(Element map, List<MCRImportField> fieldList);

}