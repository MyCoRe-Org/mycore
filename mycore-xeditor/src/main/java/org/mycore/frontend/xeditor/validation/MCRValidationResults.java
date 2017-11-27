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

package org.mycore.frontend.xeditor.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration;

public class MCRValidationResults {

    static final String MARKER_DEFAULT;

    static final String MARKER_SUCCESS;

    static final String MARKER_ERROR;

    static {
        String prefix = "MCR.XEditor.Validation.Marker.";
        MARKER_DEFAULT = MCRConfiguration.instance().getString(prefix + "default", "");
        MARKER_SUCCESS = MCRConfiguration.instance().getString(prefix + "success", "has-success");
        MARKER_ERROR = MCRConfiguration.instance().getString(prefix + "error", "has-error");
    }

    private Map<String, String> xPath2Marker = new HashMap<>();

    private LinkedHashMap<String, MCRValidator> xPath2FailedRule = new LinkedHashMap<>();

    private boolean isValid = true;

    public boolean hasError(String xPath) {
        return MARKER_ERROR.equals(xPath2Marker.get(xPath));
    }

    public void mark(String xPath, boolean isValid, MCRValidator failedRule) {
        if (hasError(xPath)) {
            return;
        }

        if (isValid) {
            xPath2Marker.put(xPath, MARKER_SUCCESS);
        } else {
            xPath2Marker.put(xPath, MARKER_ERROR);
            xPath2FailedRule.put(xPath, failedRule);
            this.isValid = false;
        }
    }

    public String getValidationMarker(String xPath) {
        return xPath2Marker.getOrDefault(xPath, MARKER_DEFAULT);
    }

    public MCRValidator getFailedRule(String xPath) {
        return xPath2FailedRule.get(xPath);
    }

    public Collection<MCRValidator> getFailedRules() {
        return xPath2FailedRule.values();
    }

    public boolean isValid() {
        return isValid;
    }
}
