package org.mycore.pi.urn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRURNUtils {

    public static Optional<Date> getDNBRegisterDate(MCRPIRegistrationInfo dnburn) {
        try {
            return Optional.of(getDNBRegisterDate(dnburn.getIdentifier()));
        } catch (MCRIdentifierUnresolvableException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static Date getDNBRegisterDate(MCRDNBURN dnburn) throws MCRIdentifierUnresolvableException, ParseException {
        return getDNBRegisterDate(dnburn.asString());
    }

    public static Date getDNBRegisterDate(String identifier) throws MCRIdentifierUnresolvableException,
        ParseException {
        Document document = MCRDNBPIDefProvider.get(identifier);
        XPathExpression<Element> xp = XPathFactory.instance().compile(
            ".//pidef:created[contains(../pidef:identifier, '" + identifier
                + "')]",
            Filters.element(), null, MCRConstants.PIDEF_NAMESPACE);
        Element element = xp.evaluateFirst(document);
        if (element == null) {
            return null;
        }

        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.GERMAN).parse(element.getText());
    }

}
