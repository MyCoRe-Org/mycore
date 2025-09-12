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

import org.jdom2.transform.JDOMSource;

public class MCRTransformerHelperResolver implements URIResolver {

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

        JDOMSource source = new JDOMSource(call.getReturnElement());
        // Workaround to prevent URI Caching:
        source.setSystemId(source.getSystemId() + Math.random());
        return source;
    }

    private void handleCall(MCRTransformerHelperCall call) throws Exception {
        MCRTransformerHelper tfhelper = call.getTransformerHelper();

        switch (call.getMethod()) {
            case "form":
                tfhelper.handleForm(call);
                break;
            case "input":
                tfhelper.handleInput(call);
                break;
            case "textarea":
                tfhelper.handleTextarea(call);
                break;
            case "button":
                tfhelper.handleSubmitButton(call);
                break;
            case "cleanup-rule":
                tfhelper.handleCleanupRule(call);
                break;
            case "load-resource":
                tfhelper.handleLoadResource(call);
                break;
            case "replaceXPaths":
                tfhelper.handleReplaceXPaths(call);
                break;
            default:
                tfhelper.handle(call);
        }
    }
}
