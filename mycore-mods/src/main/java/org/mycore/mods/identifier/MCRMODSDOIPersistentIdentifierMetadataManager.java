package org.mycore.mods.identifier;

import java.util.Objects;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierMetadataManager;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMODSDOIPersistentIdentifierMetadataManager
    extends MCRPersistentIdentifierMetadataManager<MCRDigitalObjectIdentifier> {

    public MCRMODSDOIPersistentIdentifierMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "doi", identifier.asString())
            .orElseThrow(() -> new MCRException("Could not insert doi into mods document!"));
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if (!(base instanceof MCRObject)) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }

    @Override
    public void removeIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase base, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement("mods:identifier[@type='doi']");
        if (element == null) {
            return Optional.empty();
        }

        String doiText = element.getTextNormalize();

        return new MCRDOIParser()
            .parse(doiText)
            .filter(Objects::nonNull)
            .map(MCRPersistentIdentifier.class::cast);
    }
}
