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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;

public class MCRDNBPIDefProvider {
    private static final String RESOLVING_URL_TEMPLATE = "https://nbn-resolving.org/resolver?identifier={urn}&verb=full&xml=on";

    public static Document get(MCRDNBURN urn) throws MCRIdentifierUnresolvableException {
        return get(urn.asString());
    }

    public static Document get(String identifier) throws MCRIdentifierUnresolvableException {
        HttpGet get = new HttpGet(RESOLVING_URL_TEMPLATE.replaceAll("\\{urn\\}", identifier));
        try (CloseableHttpClient httpClient = MCRHttpUtils.getHttpClient()) {
            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();
            return new SAXBuilder().build(entity.getContent());
        } catch (IOException | JDOMException e) {
            String message = "The identifier " + identifier + " is not resolvable!";
            throw new MCRIdentifierUnresolvableException(identifier, message, e);
        }
    }
}
