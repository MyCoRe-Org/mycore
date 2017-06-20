/**
 * 
 */
package org.mycore.pi.doi;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * Uses mapping from MCRObjectID base to DOI prefix to generate DOIs.
 * e.g. <code>MCR.PI.Generator.MapObjectIDDOI.Prefix.mycore_mods = 10.5072/my.</code> will map
 * <code>mycore_mods_00004711</code> to <code>10.5072/my.4711</code>
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRMapObjectIDDOIGenerator extends MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private final MCRDOIParser mcrdoiParser;

    private String generatorID;

    public MCRMapObjectIDDOIGenerator(String generatorID) {
        super(generatorID);
        mcrdoiParser = new MCRDOIParser();
        this.generatorID = generatorID;
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional)
        throws MCRPersistentIdentifierException {
        String prefixProperty = "MCR.PI.Generator." + generatorID + ".Prefix." + mcrID.getBase();
        return MCRConfiguration2.getString(prefixProperty)
            .map(prefix -> prefix.contains("/") ? prefix + mcrID.getNumberAsInteger()
                : prefix + '/' + mcrID.getNumberAsInteger())
            .flatMap(mcrdoiParser::parse).map(MCRDigitalObjectIdentifier.class::cast)
            .orElseThrow(() -> new MCRPersistentIdentifierException(prefixProperty + " does is not defined."));
    }

}
