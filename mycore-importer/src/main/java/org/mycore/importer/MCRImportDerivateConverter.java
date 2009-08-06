package org.mycore.importer;

import org.mycore.importer.derivate.MCRImportDerivate;

public interface MCRImportDerivateConverter<T> {

    public MCRImportDerivate convert(T toConvert);
    
}
