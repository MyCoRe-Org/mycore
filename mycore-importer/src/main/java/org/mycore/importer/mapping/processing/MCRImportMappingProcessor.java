package org.mycore.importer.mapping.processing;

import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportObject;

/**
 * A mapping processor can be used for pre- and postprocessing manipulations
 * of an import object and a record. 
 * 
 * @author Matthias Eichner
 */
public interface MCRImportMappingProcessor {

    /**
     * Do a preprocessing for the given parameters. The import object has only
     * the datamodel set and the record is unparsed.
     * 
     * @param mcrObject the mcrimport object without any data
     * @param record the uparsed record
     */
    public void preProcessing(MCRImportObject mcrObject, MCRImportRecord record);
    
    /**
     * Do a postprocessing for the given parameters. At this time the import object
     * is succesfully filled with the data from the record.
     * 
     * @param mcrObject the complete mcrimport object
     * @param record the parsed record
     */
    public void postProcessing(MCRImportObject mcrObject, MCRImportRecord record);

}