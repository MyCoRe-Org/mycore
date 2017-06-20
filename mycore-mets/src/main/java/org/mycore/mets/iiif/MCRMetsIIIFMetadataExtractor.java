package org.mycore.mets.iiif;

import java.util.List;

import org.jdom2.Element;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;

public interface MCRMetsIIIFMetadataExtractor {
    List<MCRIIIFMetadata> extractModsMetadata(Element modsElement);
}
