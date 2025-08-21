package org.mycore.frontend.xeditor.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mycore.common.config.MCRConfiguration2;

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
