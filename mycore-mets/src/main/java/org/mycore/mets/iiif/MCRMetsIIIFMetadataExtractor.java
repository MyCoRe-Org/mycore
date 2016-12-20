package org.mycore.mets.iiif;


import org.jdom2.Element;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;

import java.util.List;

public interface MCRMetsIIIFMetadataExtractor {
    List<MCRIIIFMetadata> extractModsMetadata(Element modsElement);
}
