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
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURNParser;
import org.mycore.pi.urn.MCRUniformResourceName;

public class MCRMODSURNPersistentIdentifierMetadataManager
    extends MCRPersistentIdentifierMetadataManager<MCRUniformResourceName> {

    private static final String MODS_IDENTIFIER_TYPE_URN = "mods:identifier[@type='urn']";

    public MCRMODSURNPersistentIdentifierMetadataManager(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRUniformResourceName identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "urn", identifier.asString())
            .orElseThrow(() -> new MCRException("Could not insert urn into mods document!"));
    }

    @Override
    public void removeIdentifier(MCRUniformResourceName identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    public Optional<MCRPersistentIdentifier> getIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement(MODS_IDENTIFIER_TYPE_URN);

        if (element == null) {
            return Optional.empty();
        }

        String urnText = element.getTextNormalize();
        return new MCRDNBURNParser()
            .parse(urnText)
            .filter(Objects::nonNull)
            .map(MCRPersistentIdentifier.class::cast);
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if (!(base instanceof MCRObject)) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }
}
