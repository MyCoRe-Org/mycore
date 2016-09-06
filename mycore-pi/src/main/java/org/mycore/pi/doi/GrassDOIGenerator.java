package org.mycore.mir;


import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifierGenerator;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class GrassDOIGenerator implements MCRPersistentIdentifierGenerator<MCRDigitalObjectIdentifier> {

    private static final String OPTIONAL_PREFIX = "DBNummerNeu: ";
    private static final String GENRE_URI = "http://webdatenbank.grass-medienarchiv.de/classifications/mir_genres";
    private String DOI_PREFIX = MCRConfiguration.instance().getString("MCR.DOI.Prefix");

    public GrassDOIGenerator() {
    }

    @Override
    public MCRDigitalObjectIdentifier generate(MCRObjectID mcrID, String additional) throws MCRPersistentIdentifierException {
        MCRMODSWrapper mw = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(mcrID));

        String dbNumberString = mw.getElement("mods:identifier[@type='intern']").getText();

        // dbNumberString maybe prefixed with OPTIONAL_PREFIX
        if (dbNumberString.startsWith(OPTIONAL_PREFIX)) {
            dbNumberString = dbNumberString.substring(OPTIONAL_PREFIX.length());
        }

        // dbNumberString should have 4 digits (if not then it need to be filled with 0)
        if (dbNumberString.length() < 4) {
            while (4 - dbNumberString.length() > 0) {
                dbNumberString = "0" + dbNumberString;
            }
        }

        Element genreElement = mw.getElement("mods:genre[@type='intern' and @authorityURI='" + GENRE_URI + "']");
        String valueURI = genreElement.getAttributeValue("valueURI");
        String genre = valueURI.substring(GENRE_URI.length() + 1);


        String doi = DOI_PREFIX + "/db" + genre.substring(0, 3) + "-" + dbNumberString;

        return (MCRDigitalObjectIdentifier) new MCRDOIParser().parse(doi).orElseThrow(()->new MCRPersistentIdentifierException("Could not generate doi from value " + doi));
    }
}
