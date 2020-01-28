package org.mycore.services.staticcontent;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRException;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRStaticContentResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] parts = href.split(":", 3);
        if (parts.length != 3) {
            throw new MCRException("href needs to be staticContent:ContentGeneratorID:ObjectID but was " + href);
        }

        final String contentGeneratorID = parts[1];
        final MCRObjectID objectID = MCRObjectID.getInstance(parts[2]);
        final MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(contentGeneratorID);

        return new MCRLazyStreamSource(() -> generator.get(objectID), href);
    }
}
