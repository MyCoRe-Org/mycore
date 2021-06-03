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
package org.mycore.access.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.access.xml.conditions.MCRCondition;
import org.mycore.access.xml.conditions.MCRDebuggableCondition;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MCRXMLAccessSystem implements MCRAccessInterface, MCRAccessCheckStrategy {

    protected static final Logger LOGGER = LogManager.getLogger();

    private MCRCondition rules;

    private String rulesURI = "resource:rules.xml";

    private Map<String, String> properties;

    private MCRAccessCheckStrategy fallbackStrategy = null;

    public MCRXMLAccessSystem() {
    }

    @MCRPostConstruction
    public void init(String property) {
        rules = this.buildRulesFromXML();
        Optional<MCRAccessCheckStrategy> fallback = MCRConfiguration2.getInstanceOf(property + ".FallbackStrategy");
        fallback.ifPresent(strategy -> fallbackStrategy = strategy);
    }

    @MCRProperty(name = "rulesURI", required = false)
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
        return MCRConditionFactory.parse(eRules);
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
    public boolean checkPermission(String id, String permission, MCRUserInformation userInfo) {
        String action = permission.replaceAll("db$", ""); // writedb -> write

        String target; // metadata|files|webpage
        String cacheKey;

        MCRFacts facts = new MCRFacts();

        if(id==null ){
            cacheKey = action;
        } else{
            if (MCRObjectID.isValid(id)) {
                MCRObjectID oid = MCRObjectID.getInstance(id);
                target = "metadata";

                if ("derivate".equals(oid.getTypeId())) {
                    facts.add(MCRConditionFactory.build("derivateid", id));
                    target = "files";
                    MCRDerivate deriv = MCRMetadataManager.retrieveMCRDerivate(oid);
                    id = deriv.getOwnerID().toString();
                }
            } else if (id.startsWith("webpage")) {
                target = "webpage";
            } else if (id.startsWith("solr")){
                target = "solr";
            } else {
                target = "unknown";
            }
            cacheKey = action + " " + id + " " + target;
            facts.add(MCRConditionFactory.build("id", id));
            facts.add(MCRConditionFactory.build("target", target));
        }

        LOGGER.debug("Testing {} ", cacheKey);
        facts.add(MCRConditionFactory.build("action", action));



        boolean result;
        if (LOGGER.isDebugEnabled()) {
            MCRCondition rules = buildRulesFromXML();
            result = rules.matches(facts);
            LOGGER.debug("Facts are: {}", facts);
            if(rules instanceof MCRDebuggableCondition){
                Element xmlTree = ((MCRDebuggableCondition) rules).getBoundElement();
                String xmlString = new XMLOutputter(Format.getPrettyFormat()).outputString(xmlTree);
                LOGGER.debug(xmlString);
            }
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
