package org.mycore.importer;

public interface MCRImportRecordConverter<T> {

    public MCRImportRecord convert(T toConvert);

}