package org.mycore.services.acl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.mycore.access.mcrimpl.MCRAccessControlSystem;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSPK;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

import sun.security.krb5.internal.UDPClient;

public class MCRACLXMLProcessing {
    private static Logger LOGGER = Logger.getLogger(MCRACLXMLProcessing.class);

    public Element accessFilter2XML(String objid, String acpool) {
        Element filter = new Element("mcr_access_filter");
        Element objidFilter = new Element("objid");
        Element acpoolFilter = new Element("acpool");

        if (objid == null)
            objid = "";

        if (acpool == null)
            acpool = "";

        objidFilter.addContent(objid);
        filter.addContent(objidFilter);

        acpoolFilter.addContent(acpool);
        filter.addContent(acpoolFilter);

        return filter;
    }

    public Element access2XML(List accessList) {
        Element mcrAccessSet = new Element("mcr_access_set");

        if (accessList == null){
            MCRACCESS emptyAccess = new MCRACCESS();
            emptyAccess.setRid("");
            emptyAccess.setKey(new MCRACCESSPK("",""));
            accessList = new LinkedList();
            accessList.add(emptyAccess);
        }
        
        int i = 0;
        for (Iterator it = accessList.iterator(); it.hasNext();) {
            MCRACCESS accessView = (MCRACCESS) it.next();

            Element mcrAccess = new Element("mcr_access");
            mcrAccess.setAttribute("pos", Integer.toString(i));

            Element ACPOOL = new Element("ACPOOL");
            ACPOOL.addContent(accessView.getKey().getAcpool());

            Element OBJID = new Element("OBJID");
            OBJID.addContent(accessView.getKey().getObjid());

            Element RID = new Element("RID");
            RID.addContent(accessView.getRid());

            mcrAccess.addContent(ACPOOL);
            mcrAccess.addContent(OBJID);
            mcrAccess.addContent(RID);

            mcrAccessSet.addContent(mcrAccess);
            
            i++;
        }
        return mcrAccessSet;
    }
    
    public Element ruleSet2Items(List ruleList) {
        Element items = new Element("items");

        for (Iterator it = ruleList.iterator(); it.hasNext();) {
            MCRACCESSRULE rule = (MCRACCESSRULE) it.next();

            Element item = new Element("item");
            String rid = rule.getRid();
            item.setAttribute("value", rid);

            String descr = rule.getDescription();

            if (descr != null)
                item.setAttribute("label", descr + " ( " + rid + ")");
            else
                item.setAttribute("label", rid);

            items.addContent(item);
        }

        return items;
    }

    public Element ruleSet2XML(List ruleList) {
        Element mcrAccessRuleSet = new Element("mcr_access_rule_set");
        
        if (ruleList == null){
            MCRACCESSRULE emptyRule = new MCRACCESSRULE();
            emptyRule.setRid("");
            emptyRule.setRule("");
            ruleList = new LinkedList();
            ruleList.add(emptyRule);
        }

        int i = 0;
        for (Iterator it = ruleList.iterator(); it.hasNext();) {
            MCRACCESSRULE rule = (MCRACCESSRULE) it.next();

            Element mcrAccessRule = new Element("mcr_access_rule");
            mcrAccessRule.setAttribute("pos", Integer.toString(i));
            
            mcrAccessRule.addContent(new Element("RID").addContent(rule.getRid()));
            mcrAccessRule.addContent(new Element("RULE").addContent(rule.getRule()));

            String descr = rule.getDescription();

            if (descr != null)
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(descr));
            else
                mcrAccessRule.addContent(new Element("DESCRIPTION").addContent(""));

            mcrAccessRuleSet.addContent(mcrAccessRule);
            
            i++;
        }

        return mcrAccessRuleSet;
    }

    public Map findRulesDiff(Document editedRules, Document origRules) throws Exception {
        Element editedRulesRoot = editedRules.getRootElement();
        Element origRulesRoot = origRules.getRootElement();
        
        Map diffMap = new HashMap();
        
        List updateList = new LinkedList();
        List saveList = new LinkedList();
        List deleteList = new LinkedList();
        
        List matches = new LinkedList();
        
        String rule = "";
        String description = "";
        
        Filter newRulesFilter = new Filter() {
            public boolean matches(Object arg0) {
                if (((Element) arg0).getAttributeValue("pos") == null) {
                    return true;
                } else
                    return false;
            }
        };
        
        matches = editedRulesRoot.removeContent(newRulesFilter);
        
        // saving new rules
        int i = 0;
        Iterator iter = matches.iterator();
        while (iter.hasNext()){
            Element currentRule = (Element) iter.next();
            
            rule = currentRule.getChildText("RULE");
            description = currentRule.getChildText("DESCRIPTION");
            
            MCRACCESSRULE accessRule = new MCRACCESSRULE();
            accessRule.setRule(rule);
            accessRule.setDescription(description);
            
            saveList.add(accessRule);
            i++;
        }
        
        iter = editedRulesRoot.getChildren().iterator();
        while (iter.hasNext()){
            Element currentElem = (Element) iter.next();
            final String position = currentElem.getAttributeValue("pos");
            
            Filter posFilter = new Filter() {
                public boolean matches(Object arg0) {
                    if (((Element) arg0).getAttributeValue("pos").equals(position)) {
                        return true;
                    } else
                        return false;
                }
            };
            
            List matchingList = origRulesRoot.removeContent(posFilter);
            if (matchingList.size() > 1)
                throw new Exception("Position number in access XML not unique!!");
            
            Element origElem = (Element) matchingList.get(0);
            String currentRID = currentElem.getChildText("RID");
            String currentRule = currentElem.getChildText("RULE");
            String currentDesc = currentElem.getChildText("DESCRIPTION");
            if (currentDesc == null)
                currentDesc = "";
            
            String origRule = origElem.getChildText("RULE");
            String origDesc = origElem.getChildText("DESCRIPTION");
            if (origDesc == null)
                origDesc = "";
            
            LOGGER.debug("\t Rule edited: \n\t\t" + currentRule + "\n\t orig: \n\t\t" + origRule);
            LOGGER.debug("\t Desc edited: " + currentDesc + " # orig: " + origDesc);
            
            if (!currentRule.equals(origRule) || !currentDesc.equals(origDesc)) {
                
                /*try {
                    SAXBuilder saxBuilder=new SAXBuilder("org.apache.xerces.parsers.SAXParser");
                    Reader stringReader=new StringReader(currentRule);
                    org.jdom.Document jdomDocument=saxBuilder.build(stringReader);
                } catch (JDOMParseException e) {
                    // TODO: handle exception
                }*/
                LOGGER.debug("Adding rule " + currentRID + " to update list!");
                
                updateList.add(new MCRAccessRule(currentRID,"ACL-Editor",new Date(),currentRule,currentDesc));
            }
        }
        
        iter = origRulesRoot.getChildren().iterator();
        while(iter.hasNext()){
            deleteList.add(((Element)iter.next()).getChildText("RID"));
        }
        
        diffMap.put("update", updateList);
        diffMap.put("save", saveList);
        diffMap.put("delete", deleteList);
        
        return diffMap;
    }
    
    public Map findAccessDiff(Document editedAcces, Document origAccess) throws Exception {
        Element editedAccessRoot = editedAcces.getRootElement();
        Map diffMap = new HashMap();

        LOGGER.info("******* Filter not changed!!");

        List editedElemList = editedAccessRoot.getChildren();
        Element origAccessRoot = origAccess.getRootElement();
        
        List updateList = new LinkedList();
        List saveList = new LinkedList();
        List deleteList = new LinkedList();

        for (Iterator iter = editedElemList.iterator(); iter.hasNext();) {
            Element currentElem = (Element) iter.next();
            final String position = currentElem.getAttributeValue("pos");

            if (position == null) {
                String currentACPool = currentElem.getChildText("ACPOOL");
                String currentObjID = currentElem.getChildText("OBJID");
                String currentRID = currentElem.getChildText("RID");

                saveList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
            } else {
                Filter posFilter = new Filter() {
                    public boolean matches(Object arg0) {
                        if (((Element) arg0).getAttributeValue("pos").equals(position)) {
                            return true;
                        } else
                            return false;
                    }
                };

                List matchingList = origAccessRoot.removeContent(posFilter);
                if (matchingList.size() > 1)
                    throw new Exception("Position number in access XML not unique!!");

                Element origElem = (Element) matchingList.get(0);
                String currentACPool = currentElem.getChildText("ACPOOL");
                String currentObjID = currentElem.getChildText("OBJID");
                String currentRID = currentElem.getChildText("RID");

                String origACPool = origElem.getChildText("ACPOOL");
                String origObjID = origElem.getChildText("OBJID");
                String origRID = origElem.getChildText("RID");

                if (!currentACPool.equals(origACPool) || !currentObjID.equals(origObjID)) {
                    saveList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
                    deleteList.add(createRuleMapping(origRID, origACPool, origObjID));
                } else if (!currentRID.equals(origRID)) {
                    updateList.add(createRuleMapping(currentRID, currentACPool, currentObjID));
                }
            }
        }

        for (Iterator iter = origAccessRoot.getChildren().iterator(); iter.hasNext();) {
            Element origElem = (Element) iter.next();

            String origACPool = origElem.getChildText("ACPOOL");
            String origObjID = origElem.getChildText("OBJID");
            String origRID = origElem.getChildText("RID");

            deleteList.add(createRuleMapping(origRID, origACPool, origObjID));

        }

        diffMap.put("update", updateList);
        diffMap.put("save", saveList);
        diffMap.put("delete", deleteList);

        return diffMap;
    }

    public Element findDiffAsXML(Document editedAcces, Document origAccess) throws Exception {
        Map diffMap = findAccessDiff(editedAcces, origAccess);

        Element diff = new Element("Diff_list");

        Element update = makeElem((List) diffMap.get("update"), "update");
        Element save = makeElem((List) diffMap.get("save"), "save");
        Element delete = makeElem((List) diffMap.get("delete"), "delete");

        diff.addContent(update);
        diff.addContent(save);
        diff.addContent(delete);

        return diff;
    }

    private Element makeElem(List list, String rootName) {
        Element root = new Element(rootName);
        Element mcr_access_set = new Element("mcr_access_set");
        int i = 0;

        for (Iterator it = list.iterator(); it.hasNext();) {
            MCRRuleMapping ruleMapping = (MCRRuleMapping) it.next();

            Element mcr_access = new Element("mcr_access");
            mcr_access.setAttribute("pos", Integer.toString(i));

            Element ACPOOL = new Element("ACPOOL");
            ACPOOL.addContent(ruleMapping.getPool());

            Element OBJID = new Element("OBJID");
            OBJID.addContent(ruleMapping.getObjId());

            Element RID = new Element("RID");
            RID.addContent(ruleMapping.getRuleId());

            mcr_access.addContent(ACPOOL);
            mcr_access.addContent(OBJID);
            mcr_access.addContent(RID);

            mcr_access_set.addContent(mcr_access);
            i++;
        }

        return root.addContent(mcr_access_set);

    }

    private MCRRuleMapping createRuleMapping(String rid, String acpool, String objid) {
        MCRRuleMapping ruleMapping = new MCRRuleMapping();

        ruleMapping.setCreator("ACL-Editor");
        ruleMapping.setCreationdate(new Date());
        ruleMapping.setPool(acpool);
        ruleMapping.setRuleId(rid);
        ruleMapping.setObjId(objid);
        return ruleMapping;
    }
}
