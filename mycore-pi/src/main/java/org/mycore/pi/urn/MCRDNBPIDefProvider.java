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

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.util.MCRHttpUtils;

public class MCRDNBPIDefProvider {
    private static final String RESOLVING_URL_TEMPLATE
        = "https://nbn-resolving.org/resolver?identifier={urn}&verb=full&xml=on";

    public static Document get(MCRDNBURN urn) throws MCRIdentifierUnresolvableException {
        return get(urn.asString());
    }

    public static Document get(String identifier) throws MCRIdentifierUnresolvableException {
        HttpGet get = new HttpGet(RESOLVING_URL_TEMPLATE.replaceAll("\\{urn\\}", identifier));
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient().build()) {
            MCRContent response = httpClient.execute(get, MCRHttpUtils.getMCRContentResponseHandler(get.getUri()));
            return MCRXMLParserFactory.getNonValidatingParser().parseXML(response);
        } catch (IOException | JDOMException | URISyntaxException e) {
            String message = "The identifier " + identifier + " is not resolvable!";
            throw new MCRIdentifierUnresolvableException(identifier, message, e);
        }
    }
}
