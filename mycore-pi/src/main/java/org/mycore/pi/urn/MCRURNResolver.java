package org.mycore.pi.urn;


import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.MCRPersistentIdentifierResolver;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRURNResolver extends MCRPersistentIdentifierResolver<MCRDNBURN> {


    public MCRURNResolver() {
        super("NBN-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRPersistentIdentifier identifier) throws MCRIdentifierUnresolvableException {
        if(identifier instanceof MCRDNBURN){
            Document pidefDocument = MCRDNBPIDefProvider.get((MCRDNBURN) identifier);
            XPathExpression<Element> compile = XPathFactory.instance().compile(".//pidef:resolving_information/pidef:url_info/pidef:url", Filters.element(), null, MCRConstants.PIDEF_NAMESPACE);
            return compile.evaluate(pidefDocument).stream().map(Element::getTextTrim);
        } else {
            return Stream.empty();
        }

    }


}
