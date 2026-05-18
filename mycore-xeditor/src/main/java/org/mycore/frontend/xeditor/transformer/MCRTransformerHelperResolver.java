/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend.xeditor.transformer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * {@link URIResolver} that assists in transforming XEditor forms to HTML.
 */
public class MCRTransformerHelperResolver implements URIResolver {

    /**
     * Resolves the given transformer helper call and returns the result as an XML source.
     * <p>The URI is parsed into a {@link MCRTransformerHelperCall}, which is dispatched to the
     * appropriate method handler via {@link MCRTransformationState}.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{method}[:{params}]
     * </pre>
     *
     * @param href the URI to resolve; parsed as a {@link MCRTransformerHelperCall}
     * @param base the base URI of the calling stylesheet (unused)
     * @return the {@link Source} returned by the invoked method handler
     * @throws TransformerException if the method handler throws a {@link TransformerException}
     *                              or any other exception during processing
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        MCRTransformerHelperCall call = new MCRTransformerHelperCall(href);

        try {
            handleCall(call);
        } catch (TransformerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }

        return call.getSourceToReturn();
    }

    private void handleCall(MCRTransformerHelperCall call) throws Exception {
        MCRTransformationState tfhelper = call.getTransformerHelper();
        tfhelper.getMethodHelperMap().get(call.getMethod()).handle(call);
    }

}
