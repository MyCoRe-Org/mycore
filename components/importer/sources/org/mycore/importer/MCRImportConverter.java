package org.mycore.importer;

import java.util.List;


public interface MCRImportConverter<T> {

    public List<MCRImportRecord> convert(T toConvert);

}