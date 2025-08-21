package org.mycore.frontend.xeditor.cleanup;

import java.util.ArrayList;
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

    private static final List<MCRCleaningRule> defaultRules = new ArrayList<>();

    static {
        Map<String, String> config = MCRConfiguration2.getSubPropertiesMap(CONFIG_PREFIX);
        
        config.keySet().stream().sorted().filter(key -> key.endsWith(CONFIG_KEY_NODES_TO_INSPECT)).forEach(key -> {
            String rulePrefix = key.substring(0, key.indexOf(CONFIG_KEY_NODES_TO_INSPECT));
            System.out.println(rulePrefix);

            String nodesToInspect = config.get(key);
            System.out.println(nodesToInspect);
            String relevantIf = config.get(rulePrefix + CONFIG_KEY_RELEVANT_IF);
            System.out.println(relevantIf);

            MCRCleaningRule rule = new MCRCleaningRule(nodesToInspect, relevantIf);
            defaultRules.add(rule);
        });
    }

    static List<MCRCleaningRule> getDefaultRules() {
        return defaultRules;
    }
}
