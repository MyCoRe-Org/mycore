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

package org.mycore.frontend.xeditor.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;

/**
 * <p>
 * Reads default rules for XML cleaning from configuration.
 * Each rule has a common prefix and two properties:
 * </p>
 * 
 * <ul>
 *   <li>The XPath to select the nodes to inspect
 *   <li>The XPath to decide if this node is relevant or should be removed from xml.
 * </ul>
 * 
 * Rules are read in alphabetical order of their prefix.  
 * 
 * <p>
 * Example:
 * <code>
 * MCR.XEditor.XMLCleaner.DefaultRule.01.RemoveEmptyAttributes.NodesToInspect=//@*<br/>
 * MCR.XEditor.XMLCleaner.DefaultRule.01.RemoveEmptyAttributes.RelevantIf=string-length(.) > 0
 * </code>
 * </p>
 * 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRDefaultRules {

    private static final String CONFIG_PREFIX = "MCR.XEditor.XMLCleaner.DefaultRule.";
    private static final String CONFIG_KEY_NODES_TO_INSPECT = "NodesToInspect";
    private static final String CONFIG_KEY_RELEVANT_IF = "RelevantIf";

    private static final List<MCRCleaningRule> DEFAULT_RULES;

    static {
        List<MCRCleaningRule> defaultRules = new ArrayList<>();

        Map<String, String> config = MCRConfiguration2.getSubPropertiesMap(CONFIG_PREFIX);

        config.keySet().stream().sorted().filter(key -> key.endsWith(CONFIG_KEY_NODES_TO_INSPECT)).forEach(key -> {
            String rulePrefix = key.substring(0, key.indexOf(CONFIG_KEY_NODES_TO_INSPECT));

            String nodesToInspect = config.get(key);
            String relevantIf = config.get(rulePrefix + CONFIG_KEY_RELEVANT_IF);

            MCRCleaningRule rule = new MCRCleaningRule(nodesToInspect, relevantIf);
            defaultRules.add(rule);
        });

        DEFAULT_RULES = Collections.unmodifiableList(defaultRules);
    }

    static List<MCRCleaningRule> getDefaultRules() {
        return DEFAULT_RULES;
    }
}
