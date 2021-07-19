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
package org.mycore.access.facts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.access.facts.model.MCRCombinedCondition;
import org.mycore.access.facts.model.MCRCondition;
import org.mycore.access.facts.model.MCRFact;
import org.mycore.access.facts.model.MCRFactComputable;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * base class for XML fact based access system
 * 
 * enabled it with the 2 properties:
 * MCR.Access.Class=org.mycore.access.facts.MCRFactsAccessSystem
 * MCR.Access.Strategy.Class=org.mycore.access.facts.MCRFactsAccessSystem
 * 
 */
@Singleton
public class MCRFactsAccessSystem implements MCRAccessInterface, MCRAccessCheckStrategy {

    protected static final Logger LOGGER = LogManager.getLogger();

    private MCRCondition rules;

    private Collection<MCRFactComputable<MCRFact<?>>> computers;

    //RS: when introducing this feature in 2021.06.LTS it needed to be configured twice
    //(as access system and as strategy). To simplify things during the transition period 
    //we are going to use the base property to initialize the rulesURI for both cases
    //By using the property MCR.Access.Strategy.RulesURI it could be overwritten if used for strategy.
    private String rulesURI = MCRConfiguration2.getString("MCR.Access.RulesURI").orElse("resource:rules.xml");

    private Map<String, String> properties;

    @MCRPostConstruction
    public void init(String property) {
        rules = buildRulesFromXML();
        computers = buildComputersFromRules();
    }

    private Collection<MCRFactComputable<MCRFact<?>>> buildComputersFromRules() {
        Map<String, MCRFactComputable<MCRFact<?>>> collectedComputers = new HashMap<>();
        collectComputers(rules, collectedComputers);
        return collectedComputers.values();
    }

    @SuppressWarnings("unchecked")
    private void collectComputers(MCRCondition coll, Map<String, MCRFactComputable<MCRFact<?>>> computers) {
        if (coll instanceof MCRFactComputable<?>
            && !computers.containsKey(((MCRFactComputable<?>) coll).getFactName())) {
            computers.put(((MCRFactComputable<?>) coll).getFactName(), (MCRFactComputable<MCRFact<?>>) coll);
        }
        if (coll instanceof MCRCombinedCondition) {
            ((MCRCombinedCondition) coll).getChildConditions().forEach(c -> collectComputers(c, computers));
        }
    }

    @MCRProperty(name = "RulesURI", required = false)
    public void setRulesURI(String uri) {
        rulesURI = uri;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @MCRProperty(name = "*")
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    private MCRCondition buildRulesFromXML() {
        Element eRules = MCRURIResolver.instance().resolve(rulesURI);
        Objects.requireNonNull(eRules, "The rulesURI " + rulesURI + " resolved to null!");
        return MCRFactsAccessSystemHelper.parse(eRules);
    }

    @Override
    public boolean checkPermission(String id, String permission) {
        return this.checkPermission(id, permission, MCRSessionMgr.getCurrentSession().getUserInformation());
    }

    @Override
    public boolean checkPermissionForUser(String permission, MCRUserInformation userInfo) {
        return false;
    }

    @Override
    public boolean checkPermission(final String checkID, String permission, MCRUserInformation userInfo) {
        String action = permission.replaceAll("db$", ""); // writedb -> write

        String target; // metadata|files|webpage
        String cacheKey;

        MCRFactsHolder facts = new MCRFactsHolder(computers);

        if (checkID == null) {
            cacheKey = action;
        } else {
            if (MCRObjectID.isValid(checkID)) {
                MCRObjectID oid = MCRObjectID.getInstance(checkID);
                target = "metadata";

                if ("derivate".equals(oid.getTypeId())) {
                    facts.add(new MCRObjectIDFact("derid", checkID, oid));
                    target = "files";
                    MCRDerivate deriv = MCRMetadataManager.retrieveMCRDerivate(oid);
                    facts.add(new MCRObjectIDFact("objid", checkID, deriv.getOwnerID()));
                } else {
                    facts.add(new MCRObjectIDFact("objid", checkID, oid));
                }
            } else if (checkID.startsWith("webpage")) {
                target = "webpage";
            } else if (checkID.startsWith("solr")) {
                target = "solr";
            } else {
                target = "unknown";
            }
            cacheKey = action + " " + checkID + " " + target;
            facts.add(new MCRStringFact("id", checkID));
            facts.add(new MCRStringFact("target", target));
        }

        LOGGER.debug("Testing {} ", cacheKey);
        facts.add(new MCRStringFact("action", action));

        boolean result;
        if (LOGGER.isDebugEnabled()) {
            MCRCondition rules = buildRulesFromXML();
            result = rules.matches(facts);
            LOGGER.debug("Facts are: {}", facts);

            Element xmlTree = rules.getBoundElement();
            String xmlString = new XMLOutputter(Format.getPrettyFormat()).outputString(xmlTree);
            LOGGER.debug(xmlString);
        } else {
            result = rules.matches(facts);
        }
        LOGGER.info("Checked permission to {} := {}", cacheKey, result);

        return result;
    }

    @Override
    public boolean checkPermission(String permission) {
        return checkPermission(null, permission);
    }

}
