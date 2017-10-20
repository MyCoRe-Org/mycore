package org.mycore.mets.model;

import java.io.IOException;

import org.mycore.common.MCRException;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Base interface to create a mets.xml.
 */
public interface MCRMETSGenerator {

    /**
     * Creates a new METS pojo.
     *
     * @return the newly generated mets
     * @throws MCRException unable to generate the mets
     */
    Mets generate() throws MCRException;

}
