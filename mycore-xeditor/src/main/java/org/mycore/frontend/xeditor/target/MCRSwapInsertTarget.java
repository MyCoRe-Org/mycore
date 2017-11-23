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

package org.mycore.frontend.xeditor.target;

import javax.servlet.ServletContext;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCRBinding;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.MCRRepeatBinding;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCRSwapInsertTarget extends MCRRepeaterControl {

    protected void handleRepeaterControl(ServletContext context, MCRServletJob job, MCREditorSession session,
        String param)
        throws Exception {
        handle(param, session.getRootBinding());
        session.setBreakpoint("After handling target " + getClass().getName() + " " + param);
    }

    public void handle(String swapParameter, MCRBinding root) throws JaxenException, JDOMException {
        String[] tokens = swapParameter.split("\\|");
        String parentXPath = tokens[0];
        String posString = tokens[1];
        int pos = Integer.parseInt(posString);
        String method = tokens[2];
        String elementNameWithPredicates = swapParameter
            .substring(parentXPath.length() + posString.length() + method.length() + 3);

        MCRBinding parentBinding = new MCRBinding(parentXPath, false, root);
        MCRRepeatBinding repeatBinding = new MCRRepeatBinding(elementNameWithPredicates, parentBinding, method);
        handle(pos, repeatBinding);
        repeatBinding.detach();
        parentBinding.detach();
    }

    protected abstract void handle(int pos, MCRRepeatBinding repeatBinding) throws JaxenException;

    protected static String buildParameter(MCRRepeatBinding repeatBinding, int pos) throws JaxenException {
        String parentXPath = MCRXPathBuilder.buildXPath(repeatBinding.getParentElement());
        String nameWithPredicates = repeatBinding.getElementNameWithPredicates();
        String method = repeatBinding.getMethod();
        return parentXPath + "|" + pos + "|" + method + "|" + nameWithPredicates;
    }
}
