package org.mycore.mods.identifier;


import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifierInscriber;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRMODSDOIPersistentIdentifierInscriber implements MCRPersistentIdentifierInscriber<MCRDigitalObjectIdentifier> {

    @Override
    public void insertIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase base,String additional) throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "doi", identifier.asString()).orElseThrow(() -> new MCRException("Could not insert doi into mods document!"));
    }

    @Override
    public boolean hasIdentifier(MCRBase base, String additional) throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(base);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement(".//mods:identifier[@type='doi']");
        return element != null;
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if(!(base instanceof MCRObject)){
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }

    @Override
    public void removeIdentifier(MCRDigitalObjectIdentifier identifier, MCRBase obj, String additional) {

    }
}
