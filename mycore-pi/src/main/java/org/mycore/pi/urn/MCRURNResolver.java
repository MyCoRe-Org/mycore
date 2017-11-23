/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.pi.urn;

import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.pi.MCRPersistentIdentifierResolver;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRURNResolver extends MCRPersistentIdentifierResolver<MCRDNBURN> {

    public MCRURNResolver() {
        super("NBN-Resolver");
    }

    @Override
    public Stream<String> resolve(MCRDNBURN identifier) throws MCRIdentifierUnresolvableException {
        Document pidefDocument = MCRDNBPIDefProvider.get(identifier);
        XPathExpression<Element> compile = XPathFactory.instance().compile(
            ".//pidef:resolving_information/pidef:url_info/pidef:url", Filters.element(), null,
            MCRConstants.PIDEF_NAMESPACE);
        return compile.evaluate(pidefDocument).stream().map(Element::getTextTrim);
    }

}
