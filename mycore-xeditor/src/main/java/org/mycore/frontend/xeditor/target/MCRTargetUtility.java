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

import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRTargetUtility {
    static Map<String, String[]> getSubmittedValues(MCRServletJob job, String baseXPath)
        throws JDOMException, JaxenException {
        Map<String, String[]> valuesToSet = new HashMap<>();

        for (String paramName : job.getRequest().getParameterMap().keySet()) {
            if (!paramName.startsWith("_xed_")) {
                String xPath = baseXPath + "/" + paramName;
                String[] values = job.getRequest().getParameterValues(paramName);
                valuesToSet.put(xPath, values);
            }
        }
        return valuesToSet;
    }
}
