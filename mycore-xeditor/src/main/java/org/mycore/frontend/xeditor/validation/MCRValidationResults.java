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

package org.mycore.frontend.xeditor.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

import org.mycore.common.config.MCRConfiguration2;
import org.w3c.dom.Node;

/**
 * Collects validation markers and failed validation rules.
 * <p>
 * A marker describes the combined validation state of an absolute XPath: once a rule fails there, the marker remains
 * {@link #MARKER_ERROR}. Failed rules are tracked separately by absolute XPath and originating validation rule element.
 * Thus, distinct {@code xed:validate} declarations can be reported for the same XPath, while multiple internal
 * validators created from one declaration produce only one failure for that XPath.
 */
public class MCRValidationResults {

    static final String MARKER_DEFAULT;

    static final String MARKER_SUCCESS;

    static final String MARKER_ERROR;

    static {
        String prefix = "MCR.XEditor.Validation.Marker.";
        MARKER_DEFAULT = MCRConfiguration2.getString(prefix + "default").orElse("");
        MARKER_SUCCESS = MCRConfiguration2.getString(prefix + "success").orElse("mcr-valid");
        MARKER_ERROR = MCRConfiguration2.getString(prefix + "error").orElse("mcr-invalid");
    }

    private Map<String, String> xPath2Marker = new HashMap<>();

    private SequencedMap<FailedRuleKey, MCRValidator> failedRules = new LinkedHashMap<>();

    private boolean isValid = true;

    public boolean hasError(String xPath) {
        return MARKER_ERROR.equals(xPath2Marker.get(xPath));
    }

    public void mark(String xPath, boolean isValid, MCRValidator failedRule) {
        if (isValid) {
            if (!hasError(xPath)) {
                xPath2Marker.put(xPath, MARKER_SUCCESS);
            }
        } else {
            xPath2Marker.put(xPath, MARKER_ERROR);
            Node ruleElement = failedRule == null ? null : failedRule.getRuleElement();
            failedRules.putIfAbsent(new FailedRuleKey(xPath, ruleElement), failedRule);
            this.isValid = false;
        }
    }

    public String getValidationMarker(String xPath) {
        return xPath2Marker.getOrDefault(xPath, MARKER_DEFAULT);
    }

    /**
     * Returns the first failed rule for the given absolute XPath, or {@code null} if no rule failed there.
     */
    public MCRValidator getFailedRule(String xPath) {
        for (Map.Entry<FailedRuleKey, MCRValidator> failedRule : failedRules.entrySet()) {
            if (xPath.equals(failedRule.getKey().xPath())) {
                return failedRule.getValue();
            }
        }
        return null;
    }

    /**
     * Returns all distinct failed rules in validation order. A rule that fails for multiple absolute XPaths occurs once
     * per XPath, preserving the established behavior.
     */
    public Collection<MCRValidator> getFailedRules() {
        return failedRules.values();
    }

    public boolean isValid() {
        return isValid;
    }

    private record FailedRuleKey(String xPath, Node ruleElement) {

        /**
         * DOM {@link Node} does not define Java {@link Object#equals(Object)} semantics. Use object identity here so
         * equality consistently means that failures originated from the exact same validation rule element,
         * independent of the DOM implementation.
         */
        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof FailedRuleKey other
                && xPath.equals(other.xPath) && ruleElement == other.ruleElement;
        }

        /**
         * Uses the identity hash corresponding to the identity comparison in {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return 31 * xPath.hashCode() + System.identityHashCode(ruleElement);
        }
    }
}
