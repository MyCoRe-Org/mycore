package org.mycore.importer.mapping.mapper;

import org.jdom.Element;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;

/** 
 * A mapper is responsible for the linkage between the input record and the mapping files.
 * The result of this process is stored in an xml object abstraction.
 * 
 * @author Matthias Eichner
 */
public interface MCRImportMapper {

    public void map(MCRImportObject importObject, MCRImportRecord record, Element map);
    public String getType();

}